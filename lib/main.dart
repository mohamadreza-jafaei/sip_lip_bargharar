import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'dart:async';
import 'package:flutter/services.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();

  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // TRY THIS: Try running your application with "flutter run". You'll see
        // the application has a blue toolbar. Then, without quitting the app,
        // try changing the seedColor in the colorScheme below to Colors.green
        // and then invoke "hot reload" (save your changes or press the "hot
        // reload" button in a Flutter-supported IDE, or press "r" if you used
        // the command line to start the app).
        //
        // Notice that the counter didn't reset back to zero; the application
        // state is not lost during the reload. To reset the state, use hot
        // restart instead.
        //
        // This works for code too, not just values: Most code changes can be
        // tested with just a hot reload.
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
        useMaterial3: true,
      ),
      home: MyHomePage(title: 'Flutter Demo Home Page'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  MyHomePage({super.key, required this.title});

  // This widget is the home page of your application. It is stateful, meaning
  // that it has a State object (defined below) that contains fields that affect
  // how it looks.

  // This class is the configuration for the state. It holds the values (in this
  // case the title) provided by the parent (in this case the App widget) and
  // used by the build method of the State. Fields in a Widget subclass are
  // always marked "final".

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  static const platform = MethodChannel('ir.bargharar/call');
  static const EventChannel eventChannel =
      EventChannel('ir.bargharar/register_events');
  static final eventStream = eventChannel.receiveBroadcastStream();

  String status = "not_connect";
  String callStatus = "nothing";

  Future<void> connectToSip() async {
    eventStream.listen((event) {
      if (event == "Failed") {
        setState(() {
          status = "Failed";
        });
      } else if (event == "Ok") {
        setState(() {
          status = "Ok";
        });
      } else if (event == "Incoming") {
        setState(() {
          callStatus = "incomming";
        });
      } else if (event == "ConnectIncoming") {
        setState(() {
          callStatus = "connecting";
        });
      } else if (event == "EndIncoming") {
        setState(() {
          callStatus = "ennnnnded";
        });
      }
    });
    try {
      bool? result = await platform.invokeMethod<bool>('connect_to_sip');
      if (kDebugMode) {
        print(result);
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed to connect sip: '${e.message}'.");
      }
    }
  }

  
Future<void> sendEvent(dddd) async {
    try {
      await platform.invokeMethod<bool>(dddd);
      
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed  sip: '${e.message}'.");
      }
    }
  }

  Future<void> call() async {
    try {
      bool? result = await platform.invokeMethod<bool>('call_to_sip');
      if (kDebugMode) {
        print(result);
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("Failed to connect sip: '${e.message}'.");
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            Text(
              status,
            ),
            Text(
              callStatus,
              style: Theme.of(context).textTheme.headlineMedium,
            ),
            InkWell(
              onTap: () {
                call();
              },
              child: const Text("call to sip"),
            ),
            InkWell(
              onTap: () {
                sendEvent("answer");
              },
              child: const Text("answer"),
            ),
            InkWell(
              onTap: () {
                sendEvent("hang_up");
              },
              child: const Text("hangup"),
            ),
            InkWell(
              onTap: () {
                sendEvent("mute_mic");
              },
              child: const Text("mute_mic"),
            ),
            InkWell(
              onTap: () {
                sendEvent("toggle_speaker");
              },
              child: const Text("toggle_speaker"),
            ),
            
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton(
        onPressed: connectToSip,
        tooltip: 'Increment',
        child: const Icon(Icons.add),
      ),
    );
  }
}

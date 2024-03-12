package com.example.bargharar_lipphone_kotlin

import io.flutter.embedding.android.FlutterActivity

import androidx.annotation.NonNull
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import io.flutter.plugin.common.EventChannel

import org.linphone.core.*
import org.linphone.core.tools.Log

class MainActivity: FlutterActivity() {
    private lateinit var core: Core
    
    private val CHANNEL = "ir.bargharar/call"

    private val registerHandler = RegisterState()



    private fun toggleSpeaker() {
        // Get the currently used audio device
        val currentAudioDevice = core.currentCall?.outputAudioDevice
        val speakerEnabled = currentAudioDevice?.type == AudioDevice.Type.Speaker

        // We can get a list of all available audio devices using
        // Note that on tablets for example, there may be no Earpiece device
        for (audioDevice in core.audioDevices) {
            if (speakerEnabled && audioDevice.type == AudioDevice.Type.Earpiece) {
                core.currentCall?.outputAudioDevice = audioDevice
                return
            } else if (!speakerEnabled && audioDevice.type == AudioDevice.Type.Speaker) {
                core.currentCall?.outputAudioDevice = audioDevice
                return
            }/* If we wanted to route the audio to a bluetooth headset
            else if (audioDevice.type == AudioDevice.Type.Bluetooth) {
                core.currentCall?.outputAudioDevice = audioDevice
            }*/
        }
    }

    

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val factory = Factory.instance()
        factory.setDebugMode(true, "Hello Linphone")
        core = factory.createCore(null, null, this)
        EventChannel(flutterEngine.dartExecutor.binaryMessenger, "ir.bargharar/register_events").setStreamHandler(registerHandler)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler {
            // This method is invoked on the main thread.
            call, result ->
            if(call.method == "connect_to_sip"){
                val username = "09014700708"
                val password = "09014700708"
                val domain = "voip.ariyanasoft.net"

                val transportType = TransportType.Tcp

                val authInfo = Factory.instance().createAuthInfo(username, null, password, null, null, domain, null)

                val accountParams = core.createAccountParams()

                val identity = Factory.instance().createAddress("sip:$username@$domain")
                accountParams.identityAddress = identity

                val address = Factory.instance().createAddress("sip:$domain")
                // We use the Address object to easily set the transport protocol
                address?.transport = transportType
                accountParams.serverAddress = address
                accountParams.registerEnabled = true

                // Now that our AccountParams is configured, we can create the Account object
                val account = core.createAccount(accountParams)

                // Now let's add our objects to the Core
                core.addAuthInfo(authInfo)
                core.addAccount(account)

                // Also set the newly added account as default
                core.defaultAccount = account



                // To be notified of the connection status of our account, we need to add the listener to the Core
                core.addListener(registerHandler)
                // We can also register a callback on the Account object
                account.addListener { _, state, message ->
                    // There is a Log helper in org.linphone.core.tools package
                    Log.i("[Account] Registration state changed: $state, $message")
                }

                // Finally we need the Core to be started for the registration to happen (it could have been started before)
                Log.i("CORE STARTED")
                core.start()
                
            }else if(call.method == "call_to_sip"){
                // As for everything we need to get the SIP URI of the remote and convert it to an Address
                val remoteSipUri = "sip:100@voip.ariyanasoft.net"
                val remoteAddress = Factory.instance().createAddress(remoteSipUri)
                // If address parsing fails, we can't continue with outgoing call process
                if (remoteAddress == null) {
                    Log.e("Couldn't parse remote address, aborting call")
                    return@setMethodCallHandler
                }
                // We also need a CallParams object
                // Create call params expects a Call object for incoming calls, but for outgoing we must use null safely
                val params = core.createCallParams(null)
                // Same for params
                if (params == null) {
                    Log.e("Couldn't create CallParams, aborting call")
                    return@setMethodCallHandler
                }
                // We can now configure it
                // Here we ask for no encryption but we could ask for ZRTP/SRTP/DTLS
                params.mediaEncryption = MediaEncryption.None
                // If we wanted to start the call with video directly
                //params.enableVideo(true)

                // Finally we start the call
                core.inviteAddressWithParams(remoteAddress, params)
        // Call process can be followed in onCallStateChanged callback from core listener
            }else if(call.method == "hang_up"){
                core.currentCall?.terminate()
            }else if(call.method == "answer"){
                core.currentCall?.accept()
            }else if(call.method == "mute_mic"){
                core.enableMic(!core.micEnabled())
            }else if(call.method == "toggle_speaker"){
                toggleSpeaker()
            }
             else {
              result.notImplemented()
            }
          }
    }


}


class RegisterState: EventChannel.StreamHandler, CoreListenerStub(){

    private var registerEventSink: EventChannel.EventSink? = null

    fun initialize(context: Context) {
        
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        registerEventSink = events
    }

    override fun onCancel(arguments: Any?) {
        registerEventSink = null
    }

    override fun onAccountRegistrationStateChanged(core: Core, account: Account, state: RegistrationState, message: String) {
        if (state == RegistrationState.Failed || state == RegistrationState.Cleared) {
            Log.i("onAccountRegistrationStateChanged: $state")
            registerEventSink?.success("Failed")
        } else if (state == RegistrationState.Ok) {
            Log.i("onAccountRegistrationStateChanged: $state")
            registerEventSink?.success("Ok")
        }
    }

    override fun onAudioDeviceChanged(core: Core, audioDevice: AudioDevice) {
        // This callback will be triggered when a successful audio device has been changed
    }

    override fun onAudioDevicesListUpdated(core: Core) {
        // This callback will be triggered when the available devices list has changed,
        // for example after a bluetooth headset has been connected/disconnected.
    }

    override fun onCallStateChanged(
            core: Core,
            call: Call,
            state: Call.State?,
            message: String
        ) {
           

            // When a call is received
            when (state) {
                Call.State.IncomingReceived -> {
                    var number = call.remoteAddress.asStringUriOnly()
                    registerEventSink?.success("Incoming")
                    
                }
                Call.State.Connected -> {

                    registerEventSink?.success("ConnectIncoming")
                }
                Call.State.Released -> {
                    registerEventSink?.success("EndIncoming")
                }
                Call.State.OutgoingInit -> {
                    
                }
                Call.State.OutgoingProgress -> {
                    
                }
                Call.State.OutgoingRinging -> {

                }

                Call.State.StreamsRunning -> {
                    
                }
                Call.State.Paused -> {
                   
                }
                Call.State.PausedByRemote -> {
                  
                }
                Call.State.Updating -> {

                }
                Call.State.UpdatedByRemote -> {

                }
                Call.State.Released -> {
                    
                }
                Call.State.Error -> {

                }
                else ->{

                }

            }
        }

}

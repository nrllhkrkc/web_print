package com.akuple.web_print

import android.app.Activity
import android.content.Intent
import androidx.annotation.NonNull
import com.google.gson.Gson
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

/** WebPrintPlugin */
class WebPrintPlugin : FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private lateinit var activity: Activity
    private var pairListResult: Result? = null;

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "web_print")
        channel.setMethodCallHandler(this)

    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "printWebUrl" -> {
                callPrintWebUrl(call, result)
            }
            "getPairedBluetoothDevices" -> {
                getPairedBluetoothDevices(result)
            }
            "openBluetoothSettings" -> {
                openBluetoothSettings(result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun callPrintWebUrl(@NonNull call: MethodCall, @NonNull result: Result) {
        val url = call.argument<String>("url")
        printerAddress = call.argument<String>("printer_address")
        if (url != null) {
            try {
                WebPrintUtils.print(activity, url)
            } catch (e: Exception) {
                result.error("ex", e.message, e.stackTrace);
            }
            result.success(null)
        } else {
            result.error("url", "url parameter cannot be null", null)
        }
    }

    private fun getPairedBluetoothDevices(@NonNull result: Result) {
        try {
            val pairedDevices = WebPrintUtils.getPairedDevices(activity)
            result.success(Gson().toJson(pairedDevices))
        } catch (e: Exception) {
            pairListResult = result
        }
    }

    private fun openBluetoothSettings(@NonNull result: Result) {
        WebPrintUtils.openBluetoothSetting(activity)
        result.success(true)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {

    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {

    }

    override fun onDetachedFromActivity() {

    }

    companion object {
        var printerAddress: String? = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {

        when (requestCode) {
            WebPrintUtils.REQUEST_OPEN_BLUETOOTH_INTENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (pairListResult != null) {
                        getPairedBluetoothDevices(pairListResult!!)
                    }
                } else {
                    pairListResult?.error("1", "Bluetooth açık değil.", null)
                }
            }
        }

        return true

    }
}

package com.akuple.web_print

import android.app.Activity
import android.content.Intent
import androidx.annotation.NonNull
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
            "printWebView" -> {
                callPrintWebView(call, result)
            }

            "getPairedBluetoothDevices" -> {
                getPairedBluetoothDevices(result)
            }

            "openBluetoothSettings" -> {
                openBluetoothSettings(result)
            }

            "pluginTest" -> {
                result.success("test succeed");
            }

            else -> {
                result.notImplemented()
            }
        }
    }

    private fun callPrintWebView(@NonNull call: MethodCall, @NonNull result: Result) {
        val url = call.argument<String?>("url")
        val html = call.argument<String?>("html")
        printerAddress = call.argument<String>("printerAddress")
        topOffset = call.argument<Int>("topOffset")
        try {
            WebPrintUtils.print(activity, url, html)
            result.success(null)
        } catch (e: Exception) {
            result.error("ex", e.message, e.stackTraceToString())
        }
    }

    private fun getPairedBluetoothDevices(@NonNull result: Result) {
        try {
            val pairedDevices = WebPrintUtils.getPairedDevices(activity)
            val map = pairedDevices.map { d -> d.toJson() }
            result.success(map)
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


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {

        when (requestCode) {
            WebPrintUtils.REQUEST_OPEN_BLUETOOTH_INTENT -> {
                val pendingResult = pairListResult
                pairListResult = null
                if (pendingResult != null) {
                    if (resultCode == Activity.RESULT_OK) {
                        getPairedBluetoothDevices(pendingResult)
                    } else {
                        pendingResult.error("1", "Bluetooth açık değil.", null)
                    }
                }
            }
        }

        return true

    }

    companion object {
        var printerAddress: String? = null
        var topOffset: Int? = null
    }
}

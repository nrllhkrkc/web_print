package com.akuple.web_print

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Intent
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.akuple.web_print.models.BluetoothPrintDevice


object WebPrintUtils {
    const val REQUEST_BLUETOOTH_PERM = 0;
    const val REQUEST_OPEN_BLUETOOTH_INTENT = 1;

    fun print(context: Context, url: String?, htmlDocument: String?) {
        if (url == null && htmlDocument == null) throw Exception("Html and url params mustn't be null at the same time");

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val printService = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val adapter = view?.createPrintDocumentAdapter("web print")
                    printService.print("Fatura Yazdırma", adapter!!, PrintAttributes.Builder().build())
                } else {
                    throw Exception("Android 5 and higher versions are supported.")
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.clearCache(true)
        if (url != null)
            webView.loadUrl(url)
        else if (htmlDocument != null)
            webView.loadDataWithBaseURL(null, htmlDocument, "text/HTML", "UTF-8", null)
    }

    private fun isOpenBluetooth(activity: Activity): Boolean {
        val bluetoothManager = activity.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableBtIntent, REQUEST_OPEN_BLUETOOTH_INTENT)
            return false;
        }
        return true;
    }

    fun getPairedDevices(activity: Activity):
            List<BluetoothPrintDevice> {
        if (isOpenBluetooth(activity)) {
            val bluetoothManager = activity.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            return pairedDevices?.map { device -> BluetoothPrintDevice(device.name, device.address) }
                ?: listOf()
        }
        throw Exception("Bluetooth cihazı açık değil.")
    }


    fun openBluetoothSetting(activity: Activity) {
        activity.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
    }

}

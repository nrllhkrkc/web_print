package com.akuple.web_print

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
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

    fun print(context: Context, url: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val printService = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                    val adapter = view?.createPrintDocumentAdapter("web print")
                    printService.print("web print", adapter!!, PrintAttributes.Builder().build())
                } else {
                    throw Exception("Android 5 and higher versions are supported.")
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.clearCache(true)
        webView.loadUrl(url)
    }

    private fun isOpenBluetooth(activity: Activity): Boolean {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
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
            val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
            return pairedDevices?.map { device -> BluetoothPrintDevice(device.name, device.address) }
                    ?: listOf()
        }
        throw Exception("Bluetooth cihazı açık değil.")
    }

}
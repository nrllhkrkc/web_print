package com.akuple.web_print

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

object WebPrintUtils {
    fun print(context: Context, url: String) {
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
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
}
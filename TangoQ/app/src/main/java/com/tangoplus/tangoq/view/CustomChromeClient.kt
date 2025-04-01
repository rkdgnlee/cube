package com.tangoplus.tangoq.view

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.net.toUri
import com.tangoplus.tangoq.R


class CustomWebChromeClient(private val context: Context) : WebChromeClient() {

    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        val webView = view?.context?.let { WebView(it) }
        val webSettings = webView?.settings
        webSettings?.javaScriptEnabled = true

        webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()

                if (url.startsWith("intent:")) {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        val existPackage = intent.getPackage()?.let { context.packageManager.getLaunchIntentForPackage(it) }

                        if (existPackage != null) {
                            context.startActivity(intent)
                        } else {
                            val marketIntent = Intent(Intent.ACTION_VIEW)
                            marketIntent.data = "market://details?id=${intent.getPackage()}".toUri()
                            context.startActivity(marketIntent)
                        }
                        return true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }
        // WebView 설정 완료 후 결과 메시지 처리
        val transport = resultMsg?.obj as WebView.WebViewTransport
        transport.webView = webView
        resultMsg.sendToTarget()
        return true
    }
}
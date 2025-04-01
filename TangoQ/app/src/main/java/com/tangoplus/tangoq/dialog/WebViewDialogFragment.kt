package com.tangoplus.tangoq.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.databinding.FragmentWebViewDialogBinding
import com.tangoplus.tangoq.viewmodel.SignInViewModel
import androidx.core.net.toUri
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.api.NetworkUser.sendMOKResult
import com.tangoplus.tangoq.view.CustomWebChromeClient
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder


class WebViewDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentWebViewDialogBinding
    private val svm: SignInViewModel by activityViewModels()
    private var urlInfo = "https://gym.tangoplus.co.kr/api/mok/mok.html"
//    private var urlInfo = "https://www.naver.com/"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentWebViewDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


//        val headers = mapOf("Referer" to urlInfo)

        binding.wv.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            loadUrl(urlInfo)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    request?.let {

                        Log.d("WebView", "URL: ${it.url}")
                        Log.d("WebView", "URL: ${it.requestHeaders}")
                    }
                    return false // true로 할경우 pass창이 나오지 않음.
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {

                    val url = request?.url.toString()
                    if (url.contains("https://gym.tangoplus.co.kr/api/")) { // 리디렉트 URL 체크
                        val connection = URL(url).openConnection() as HttpURLConnection
                        connection.requestMethod = "GET"
                        val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                        val jsonObject = JSONObject(jsonString)
                        Log.d("AuthResult", jsonObject.toString())

                        // 필요하면 메인 스레드에서 UI 업데이트
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(view?.context, "인증 완료: ${jsonObject.toString()}", Toast.LENGTH_LONG).show()
                        }

                        return WebResourceResponse("application/json", "UTF-8", connection.inputStream)
                    }
                    return super.shouldInterceptRequest(view, request)
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                    val newWebView = WebView(view?.context!!).apply {
                        settings.javaScriptEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        webChromeClient = this@apply.webChromeClient
                    }

                    val dialog = Dialog(view.context).apply {
                        setContentView(newWebView)
                        show()
                    }

                    (resultMsg?.obj as WebView.WebViewTransport).webView = newWebView
                    resultMsg.sendToTarget()
                    return true
                }
            }


        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }


}
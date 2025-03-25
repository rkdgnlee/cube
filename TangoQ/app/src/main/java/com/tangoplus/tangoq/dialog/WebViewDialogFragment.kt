package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
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
import java.net.URLDecoder


class WebViewDialogFragment : DialogFragment() {
    private lateinit var binding: FragmentWebViewDialogBinding
    private val svm: SignInViewModel by activityViewModels()
    private var urlInfo = "https://scert.mobile-ok.com/resources/js/index.js"
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentWebViewDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // WebView 설정
        binding.wv.apply {
            loadUrl(urlInfo)
            settings.javaScriptEnabled = true
            getSettings().domStorageEnabled = true //필수설정(true)
            getSettings().javaScriptCanOpenWindowsAutomatically = true //필수설정(true)
            getSettings().cacheMode = WebSettings.LOAD_NO_CACHE
            getSettings().loadsImagesAutomatically = true
            getSettings().builtInZoomControls = true
            getSettings().setSupportZoom(true)
            getSettings().setSupportMultipleWindows(true)
            getSettings().loadWithOverviewMode = true
            getSettings().useWideViewPort = true

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()

                    // 특정 도메인이나 경로 체크
                    if (url.contains(getString(R.string.API_user) + "mok/mok_std_result.jsp")) {
                        try {
                            val uri = url.toUri()
                            val decodedData = URLDecoder.decode(uri.getQueryParameter("data"), "UTF-8") // json이 아니기 때문에 실제로 "data"라는 걸 넣으면 응답 JSON에 들어가있는 값에 접근할 수 있음.
                            val jsonObject = JSONObject(decodedData)
                            val encryptedMOKToken = jsonObject.getString("encryptMOKKeyToken")

                            // 토큰 처리 로직
                            handleMOKTokenResponse(encryptedMOKToken)

                            return true  // URL 로딩 중단
                        } catch (e: Exception) {
                            Log.e("MOKToken", "토큰 처리 중 오류", e)
                            return false
                        }
                    }
                    return false
                }

//                override fun onPageFinished(view: WebView?, url: String?) {
//                    super.onPageFinished(view, url)
//                    if (url?.contains("your_result_url") == true) {
//                        // WebView에서 결과 데이터 추출
//                        view?.evaluateJavascript(
//                            "(function() { return document.body.innerText; })();",
//                            ValueCallback { result ->
//                                // result에서 encryptMOKKeyToken 파싱
//                                val jsonResult = JSONObject(result)
//                                val mokToken = jsonResult.getString("encryptMOKKeyToken")
//                                handleMOKTokenResponse(mokToken)
//
//                            }
//                        )
//                    }
//                }
            }
            webChromeClient = CustomWebChromeClient(requireContext()) // PASS 앱 관련 처리

//            binding.wv.addJavascriptInterface(object {
//                @JavascriptInterface
//                fun onAuthComplete() {
//                    svm.passName.value = ""  // TODO 1. 본인 인증 완료됐을 때 넘겨줘야하는 값들.
//                    svm.passMobile.value = ""  // TODO 2. 본인 인증 완료됐을 때 넘겨줘야하는 값들.
//                }
//            }, "AndroidBridge")
//            val htmlData = """
//                    <html>
//                    <head>
//                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
//                    </head>
//                    <body>
//                        <iframe src="$urlInfo" width="100%" height="100%" style="border:none;"></iframe>
//                    </body>
//                    </html>
//                """.trimIndent()
////            loadUrl(urlInfo)
//            loadDataWithBaseURL(null, htmlData, "text/html", "UTF-8", null)

        }
    }

    private fun handleMOKTokenResponse(encryptedToken: String) {
        val jo = JSONObject().apply {
            put("encryptMOKKeyToken", encryptedToken)
        }
        sendMOKResult(getString(R.string.API_user), jo.toString()) { resultJo ->
            if (resultJo != null) {
                svm.passName.value = resultJo.optString("user_name")
                svm.passMobile.value = resultJo.optString("mobile")
                dismiss()
            } else {
                Toast.makeText(requireContext(), "올바르지 않은 요청입니다. 인증을 다시 해주세요", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
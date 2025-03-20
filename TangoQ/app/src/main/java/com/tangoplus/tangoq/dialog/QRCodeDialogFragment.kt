package com.tangoplus.tangoq.dialog

import android.Manifest
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignBottom
import com.skydoves.balloon.showAlignStart
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentQRCodeDialogBinding
import com.tangoplus.tangoq.api.NetworkUser.loginWithPin
import com.tangoplus.tangoq.api.NetworkUser.loginWithQRCode
import com.tangoplus.tangoq.db.Singleton_t_user
import com.tangoplus.tangoq.function.SaveSingletonManager
import `in`.aabhasjindal.otptextview.OTPListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


class QRCodeDialogFragment : DialogFragment() {
    lateinit var binding : FragmentQRCodeDialogBinding
    private var userJson = JSONObject()
    private val CAMERA_PERMISSION_CODE = 100
    private lateinit var codeScanner : CodeScanner


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQRCodeDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // ------! 바코드 스캔 시작 !------
        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject ?: JSONObject()

        val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        // ------! balloon 시작 !------
        binding.ibtnLSDBack.setOnClickListener {
            dismiss()
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
        binding.ibtnLSDInfo.setOnClickListener {
            balloonCallback {}
        }
        balloonCallback {

            binding.otvLSD.requestFocus()
            binding.otvLSD.postDelayed({
                binding.otvLSD.requestFocus()
                imm.showSoftInput(binding.otvLSD, InputMethodManager.SHOW_IMPLICIT)
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
            }, 250)
        }

        binding.tlLSD.addOnTabSelectedListener(object: OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        hideBarcodeView()
                        binding.clLSD.visibility = View.VISIBLE
                        binding.flLSD.visibility = View.GONE
                    }
                    1 -> {
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                        checkCameraPermission()
                        showBarcodeView()
                        binding.clLSD.visibility = View.GONE
                        binding.flLSD.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        binding.tlLSD.selectTab(binding.tlLSD.getTabAt(0))



        // ------! PIN 번호 관리 시작 !------
        binding.otvLSD.otpListener = object : OTPListener {
            override fun onInteractionListener() {}
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onOTPComplete(otp: String) {
                // -----! 완료 했을 경우 !------
//                Snackbar.make(binding.clLSD, "데이터를 전송했습니다. 잠시만 기다려주세요", Snackbar.LENGTH_LONG).show()

                // json으로 변환해서 보내기
                lifecycleScope.launch {
                    CoroutineScope(Dispatchers.IO).launch {

                        val status = loginWithPin(getString(R.string.API_kiosk), otp.toInt(), userJson.optString("user_uuid"))
                        withContext(Dispatchers.Main) {
                            when (status) {
                                200 -> {
                                    Toast.makeText(requireContext(), "데이터를 전송했습니다. 잠시만 기다려주세요", Toast.LENGTH_LONG).show()
                                    Handler(Looper.getMainLooper()).postDelayed({  binding.otvLSD.setOTP("") }, 500)
                                }
                                401 -> {
                                    Toast.makeText(requireContext(), "인증이 올바르지 않습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_LONG).show()
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        binding.otvLSD.setOTP("")
                                    }, 500)
                                }
                                404 -> {
                                    Toast.makeText(requireContext(), "연결에 실패했습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_LONG).show()
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        binding.otvLSD.setOTP("")
                                    }, 500)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initScanner() {
        try {
            codeScanner = CodeScanner(requireContext(), binding.csvLSD)
        } catch (e: IndexOutOfBoundsException) {
            Log.e("ProgramIndex", "${e.message}")
        } catch (e: IllegalArgumentException) {
            Log.e("ProgramIllegal", "${e.message}")
        } catch (e: IllegalStateException) {
            Log.e("ProgramIllegal", "${e.message}")
        } catch (e: NullPointerException) {
            Log.e("ProgramNull", "${e.message}")
        } catch (e: java.lang.Exception) {
            Log.e("ProgramException", "${e.message}")
        }
        codeScanner.startPreview()
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.SINGLE // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.decodeCallback = DecodeCallback {
            lifecycleScope.launch {
                CoroutineScope(Dispatchers.IO).launch {
                    Log.v("decodeResult", it.text)
                    val status = loginWithQRCode(getString(R.string.API_kiosk), userJson.optString("user_uuid"))
                    withContext(Dispatchers.Main) {
                        when (status) {
                            200 -> {
                                Toast.makeText(requireContext(), "데이터를 전송했습니다. 잠시만 기다려주세요", Toast.LENGTH_LONG).show()
                                Handler(Looper.getMainLooper()).postDelayed({}, 500)
                            }
                            400 -> { Toast.makeText(requireContext(), "인증이 올바르지 않습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_LONG).show() }
                            404 -> { Toast.makeText(requireContext(), "연결에 실패했습니다. 잠시 후 다시 시도해주세요", Toast.LENGTH_LONG).show() }
                        }
                    }
                }
            }
        }

    }
    private fun balloonCallback(callback: () -> Unit) {
        val balloon = Balloon.Builder(requireContext())
            .setWidth(BalloonSizeSpec.WRAP)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("탱고바디 키오스크로 로그인을 위한 화면입니다\n탱고바디 화면의 6자리 PIN번호를 입력해주세요")
            .setTextColorResource(R.color.subColor800)
            .setTextSize(20f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowSize(0)
            .setMargin(6)
            .setPadding(12)
            .setCornerRadius(8f)
            .setBackgroundColorResource(R.color.white)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .setOnBalloonDismissListener { callback() }
            .build()
        binding.textView20.showAlignBottom(balloon)
        balloon.dismissWithDelay(3000L)
        balloon.setOnBalloonClickListener { balloon.dismiss() }
    }
    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 권한이 승인된 경우 스캐너를 초기화합니다.
                    initScanner()
                } else {
                    // 권한이 거부된 경우 사용자에게 알림을 표시합니다.
                    Toast.makeText(requireContext(), "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            // 권한이 있을 경우, 스캐너를 초기화합니다.
            initScanner()
        } else {
            // 권한이 없을 경우, 권한을 요청합니다.
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
        }
    }

    private fun hideBarcodeView() {
        codeScanner.releaseResources()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.otvLSD.requestFocus()
            binding.otvLSD.isFocusableInTouchMode = true
            // 소프트 키보드를 강제로 띄우기
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.otvLSD, InputMethodManager.SHOW_IMPLICIT)
        }, 500)

//        val imm = context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
//        imm!!.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showBarcodeView() {
        binding.csvLSD.visibility = View.VISIBLE
        binding.otvLSD.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            initScanner()
            codeScanner.startPreview()
        }

    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        checkCameraPermission()
    }
}
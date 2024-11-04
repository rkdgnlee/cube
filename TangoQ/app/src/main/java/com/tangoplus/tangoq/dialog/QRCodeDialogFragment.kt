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
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignEnd
import com.skydoves.balloon.showAlignStart
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentQRCodeDialogBinding
import com.tangoplus.tangoq.`object`.NetworkUser.loginWithPin
import com.tangoplus.tangoq.`object`.NetworkUser.loginWithQRCode
import com.tangoplus.tangoq.`object`.Singleton_t_user
import `in`.aabhasjindal.otptextview.OTPListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject


class QRCodeDialogFragment : DialogFragment() {
    lateinit var binding : FragmentQRCodeDialogBinding
    private lateinit var behavior: BottomSheetBehavior<ConstraintLayout>
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
        // ------! 바코드 스캔 시작 !------
        checkCameraPermission()

        binding.ibtnLSDInfo.alpha = 0f
        binding.ibtnLSDInfo.visibility = View.GONE
        binding.ibtnLSDBack2.visibility = View.GONE
        userJson = Singleton_t_user.getInstance(requireContext()).jsonObject!!

        // ------! balloon 시작 !------
        val balloon = Balloon.Builder(requireContext())
            .setWidthRatio(0.6f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("탱고바디 화면에 나오는 6자리 PIN번호를 입력해주세요")
            .setTextColorResource(R.color.subColor800)
            .setTextSize(18f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowSize(0)
            .setMargin(6)
            .setPadding(12)
            .setCornerRadius(8f)
            .setBackgroundColorResource(R.color.white)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        binding.ibtnLSDInfo.setOnClickListener {
            binding.ibtnLSDInfo.showAlignStart(balloon)
            balloon.dismissWithDelay(3000L)
        }

        // ------! behavior + 바텀시트 조작 시작 !------
        val isTablet = resources.configuration.screenWidthDp >= 600
        behavior = BottomSheetBehavior.from(binding.clLSD)
        val screenHeight = resources.displayMetrics.heightPixels
        val topSpaceHeight = resources.getDimensionPixelSize(com.tangoplus.tangoq.R.dimen.top_space_height_qr_code)
        val peekHeight = screenHeight - topSpaceHeight
        behavior.apply {
            this.peekHeight = peekHeight
            isFitToContents = false
            expandedOffset = 0
            state = BottomSheetBehavior.STATE_COLLAPSED
            skipCollapsed = false
            // 기기 유형에 따라 halfExpandedRatio 설정
            halfExpandedRatio = if (isTablet) {
                0.65f
            } else {
                0.99f
            }
        }

        behavior.addBottomSheetCallback(object: BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        hideBarcodeView()
                    }
                    BottomSheetBehavior.STATE_COLLAPSED -> showBarcodeView()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.ibtnLSDInfo.visibility = View.VISIBLE
                binding.ibtnLSDBack2.visibility = View.VISIBLE
                binding.ibtnLSDInfo.alpha = slideOffset
                binding.ibtnLSDBack2.alpha = slideOffset

                binding.csvLSD.alpha = 1 - slideOffset
            }
        })
        // ------! behavior + 바텀시트 조작 끝 !------


        // ------! PIN 번호 관리 시작 !------
        binding.otvLSD.otpListener = object : OTPListener {
            override fun onInteractionListener() {}
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onOTPComplete(otp: String) {
                // -----! 완료 했을 경우 !------
//                Snackbar.make(binding.clLSD, "데이터를 전송했습니다. 잠시만 기다려주세요", Snackbar.LENGTH_LONG).show()

                // TODO json으로 변환해서 보내기
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
        binding.ibtnLSDBack1.setOnClickListener { dismiss() }
        binding.ibtnLSDBack2.setOnClickListener { dismiss() }
    }

    private fun initScanner() {
        try {
            codeScanner = CodeScanner(requireContext(), binding.csvLSD)
        } catch (e: Exception) {
            Log.e("codeScannerError", e.message!!)
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
        binding.csvLSD.visibility = View.GONE
        codeScanner.releaseResources()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.otvLSD.requestFocus()
            binding.otvLSD.isFocusableInTouchMode = true

            // 소프트 키보드를 강제로 띄우기
            val imm = requireActivity().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.otvLSD, InputMethodManager.SHOW_IMPLICIT)
        }, 350)

//        val imm = context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
//        imm!!.hideSoftInputFromWindow(view.windowToken, 0)

    }

    private fun showBarcodeView() {
        binding.csvLSD.visibility = View.VISIBLE
        binding.otvLSD.visibility = View.VISIBLE
        binding.ibtnLSDInfo.visibility = View.GONE
        binding.ibtnLSDBack2.visibility = View.GONE
        codeScanner.startPreview()

    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        checkCameraPermission()
    }
}
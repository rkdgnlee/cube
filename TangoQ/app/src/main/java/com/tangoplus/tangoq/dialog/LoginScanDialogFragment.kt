package com.tangoplus.tangoq.dialog

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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignEnd
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentLoginScanDialogBinding
import `in`.aabhasjindal.otptextview.OTPListener
import org.json.JSONObject


class LoginScanDialogFragment : DialogFragment() {
    lateinit var binding : FragmentLoginScanDialogBinding
    private lateinit var behavior: BottomSheetBehavior<ConstraintLayout>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginScanDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ------! 바코드 스캔 시작 !------
        initScanner()
        binding.ibtnLSDInfo.alpha = 0f
        binding.ibtnLSDInfo.visibility = View.GONE
        binding.ibtnLSDBack2.visibility = View.GONE

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
                    BottomSheetBehavior.STATE_EXPANDED -> hideBarcodeView()
                    BottomSheetBehavior.STATE_COLLAPSED -> showBarcodeView()
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                binding.ibtnLSDInfo.visibility = View.VISIBLE
                binding.ibtnLSDBack2.visibility = View.VISIBLE
                binding.ibtnLSDInfo.alpha = slideOffset
                binding.ibtnLSDBack2.alpha = slideOffset
                binding.bvLSD.alpha = 1 - slideOffset
            }
        })
        // ------! behavior + 바텀시트 조작 끝 !------


        // ------! PIN 번호 관리 시작 !------
        binding.otvLSD.otpListener = object : OTPListener {
            override fun onInteractionListener() {}
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onOTPComplete(otp: String) {
                // -----! 완료 했을 경우 !------
//                Snackbar.make(binding.root, "데이터를 전송했습니다. 잠시만 기다려주세요", Snackbar.LENGTH_LONG).show()
                Toast.makeText(requireContext(), "데이터를 전송했습니다. 잠시만 기다려주세요", Toast.LENGTH_LONG).show()
                val jo = JSONObject()
                jo.put("pin", binding.otvLSD.otp)
                // TODO jo로 담아서 보내기


            }
        }
        binding.ibtnLSDBack1.setOnClickListener { dismiss() }
        binding.ibtnLSDBack2.setOnClickListener { dismiss() }

        // ------! balloon 시작 !------
        val balloon = Balloon.Builder(requireContext())
            .setWidthRatio(0.6f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("탱고바디 화면에 나오는 6자리 PIN번호를 입력해주세요")
            .setTextColorResource(R.color.subColor800)
            .setTextSize(15f)
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
                binding.ibtnLSDInfo.showAlignEnd(balloon)
                balloon.dismissWithDelay(2500L)
        }
    }

    private fun initScanner() {
        binding.bvLSD.decodeContinuous{ result ->

            // TODO barcodeResult라는 데이터에서
            Log.v("barcode", "text: ${result.result.text}, timestamp: ${result.result.timestamp}, rawBytes: ${result.result.rawBytes}, metaData: ${result.result.resultMetadata}")
            binding.bvLSD.pause()
            Handler(Looper.getMainLooper()).postDelayed({binding.bvLSD.resume()}, 500)
//            dismiss()
            Snackbar.make(binding.clLSD, "인증에 성공하였습니다 ! 기기를 확인해주세요", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun hideBarcodeView() {
        binding.bvLSD.visibility = View.GONE

        binding.bvLSD.pause()
    }

    private fun showBarcodeView() {
        binding.bvLSD.visibility = View.VISIBLE
        binding.ibtnLSDInfo.visibility = View.GONE
        binding.ibtnLSDBack2.visibility = View.GONE
        binding.bvLSD.resume()
        initScanner()
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        showBarcodeView()
    }
}
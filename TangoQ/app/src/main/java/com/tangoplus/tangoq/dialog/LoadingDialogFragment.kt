package com.tangoplus.tangoq.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.tangoplus.tangoq.databinding.FragmentLoadingDialogBinding

class LoadingDialogFragment : DialogFragment() {
    lateinit var binding : FragmentLoadingDialogBinding

    companion object{
        private const val ARG_CASE = "loading_case"
        fun newInstance(case: String): LoadingDialogFragment {
            val fragment = LoadingDialogFragment()
            val args = Bundle()
            args.putString(ARG_CASE, case)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoadingDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val arg = arguments?.getString(ARG_CASE)!!


        binding.tvLD.text  = when (arg) {
            "로그인" -> "로그인 중입니다\n잠시만 기다려주세요"
            "측정이력" -> "측정 기록을 가져오고 있습니다\n잠시만 기다려주세요"
            "측정파일" -> "측정 결과를 가져오고 있습니다\n잠시만 기다려주세요"
            "추천" -> "결과를 바탕으로\n추천 운동을 가져오고 있습니다\n잠시만 기다려주세요"
            else -> "로딩중입니다"
        }
    }

    override fun onResume() {
        super.onResume()
        isCancelable = false
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog?.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.window?.setDimAmount(0.6f) // 원하는 만큼의 어둠 설정
        dialogFragmentResize()
    }
    private fun dialogFragmentResize() {
        val windowManager = context?.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        if (Build.VERSION.SDK_INT < 30) {
            val display = windowManager.defaultDisplay
            val size = Point()

            display.getSize(size)

            val window = dialog?.window

            val x = (size.x * 0.6f).toInt()
            val y = (size.y *  0.175f).toInt()
            window?.setLayout(x, y)
        } else {
            val rect = windowManager.currentWindowMetrics.bounds

            val window = dialog?.window

            val x = (rect.width() * 0.6f).toInt()
            val y = (rect.height() *  0.175f).toInt()

            window?.setLayout(x, y)
        }
    }
}
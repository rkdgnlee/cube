package com.example.mhg.Dialog

import android.annotation.SuppressLint
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
import com.example.mhg.databinding.DialogfragmentExerciseLoadBinding


class ExerciseLoadDialogFragment : DialogFragment() {
    lateinit var binding: DialogfragmentExerciseLoadBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true

    }
    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogfragmentExerciseLoadBinding.inflate(inflater)
        // ----! local Room에 연결 코루틴 !-----
//        val db = ExerciseDatabase.getInstance(requireContext())
//        lifecycleScope.launch {
//            val jsonArr = NetworkService.fetchExerciseJson(getString(R.string.IP_ADDRESS_t_Exercise_Description))
//            Log.w(TAG, "jsonArr: $jsonArr")
//            if (jsonArr != null) {
//                try {
//                    ExerciseRepository(db.ExerciseDao()).StoreExercises(jsonArr)
//                } catch (e: Exception) {
//                    Log.e(TAG, "Error storing exercises", e)
//                }
//            }
//            // -----! 통신완료된 상태에서 메인스레드 접근 !-----
//            withContext(Dispatchers.Main) {
//                binding.tvExerciseLoad.text = "완료됐습니다!\n페이지를 이동할게요!"
//                binding.progressBar3.progress = binding.progressBar3.max
//                delay(500)
//                val intent = Intent(requireContext(), PersonalSetupActivity::class.java)
//                startActivity(intent)
//                dismiss()
//            }
//        }
        return binding.root
    }



//    override fun onStart() {
//        super.onStart()
//        initializeDialogOptions()
//    }
//
//    private fun initializeDialogOptions() {
//        val darkTransparentBlack = Color.argb((255 * 0.6).toInt(), 0, 0, 0)
//        dialog?.window?.setBackgroundDrawable(ColorDrawable(darkTransparentBlack))
//        dialog?.window?.setLayout(
//            ViewGroup.LayoutParams.MATCH_PARENT,
//            ViewGroup.LayoutParams.MATCH_PARENT
//        )
//        dialog?.window?.setDimAmount(0.4f)
//        isCancelable = false
//    }

//    override fun onResume() {
//        super.onResume()
//        context?.dialogFragmentResize(ExerciseLoadDialogFragment(), 0.9f, 0.9f)
//    }
//    fun Context.dialogFragmentResize(dialogFragment: DialogFragment, width: Float, height: Float) {
//        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//
//        if (Build.VERSION.SDK_INT < 30) {
//            val display = windowManager.defaultDisplay
//            val size = Point()
//
//            display.getSize(size)
//            val window = dialogFragment.dialog?.window
//
//            val x = (size.x * width).toInt()
//            val y = (size.y * height).toInt()
//            window?.setLayout(x, y)
//        } else {
//            dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//
//            val rect = windowManager.currentWindowMetrics.bounds
//            val window = dialogFragment.dialog?.window
//            val x = (rect.width()* width).toInt()
//            val y = (rect.height()* height).toInt()
//
//            window?.setLayout(x, y)
//        }
//    }
}

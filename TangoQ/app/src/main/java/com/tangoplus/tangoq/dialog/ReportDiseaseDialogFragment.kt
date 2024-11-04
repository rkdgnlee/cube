package com.tangoplus.tangoq.dialog

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentReportDiseaseDialogBinding
import com.tangoplus.tangoq.fragment.ExerciseFragment
import java.io.File
import java.io.FileOutputStream


class ReportDiseaseDialogFragment : DialogFragment() {
    lateinit var binding : FragmentReportDiseaseDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportDiseaseDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.btnRDShare.setOnClickListener {
            val bitmap = Bitmap.createBitmap(binding.constraintLayout47.width, binding.constraintLayout47.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            binding.constraintLayout47.draw(canvas)

            val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "shared_image.jpg")
            val fileOutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
            fileOutputStream.close()

            val fileUri = FileProvider.getUriForFile(requireContext(), context?.packageName + ".provider", file)
            // ------! 그래프 캡처 끝 !------
            val url = Uri.parse("https://tangopluscompany.github.io/deep-link-redirect/#/4")
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "image/png" // 이곳에서 공유 데이터 변경
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.putExtra(Intent.EXTRA_TEXT, "제 질병 예측 결과를 공유하고 싶어요 !\n$url")
            startActivity(Intent.createChooser(intent, "질병 예측 결과"))
        }

        // TODO ------# 관계에 맞는 값들을 넣어야함. #------
        binding.btnRDRecommend.setOnClickListener {
            val bnb : BottomNavigationView = requireActivity().findViewById(R.id.bnbMain)
            bnb.selectedItemId = R.id.exercise
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}
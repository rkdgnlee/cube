package com.tangoplus.tangoq.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.databinding.FragmentExerciseBSDialogBinding


class ExerciseBSDialogFragment : BottomSheetDialogFragment() {
    lateinit var binding : FragmentExerciseBSDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExerciseBSDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bundle = arguments
        val exerciseUnit = bundle?.getParcelable<ExerciseVO>("ExerciseUnit")

        binding.tvEBSName.text = exerciseUnit?.exerciseName
//        binding.ivFrBsThumbnail.setImageResource(R.drawable)
        binding.llEBsPlay.setOnClickListener {
            dismiss()
            val dialogFragment = PlayThumbnailDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable("ExerciseUnit", exerciseUnit)
                }
            }
            dialogFragment.show(requireActivity().supportFragmentManager, "PlayThumbnailDialogFragment")
//            requireActivity().supportFragmentManager.beginTransaction().apply {
//                replace(R.id.flMain, DialogFragment)
//                commit()
//            }
        }
        binding.ibtnEBsExit.setOnClickListener {
            dismiss()
        }

        // ------! 공유하기 시작 !------
        binding.llEBSShare.setOnClickListener {
//            val bitmap = Bitmap.createBitmap(binding.ClMs.width, binding.ClMs.height, Bitmap.Config.ARGB_8888)
//            val canvas = Canvas(bitmap)
//            binding.ClMs.draw(canvas)
//
//            val file = File(context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "shared_image.jpg")
//            val fileOutputStream = FileOutputStream(file)h
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
//            fileOutputStream.flush()
//            fileOutputStream.close()
//
//            val fileUri = FileProvider.getUriForFile(requireContext(), context?.packageName + ".provider", file)
//            val intent = Intent(Intent.ACTION_SEND)
//            intent.type = "image/png" // 이곳에서 공유 데이터 변경
//            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
//            intent.putExtra(Intent.EXTRA_TEXT, "제 밸런스 그래프를 공유하고 싶어요 !")
//            startActivity(Intent.createChooser(intent, "밸런스 그래프"))
        }


    }
}
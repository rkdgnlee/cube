package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.tangoplus.tangoq.databinding.FragmentPoseViewDialogBinding


class PoseViewDialogFragment : DialogFragment() {
    lateinit var binding : FragmentPoseViewDialogBinding


    companion object {
        private const val ARG_IMAGE_PATH = "image_path"

        fun newInstance(imagePath: String): PoseViewDialogFragment {
            val args = Bundle()
            args.putString(ARG_IMAGE_PATH, imagePath)
            val fragment = PoseViewDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPoseViewDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibtnPVDExit.setOnClickListener {
            dismiss()
        }
        // TODO DB에서 URL받아와서 할껀지, 아니면 그냥 저장소꺼 가져올껀지? 근데 db가 맞음
        val imagePath = arguments?.getString(ARG_IMAGE_PATH)
        if (imagePath != null) {
            Glide.with(this)
                .load(imagePath)
                .into(binding.ivPVDPose)
        }

    }

    override fun onResume() {
        super.onResume()
        // full Screen code
//        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
//        dialog?.window?.setDimAmount(0.4f)
//        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        val darkTransparentBlack = Color.argb((255 * 0.6).toInt(), 0, 0, 0)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(darkTransparentBlack))
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setDimAmount(0.4f)
    }




}
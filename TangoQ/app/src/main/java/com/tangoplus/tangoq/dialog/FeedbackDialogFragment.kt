package com.tangoplus.tangoq.dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.databinding.FragmentFeedbackDialogBinding

class FeedbackDialogFragment : DialogFragment() {
    lateinit var binding: FragmentFeedbackDialogBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedbackDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.btnFbSubmit.setOnClickListener{
            val intent = Intent(requireActivity(), MainActivity::class.java)
            startActivity(intent)
            requireActivity().finishAffinity()
        }

        binding.btnFbPainPartSelect.setOnClickListener {
            val dialog = FeedbackPartDialogFragment()
            dialog.show(requireActivity().supportFragmentManager, "FeedbackPartDialogFragment")
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
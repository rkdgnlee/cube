package com.tangoplus.tangoq.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.data.FavoriteViewModel
import com.tangoplus.tangoq.databinding.FragmentFeedbackDialogBinding
import com.tangoplus.tangoq.`object`.Singleton_t_user
import org.json.JSONObject

class FeedbackDialogFragment : DialogFragment() {
    lateinit var binding: FragmentFeedbackDialogBinding
    val viewModel: FavoriteViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentFeedbackDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val t_userdata = Singleton_t_user.getInstance(requireContext())
        val userJson= t_userdata.jsonObject?.getJSONObject("data")

        binding.tvFTime.text = "${viewModel.exerciseLog.value?.first.toString()} 초" ?: "0"
        binding.tvFCount.text = viewModel.exerciseLog.value?.second ?: "0"

        binding.btnFbSubmit.setOnClickListener{
            // TODO 점수 보내기
            val jsonObj = JSONObject()
            jsonObj.put("user_email", userJson?.optString("user_email"))
            jsonObj.put("fatigue_score",getCheckedRadioButtonIndex(binding.rgFatigue!!))
            jsonObj.put("satisfaction_score",getCheckedRadioButtonIndex(binding.rgSatisfaction!!))
            jsonObj.put("intensity_score",getCheckedRadioButtonIndex(binding.rgIntensity!!))

            Log.v("피드백 점수", "$jsonObj")
//            val intent = Intent(requireActivity(), MainActivity::class.java)
//            startActivity(intent)
//            requireActivity().finishAffinity()
            dismiss()
            viewModel.exerciseLog.value = null
            viewModel.isDialogShown.value = true
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
    fun getCheckedRadioButtonIndex(radioGroup: RadioGroup): Int {
        val checkedRadioButtonId = radioGroup.checkedRadioButtonId
        val radioButton = radioGroup.findViewById<RadioButton>(checkedRadioButtonId)
        return radioGroup.indexOfChild(radioButton)
    }
}
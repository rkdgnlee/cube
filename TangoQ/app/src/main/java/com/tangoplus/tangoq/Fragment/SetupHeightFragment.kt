package com.tangoplus.tangoq.Fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.ViewModel.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentSetupHeightBinding


class SetupHeightFragment : Fragment() {
    lateinit var binding: FragmentSetupHeightBinding
    val viewModel: UserViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupHeightBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
    override fun onPause() {
        super.onPause()
        val height = binding.etSuHeight.text
        Log.w("키", "$height")
        viewModel.User.value?.put("user_height", height)
    }
}
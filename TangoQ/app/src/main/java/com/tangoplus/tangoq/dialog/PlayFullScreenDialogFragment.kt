package com.tangoplus.tangoq.dialog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentPlayFullScreenDialogBinding

class PlayFullScreenDialogFragment : Fragment() {
    private lateinit var binding : FragmentPlayFullScreenDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPlayFullScreenDialogBinding.inflate(inflater)
        return binding.root
    }

}
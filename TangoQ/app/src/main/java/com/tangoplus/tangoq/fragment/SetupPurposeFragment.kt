package com.tangoplus.tangoq.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.databinding.FragmentSetupPurposeBinding

class SetupPurposeFragment : Fragment() {
   lateinit var binding : FragmentSetupPurposeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupPurposeBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setCbTextColor(binding.checkBox)
        setCbTextColor(binding.checkBox2)
        setCbTextColor(binding.checkBox3)
        setCbTextColor(binding.checkBox4)
        setCbTextColor(binding.checkBox5)

    }

    private fun setCbTextColor(checkBox: CheckBox) {
        checkBox.setOnCheckedChangeListener{buttonView, isChecked ->
            when (isChecked) {
                true -> checkBox.setTextColor(binding.checkBox.resources.getColor(R.color.mainColor))
                false -> checkBox.setTextColor(binding.checkBox.resources.getColor(R.color.subColor700))
            }
        }
    }
}
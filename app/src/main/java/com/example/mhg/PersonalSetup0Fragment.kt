package com.example.mhg

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.FragmentPersonalSetup0Binding
import com.google.gson.JsonObject
import org.json.JSONObject


class PersonalSetup0Fragment : Fragment() {
    private lateinit var viewModel: UserViewModel
    lateinit var binding : FragmentPersonalSetup0Binding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPersonalSetup0Binding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val JsonObj = JSONObject()
        JsonObj.put("user_name", "")
//        fetchJson(R.string.IP_ADDRESS.toString(), JsonObj.toString(), "PUT")


    }

}

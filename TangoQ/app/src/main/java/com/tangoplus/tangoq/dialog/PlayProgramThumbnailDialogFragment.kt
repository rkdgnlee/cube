package com.tangoplus.tangoq.dialog

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.databinding.FragmentPlayProgramThumbnailDialogBinding


class PlayProgramThumbnailDialogFragment : DialogFragment() {
    lateinit var binding : FragmentPlayProgramThumbnailDialogBinding
    lateinit var program : ProgramVO

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayProgramThumbnailDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        binding.nsvPPTD.isNestedScrollingEnabled = true
//        binding.rvPPTD.overScrollMode = View.OVER_SCROLL_NEVER
//        binding.rvPPTD.isNestedScrollingEnabled = false

        val bundle = arguments
        program = bundle?.getParcelable("Program")!!

        binding.tvPPTDName.text = program.programName
        binding.tvPPTDExplain.text=  program.programDescription

        val adapter = ExerciseRVAdapter(this@PlayProgramThumbnailDialogFragment, program.exercises!!, listOf(), "main")
        binding.rvPPTD.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(requireContext() ,LinearLayoutManager.VERTICAL, false)
        binding.rvPPTD.layoutManager = linearLayoutManager

        binding.ibtnPPTDBack.setOnClickListener { dismiss() }
        binding.btnPPTDPlay.setOnClickListener {
            val urls = storePickUrl(program.exercises!!)
            val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
            intent.putStringArrayListExtra("urls", ArrayList(urls))
            requireContext().startActivity(intent)
            startActivityForResult(intent, 8080)
        }
    }
    override fun onResume() {
        super.onResume()
        // full Screen code
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
    private fun storePickUrl(currentItem : MutableList<ExerciseVO>) : MutableList<String> {
        val urls = mutableListOf<String>()
        for (i in currentItem.indices) {
            val exercise = currentItem[i]
            urls.add(exercise.videoFilepath.toString())
        }
        Log.v("urls", "${urls}")
        return urls
    }
}
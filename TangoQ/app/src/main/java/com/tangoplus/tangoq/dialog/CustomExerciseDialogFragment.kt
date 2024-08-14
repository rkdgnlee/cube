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
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import com.skydoves.balloon.showAlignBottom
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.ExerciseCategoryRVAdapter
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ProgramVO
import com.tangoplus.tangoq.data.UserViewModel
import com.tangoplus.tangoq.databinding.FragmentCustomExerciseDialogBinding
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.listener.OnCategoryClickListener
import com.tangoplus.tangoq.`object`.Singleton_t_user

class CustomExerciseDialogFragment : DialogFragment(), OnCategoryClickListener {
    lateinit var binding : FragmentCustomExerciseDialogBinding
    lateinit var program : ProgramVO
    val viewModel : UserViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCustomExerciseDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.ibtnCEDBack.setOnClickListener { dismiss() }
        binding.ibtnCEDMore.setOnClickListener {  }

        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        val bundle = arguments
        if (bundle != null) {
            program = bundle.getParcelable("Program")!!
        } else {
            program = ProgramVO(0, mutableListOf(), "",  0,"","", mutableListOf() )
        }
        Log.v("program", "${program}")

        // ------! 요약 시작 !------
        // TODO 주차별 운동 변동해야함.
        binding.tvCEDTitle.text = "1주차 맞춤운동"

        binding.tvCEDTime.text = (if (program.programTime <= 60) {
            "${program.programTime}초"
        } else {
            "${program.programTime / 60}분 ${program.programTime % 60}초"
        }).toString()
        when (program.programStage) {
            "초급" -> binding.tvCEDStage.text = "초급자"
            "중급" -> binding.tvCEDStage.text = "중급자"
            "고급" -> binding.tvCEDStage.text = "상급자"
        }
        binding.tvCEDCount.text = "${program.programCount} 개"

        val userSn = userJson?.optString("user_sn").toString()
        val prefsManager = PreferencesManager(requireContext())
        val currentValue = prefsManager.getStoredInt(userSn)
        Log.v("prefs>CurrentValue", "user_sn: ${userSn}, currentValue: ${currentValue}")
        val programCount = if (program.programCount == "") 1 else program.programCount?.toInt()
        binding.tvCEDSet.text = "완료 $currentValue / 5 개"
        // ------! 요약 끝 !------

        // ------! 횡 rv 시작 !------
        // TODO 주차 넣어야 함
        val weeks = listOf("1주차", "2주차", "3주차", "4주차", "5주차", "6주차")
        val adapter = ExerciseCategoryRVAdapter(mutableListOf(), weeks, this@CustomExerciseDialogFragment, this@CustomExerciseDialogFragment, -1 ,"subCategory")
        val layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvCEDHorizontal.layoutManager = layoutManager
        binding.rvCEDHorizontal.adapter = adapter


        // ------! 횡 rv 끝 !------

        val adapter2 = ExerciseRVAdapter(this@CustomExerciseDialogFragment, program.exercises!!, mutableListOf(), "main")
        binding.rvCED.adapter = adapter2
        val layoutManager2 = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        binding.rvCED.layoutManager = layoutManager2

        binding.btnCEDStart.setOnClickListener {
            val urls = storePickUrl(program.exercises!!)
            val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
            intent.putStringArrayListExtra("urls", ArrayList(urls))
            intent.putExtra("total_time", program.programTime)
            requireContext().startActivity(intent)
            startActivityForResult(intent, 8080)
        }

        val balloon2 = Balloon.Builder(requireContext())
            .setWidthRatio(0.5f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setText("주차 요일에 맞추어 운동을 진행합니다.")
            .setTextColorResource(R.color.white)
            .setTextSize(15f)
            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            .setArrowSize(0)
            .setMargin(10)
            .setPadding(12)
            .setCornerRadius(8f)
            .setBackgroundColorResource(R.color.mainColor)
            .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
            .setLifecycleOwner(viewLifecycleOwner)
            .build()

        binding.ibtnCEDInfo.showAlignBottom(balloon2)
        balloon2.dismissWithDelay(3000L)
        binding.ibtnCEDInfo.setOnClickListener { it.showAlignBottom(balloon2) }


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

    override fun onCategoryClick(sn: Int, category: String) {

    }
}
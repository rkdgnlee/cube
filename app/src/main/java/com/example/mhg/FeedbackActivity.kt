package com.example.mhg

import android.animation.Animator
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mhg.databinding.ActivityFeedbackBinding
import com.litao.slider.NiftySlider
import org.json.JSONObject

class FeedbackActivity : AppCompatActivity() {
    lateinit var binding: ActivityFeedbackBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        binding.lavCongratulation.enableMergePathsForKitKatAndAbove(true)
//        binding.lavCongratulation.addAnimatorListener(object : Animator.AnimatorListener {
//            override fun onAnimationStart(animation: Animator) {}
//            override fun onAnimationEnd(animation: Animator) {
//                binding.lavCongratulation.visibility = View.GONE
//            }
//            override fun onAnimationCancel(animation: Animator) {}
//            override fun onAnimationRepeat(animation: Animator) {}
//        })

        setScore(binding.nsPain, binding.tvPainScore)
        setScore(binding.nsIntensity, binding.tvIntensityScore)
        setScore(binding.nsSatisfaction, binding.tvSatsfactionScore)



        binding.btnfeedback.setOnClickListener {
            val jsonObj = JSONObject()
            when (binding.rgFeedback.checkedRadioButtonId) {
                1-> jsonObj.put("exercise_place", "실내")
                2-> jsonObj.put("exercise_place", "야외")
                3-> jsonObj.put("exercise_place", "운동 시설")
                else -> jsonObj.put("exercise_place", "기타")
            }

//            jsonObj.put("exercise_intensity", binding.tvPainScore.value.toString())
//            jsonObj.put("exercise_fatigue", binding.tvIntensityScore.value.toString())
//            jsonObj.put("exercise_satisfic", binding.tvSatsfactionScore.value.toString())
            jsonObj.put("other_feedback", binding.tietFeedback.text)

//            Log.w(TAG+" 피드백", "장소: ${jsonObj.getString("exercise_place")}, 강도: ${jsonObj.getString("exercise_intense")}, 피로도: ${jsonObj.getString("exercise_fatigue")}, 만족도: ${jsonObj.getString("exercise_satisfic")}, 기타사항: ${jsonObj.getString("exercise_place")}")

            val intent = Intent(this@FeedbackActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
    private fun setScore(niftySlider: NiftySlider, textView: TextView) {
        when (textView.id) {
            R.id.tvPainScore -> {
                niftySlider.addOnValueChangeListener { _, value, _ ->
                    textView.text = when (value) {
                        1.0F -> "통증이 전혀 없다"
                        2.0F -> "통증이 거의 없다"
                        3.0F -> "약간의 통증이 있다"
                        4.0F -> "약간 심한 통증이 있다"
                        5.0F -> "통증이 상당히 심하다"
                        else -> { "error" }
                    }
                }
            }
            R.id.tvIntensityScore -> {
                niftySlider.addOnValueChangeListener{_ , value, _ ->
                    textView.text = when (value) {
                        1.0f -> "전혀 힘들지 않다"
                        2.0f -> "힘들지 않다"
                        3.0f -> "보통이다"
                        4.0f -> "힘들다"
                        5.0f -> "매우 힘들다"
                        else -> { "error" }
                    }
                }
            }
            R.id.tvSatsfactionScore -> {
                niftySlider.addOnValueChangeListener{_ , value, _ ->
                    textView.text = when (value) {
                        1.0f -> "전혀 만족스럽지 않다"
                        2.0f -> "만족스럽지 않다"
                        3.0f -> "보통이다"
                        4.0f -> "만족스럽다"
                        5.0f -> "매우 만족스럽다"
                        else -> { "error" }
                    }
                }
            }
        }

    }
}
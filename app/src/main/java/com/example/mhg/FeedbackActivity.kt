package com.example.mhg

import android.animation.Animator
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.mhg.databinding.ActivityFeedbackBinding
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


        binding.btnfeedback.setOnClickListener {
            val jsonObj = JSONObject()
            when (binding.rgFeedback.checkedRadioButtonId) {
                1-> jsonObj.put("exercise_place", "실내")
                2-> jsonObj.put("exercise_place", "야외")
                3-> jsonObj.put("exercise_place", "운동 시설")
                else -> jsonObj.put("exercise_place", "기타")
            }

            jsonObj.put("exercise_intense", binding.nsIntense.value.toString())
            jsonObj.put("exercise_fatigue", binding.nsFatigue.value.toString())
            jsonObj.put("exercise_satisfic", binding.nsSatisfic.value.toString())
            jsonObj.put("other_feedback", binding.tietFeedback.text)

            Log.w(TAG+" 피드백", "장소: ${jsonObj.getString("exercise_place")}, 강도: ${jsonObj.getString("exercise_intense")}, 피로도: ${jsonObj.getString("exercise_fatigue")}, 만족도: ${jsonObj.getString("exercise_satisfic")}, 기타사항: ${jsonObj.getString("exercise_place")}")

            val intent = Intent(this@FeedbackActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

    }
}
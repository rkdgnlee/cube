package com.tangoplus.tangoq

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tangoplus.tangoq.databinding.ActivityPlaySkeletonBinding

class PlaySkeletonActivity : AppCompatActivity() {
    lateinit var binding : ActivityPlaySkeletonBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaySkeletonBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}
package com.tangoplus.tangoq

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tangoplus.tangoq.databinding.ActivityPlaySkeletonBinding
import com.tangoplus.tangoq.databinding.ActivityPlayThumbnailBinding

class PlayThumbnailActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlayThumbnailBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayThumbnailBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}
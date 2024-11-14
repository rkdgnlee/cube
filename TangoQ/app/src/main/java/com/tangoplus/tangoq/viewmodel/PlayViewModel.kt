package com.tangoplus.tangoq.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.tangoq.PlaySkeletonActivity
import com.tangoplus.tangoq.PlaySkeletonActivity.horizontalPosition

class PlayViewModel : ViewModel() {
    val currentPlaybackPosition = MutableLiveData<Long>()
    val currentWindowIndex = MutableLiveData<Int>()

    // 재생목록 재생 후 피드백 담기
    var totalProgressDuration = 0
    var exerciseLog = Triple(0, 0, 0) // 진행시간, 갯수, 총 시간.
    var isDialogShown = MutableLiveData(false)

    //  PlaySkeleton에서 count하는 공간
    var normalNose  =  Pair(0.0, 0.0)
    val normalShoulderData = mutableListOf<Pair<Double, Double>>()
    val normalElbowData = mutableListOf<Pair<Double, Double>>()
    val normalWristData = mutableListOf<Pair<Double, Double>>()
    val normalHipData = mutableListOf<Pair<Double, Double>>()
    val normalKneeData = mutableListOf<Pair<Double, Double>>()
    val normalAnkleData = mutableListOf<Pair<Double, Double>>()
    var count = 0
    var lastCountTime = 0L
    val countDown3_5 = 3500L
    val countDown5= 5000L // 5초
    val countDown6 = 6000L
    val countDown8 = 8000L
    var isCountingEnabled = true
    var isDefaultPoseSet = false
    var currentHorizontalPosition = horizontalPosition.CENTER
    var currentVerticalPosition = PlaySkeletonActivity.verticalPosition.CENTER
    var hasMovedHorizontally = false
    var hasMovedVertically = false

    var currentLeftHorizontalPosition = horizontalPosition.CENTER
    var currentRightHorizontalPosition = horizontalPosition.CENTER
    var hasMovedLeftHorizontally = false
    var hasMovedRightHorizontally = false

}
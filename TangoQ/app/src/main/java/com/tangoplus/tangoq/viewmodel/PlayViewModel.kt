package com.tangoplus.tangoq.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlayViewModel : ViewModel() {
    val currentPlaybackPosition = MutableLiveData<Long>()
    val currentWindowIndex = MutableLiveData<Int>()

    // 재생목록 재생 후 피드백 담기
    var totalProgressDuration = 0
    var exerciseLog = Triple(0, 0, 0) // 진행시간, 갯수, 총 시간.
    var isDialogShown = MutableLiveData(false)
    var isUnit = false
    // TODO PlaySkeleton 되살리기 + viewModel 값들 다 가져오기


}
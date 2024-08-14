package com.tangoplus.tangoq.data

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ExerciseViewModel : ViewModel() {
    // 재생목록 재생 후 피드백 담기
    var exerciseLog = MutableLiveData(Triple(0, "", 0)) // 진행시간, 갯수, 총 진행시간
    var isDialogShown = MutableLiveData(false)

    init {
        exerciseLog.value = Triple(0, "", 0)
    }
}
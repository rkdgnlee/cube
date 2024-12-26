package com.tangoplus.tangoq.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.tangoq.vo.ExerciseVO

class ExerciseViewModel : ViewModel() {

    // 모든 운동 exerciseFragment에서 받아서 VM에 넣는 곳
    var allExercises : MutableList<ExerciseVO>
    // 운동 검색 기록 임시로 담아두는 곳.
    var searchHistory = MutableLiveData(mutableListOf<Pair<Int, String>>()) // 운동 검색 history

    init {
        allExercises = mutableListOf()
    }
}
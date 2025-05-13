package com.tangoplus.tangoq.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.tangoplus.tangoq.vo.ExerciseHistoryVO
import com.tangoplus.tangoq.vo.ExerciseVO
import com.tangoplus.tangoq.vo.ProgramVO
import com.tangoplus.tangoq.vo.ProgressUnitVO

class ExerciseViewModel : ViewModel() {

    // 모든 운동 exerciseFragment에서 받아서 VM에 넣는 곳
    var allExercises : MutableList<ExerciseVO>?
    var allExerciseHistorys: MutableList<ExerciseHistoryVO>?
    var categoryId : ArrayList<Int>? = null
    var sn : Int? = null
    var latestProgram : ProgramVO? = null
    var latestUVP : MutableList<ProgressUnitVO>? = null
    // 운동 검색 기록 임시로 담아두는 곳.
    var searchHistory = MutableLiveData(mutableListOf<Pair<Int, String>>()) // 운동 검색 history



    init {
        allExercises = mutableListOf()
        allExerciseHistorys = mutableListOf()
    }
}
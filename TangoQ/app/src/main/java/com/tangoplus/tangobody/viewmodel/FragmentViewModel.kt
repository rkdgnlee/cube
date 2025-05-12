package com.tangoplus.tangobody.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class FragmentViewModel: ViewModel() {
    private val _currentFragmentType = MutableLiveData<FragmentType>()
    val currentFragmentType: LiveData<FragmentType> = _currentFragmentType

    // Fragment 타입 enum
    enum class FragmentType {
        MAIN_FRAGMENT,
        MAIN_ANALYSIS_FRAGMENT,
        ANALYZE_FRAGMENT,
        EXERCISE_FRAGMENT,
        EXERCISE_DETAIL_FRAGMENT,
        MEASURE_FRAGMENT,
        MEASURE_DETAIL_FRAGMENT,
        MEASURE_HISTORY_FRAGMENT,
        PROFILE_FRAGMENT
        // 필요한 다른 Fragment 타입 추가
    }

    // 현재 Fragment 설정
    fun setCurrentFragment(type: FragmentType) {
        _currentFragmentType.value = type
    }

    // 초기화 (필요시)
    init {
        _currentFragmentType.value = FragmentType.MAIN_FRAGMENT // 기본값
    }
}
package com.tangoplus.tangoq.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class FavoriteViewModel: ViewModel() {
    // 즐겨찾기
    var favoriteItem = MutableLiveData<FavoriteVO>()  // 운동 목록과 즐겨찾기 제목 등이 저장된 객체 1개
    val favoriteEditItem = MutableLiveData(JSONObject()) // edit에서 임시로 담아놓을 viewmodel
    val favoriteList =  MutableLiveData<MutableList<FavoriteVO>>()   //  pickItem이 여러개 들어간 즐겨찾기 목록(데이터 전부 포함)
    val exerciseBasketUnits = MutableLiveData(mutableListOf<ExerciseVO>()) // 장바구니에 담기는 임시 운동 목록(tab 전환 시 값 보존 목적)
    val exerciseUnits = MutableLiveData(mutableListOf<ExerciseVO>()) // 편집창에 담기는 운동 목록h
    val allExercises = MutableLiveData<List<ExerciseVO>>() // 모든 운동 목록

    // 재생목록 재생 후 피드백 담기
    var exerciseLog = MutableLiveData(Triple(0, "", 0)) // 진행시간, 갯수, 총 진행시간
    var isDialogShown = MutableLiveData(false)

    // 메인 - 프로그램
    val programList = MutableLiveData(mutableListOf<ProgramVO>())

    // 프로그램의 운동을 즐겨 찾기 추가
    var selectedFavorite = MutableLiveData<FavoriteVO>()


    init {
        exerciseUnits.value = mutableListOf()
        favoriteItem.value = FavoriteVO(mutableListOf(),0,"","", "", "",  mutableListOf())
        favoriteEditItem.value = JSONObject()
        favoriteList.value = mutableListOf()
        exerciseBasketUnits.value = mutableListOf()
        allExercises.value = listOf()
        programList.value = mutableListOf()
        exerciseLog.value = Triple(0, "", 0)
    }

    // 확인버튼 눌렀을 때 전체 다 담기
    fun addExercises(exercises: List<ExerciseVO>) {
        for (exercise in exercises) {
            repeat(exercise.quantity) {
                exerciseUnits.value?.add(exercise)
            }
        }
    }
    fun getExerciseBasketUnit(): MutableList<ExerciseVO> {
        return exerciseBasketUnits.value?.filter { it.quantity >= 1 }?.toMutableList() ?: mutableListOf()
    }
    fun addExerciseBasketUnit(exercise : ExerciseVO, quantity: Int) {
        val existingExercise = exerciseBasketUnits.value?.find { it.exerciseId.toString() == exercise.exerciseId.toString() }
        if (existingExercise == null) {
            exercise.quantity = quantity
            exerciseBasketUnits.value?.add(exercise)
        }
    }

    // +- 수량 체크
    fun setQuantity(itemId: String, quantity: Int) {
        exerciseBasketUnits.value?.find { it.exerciseId.toString() == itemId }?.quantity = quantity
    }

    fun getQuantityForItem(itemId: String): Int {
        val item = exerciseBasketUnits.value?.find { it.exerciseId.toString() == itemId }
        return item?.quantity?: 0
    }
//    fun imgThumbnailAdd(imgUrl: String) {
//        val newList = mutableListOf<String>()
//        newList.add(imgUrl)
//        favoriteItem.value.imgThumbnailList = newList
//    }
}
package com.example.mhg.VO

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LOGGER
import org.json.JSONArray
import org.json.JSONObject

class ExerciseViewModel: ViewModel() {
    // 즐겨찾기
    var favoriteItem = MutableLiveData<PickItemVO>()  // 운동 목록과 즐겨찾기 제목 등이 저장된 객체 1개
    val favoriteEditItem = MutableLiveData(JSONObject()) // edit에서 임시로 담아놓을 viewmodel
    val favoriteList = MutableLiveData(mutableListOf<Pair<Int, String>>())   // pickItems에서 제목만 가져온 string array
    val favoriteItems = MutableLiveData(mutableListOf<PickItemVO>()) // pickItem이 여러개 들어간 즐겨찾기 목록(데이터 전부 포함)

    val exerciseBasketUnits = MutableLiveData(mutableListOf<ExerciseVO>()) // 장바구니에 담기는 임시 운동 목록(tab 전환 시 값 보존 목적)
    val exerciseUnits = MutableLiveData(mutableListOf<ExerciseVO>()) // 편집창에 담기는 운동 목록
    val allExercises = MutableLiveData<List<ExerciseVO>>() // 모든 운동 목록
    init {
        exerciseUnits.value = mutableListOf()
        favoriteItem.value = PickItemVO(0, "", "", "", "", mutableListOf())
        favoriteEditItem.value = JSONObject()
        favoriteList.value = mutableListOf()

        favoriteItems.value = mutableListOf()
        exerciseBasketUnits.value = mutableListOf()
        allExercises.value = listOf()
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
        val existingExercise = exerciseBasketUnits.value?.find { it.exerciseDescriptionId.toString() == exercise.exerciseDescriptionId.toString() }
        if (existingExercise == null) {
            exercise.quantity = quantity
            exerciseBasketUnits.value?.add(exercise)
        }
    }

    // +- 수량 체크
    fun setQuantity(itemId: String, quantity: Int) {
        exerciseBasketUnits.value?.find { it.exerciseDescriptionId.toString() == itemId }?.quantity = quantity
    }

    fun getQuantityForItem(itemId: String): Int {
        val item = exerciseBasketUnits.value?.find { it.exerciseDescriptionId.toString() == itemId }
        return item?.quantity?: 0
    }

//    fun addPick(favoriteName: String, favoriteSn: Int) {
//        val pickObject = JSONObject().apply {
//            put("favorite_name", favoriteName)
//            put("favorite_sn", favoriteSn)
//        }
//        val currentList = pickList.value ?: mutableListOf()
//
//        pickList.value= currentList
//    }

}
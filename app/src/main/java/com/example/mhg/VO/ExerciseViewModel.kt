package com.example.mhg.VO

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.LOGGER
import org.json.JSONArray
import org.json.JSONObject

class ExerciseViewModel: ViewModel() {
    // 즐겨찾기
    val pickItem = MutableLiveData(JSONObject())  // 운동 목록과 즐겨찾기 제목 등이 저장된 객체 1개
    val pickEditItem = MutableLiveData(JSONObject()) // edit에서 임시로 담아놓을 viewmodel
    val pickList = MutableLiveData(JSONArray())   // pickItems에서 제목만 가져온 string array
    val pickItems = MutableLiveData(mutableListOf<PickItemVO>()) // pickItem이 여러개 들어간 즐겨찾기 목록(데이터 전부 포함)

    val exerciseBasketUnits = MutableLiveData(mutableListOf<ExerciseVO>()) // 장바구니에 담기는 임시 운동 목록(tab 전환 시 값 보존 목적)
    val exerciseUnits = MutableLiveData(mutableListOf<ExerciseVO>()) // 편집창에 담기는 운동 목록
    val allExercises = MutableLiveData<List<ExerciseVO>>() // 모든 운동 목록
    init {
        exerciseUnits.value = mutableListOf()
        pickItem.value = JSONObject()
        pickEditItem.value = JSONObject()
        pickList.value = JSONArray()
        pickItems.value = mutableListOf()
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

    fun addPick(favoriteName: String, favoriteSn: String) {
        val pickObject = JSONObject().apply {
            put("favorite_name", favoriteName)
            put("favorite_sn", favoriteSn)
        }
        val currentList = pickList.value ?: JSONArray()
        currentList.put(pickObject)
        pickList.value= currentList
    }

}
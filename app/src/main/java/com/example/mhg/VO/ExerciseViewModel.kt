package com.example.mhg.VO

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject

class ExerciseViewModel: ViewModel() {
    // 즐겨찾기
    val exerciseUnits = MutableLiveData(mutableListOf<ExerciseVO>())
    val pickItem = MutableLiveData(JSONObject())
    val pickList = MutableLiveData(JSONArray())
    val pickItems = MutableLiveData(mutableListOf<PickItemVO>())

    val itemQuantities = MutableLiveData<Map<String, Int>>(emptyMap())
    init {
        exerciseUnits.value = mutableListOf()
        pickItem.value = JSONObject()
        pickList.value = JSONArray()
        pickItems.value = mutableListOf()

    }

    // 확인버튼 눌렀을 때 전체 다 담기
    fun addExercise(exercises: List<ExerciseVO>) {
        for (exercise in exercises) {
            repeat(exercise.quantity) {
                exerciseUnits.value?.add(exercise)
            }
        }
    }

    // +- 수량 체크
    fun setQuantity(itemId: String, quantity: Int) {
//        itemQuantities.value = itemQuantities.value?.toMutableMap()?.apply {
//            put(itemId, quantity)
//        }
//        exerciseUnits.value.
    }

    fun getQuantityForItem(itemId: String): Int {
        return itemQuantities.value?.get(itemId)?: 0
    }

    fun addPick(pickName: String, pickId: String) {
        val pickObject = JSONObject().apply {
            put("pickName", pickName)
            put("pickId", pickId)
        }
        val currentList = pickList.value ?: JSONArray()
        currentList.put(pickObject)
        pickList.value= currentList
    }
    // 운동과 여러가지
//    fun addExerciseBasket(exerciseItem: ExerciseBasketVO) {
//        val ExerciseItem = JSONObject()
//        ExerciseItem.put("basket_name", "${exerciseItem.basketName}")
//        ExerciseItem.put("basket_explain_title", "${exerciseItem.basketExplainTitle}")
//        ExerciseItem.put("basket_explain", "${exerciseItem.basketExplain}")
//        ExerciseItem.put("basket_disclosure", "${exerciseItem.basketDisclosure}")
//        ExerciseItem.put("basket_exercises", "${exerciseItem.exercises}")
//
//    }


//    fun createUserBasket(): UserBasketItem {
//        return UserBasketItem()
//    }
//    // -----!작성된 즐겨찾기 여러개를 관리하는 변수 !-----
//    val userBasketList = MutableLiveData<ArrayList<UserBasketItem>>()
//    init {
//        userBasketList.value = ArrayList()
//    }
//    fun addBasket(basket : UserBasketItem) {
//        userBasketList.value?.add(basket)
//        userBasketList.value = userBasketList.value
//    }
//    fun deleteBasket(basket: UserBasketItem) {
//        userBasketList.value?.remove(basket)
//        userBasketList.value = userBasketList.value
//    }
}
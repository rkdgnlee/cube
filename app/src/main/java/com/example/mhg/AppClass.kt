package com.example.mhg

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.example.mhg.VO.ExerciseVO
import com.example.mhg.VO.PickItemVO
import org.json.JSONArray
import org.json.JSONObject

class AppClass : Application() {
    lateinit var pickItem: PickItemVO
    lateinit var pickList: MutableLiveData<MutableList<String>>
    lateinit var pickItems: MutableLiveData<MutableList<PickItemVO>>

//    override fun onCreate() {
//        super.onCreate()
//
//        // 데이터 초기화
//        pickItem = PickItemVO("","", "", "", "", mutableListOf())
//        pickList = MutableLiveData(mutableListOf())
//        pickItems = MutableLiveData(mutableListOf())
//    }
}
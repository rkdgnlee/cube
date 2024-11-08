package com.tangoplus.tangoq.data

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONObject

class UserViewModel: ViewModel() {
    val User = MutableLiveData(JSONObject())

    var setupProgress = 34
    var setupStep = 0
    var step1 = MutableLiveData<Boolean?>(null)

    var step21 = MutableLiveData<Boolean?>(null)
    var step22 = MutableLiveData<Boolean?>(null)
    val step2 = MediatorLiveData<Boolean?>().apply {
        addSource(step21) { _ -> updateStep2() }
        addSource(step22) { _ -> updateStep2() }
    }

    var step31 = MutableLiveData<Boolean?>(null)
    var step32 = MutableLiveData<Boolean?>(null)
    var step3 = MediatorLiveData<Boolean?>().apply {
        addSource(step31) { updateStep3() }
        addSource(step32) { updateStep3() }
    }

    // ------# connected 날짜 담기 #------

    val connectedCenters = mutableListOf<Pair<String, String>>()

    var processingProgram : ProgramVO? = null

    init {
        User.value = JSONObject()
        setupProgress = 34
        setupStep = 0
        processingProgram = null
    }

    private fun updateStep2() {
        step2.value = when {
            step21.value == null && step22.value == null -> null
            step21.value == true && step22.value == true -> true
            else -> false
        }
    }

    private fun updateStep3() {
        step3.value = when {
            step31.value == null && step32.value == null -> null
            step31.value == true && step32.value == true -> true
            else -> false
        }
    }
}
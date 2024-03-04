package com.example.mhg.VO

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UserViewModel: ViewModel() {
    val User = MutableLiveData<UserVO>()
}
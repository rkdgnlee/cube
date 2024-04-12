package com.example.mhg.VO

import android.bluetooth.BluetoothGatt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BtGattViewModel : ViewModel() {
    var gatt: MutableLiveData<BluetoothGatt?> = MutableLiveData()
}
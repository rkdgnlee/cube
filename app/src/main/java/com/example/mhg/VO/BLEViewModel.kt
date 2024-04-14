package com.example.mhg.VO

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BLEViewModel : ViewModel() {
    var devices = MutableLiveData(mutableSetOf<BluetoothDevice>())
    var gatt: MutableLiveData<BluetoothGatt?> = MutableLiveData()



    fun addDevice(device : BluetoothDevice) {
        val updatedDevices = devices.value ?: mutableSetOf()
        updatedDevices.add(device)
        devices.value = updatedDevices
    }
}
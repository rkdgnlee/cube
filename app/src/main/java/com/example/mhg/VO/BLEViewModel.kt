package com.example.mhg.VO

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.ContentValues
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.UUID

class BLEViewModel : ViewModel() {
    var devices = MutableLiveData(mutableSetOf<BluetoothDevice>())
    var gatt: MutableLiveData<BluetoothGatt?> = MutableLiveData()
    var gattServices: MutableLiveData<List<BluetoothGattService>> = MutableLiveData()
    var gattCharacteristics: MutableLiveData<List<BluetoothGattCharacteristic>> = MutableLiveData()

    // read한 것들 전부 넣기.
    var characteristicValues :  MutableLiveData<Map< UUID, MutableList<ByteArray>>> = MutableLiveData()

    fun addDevice(device : BluetoothDevice) {
        val updatedDevices = devices.value ?: mutableSetOf()
        updatedDevices.add(device)
        devices.value = updatedDevices
    }
}
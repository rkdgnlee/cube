package com.example.mhg.VO

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mhg.`object`.GattManager
import java.util.UUID

class BLEViewModel : ViewModel() {
    var devices = MutableLiveData(mutableSetOf<BluetoothDevice>())
    var gattServiceData: MutableLiveData<List<HashMap<String, String>>> = MutableLiveData()
    var gattCharacteristicData: MutableLiveData<List<ArrayList<HashMap<String, String>>>> = MutableLiveData()

    // gatt 객체 저장
    var gattManager : GattManager? = null
    // read한 것들 전부 넣기.
    var characteristicValues :  MutableLiveData<List<HashMap<UUID,ByteArray>>> = MutableLiveData()

    fun addDevice(device : BluetoothDevice) {
        val updatedDevices = devices.value ?: mutableSetOf()
        updatedDevices.add(device)
        devices.value = updatedDevices
    }
    fun updateGattServiceData(serviceData: List<HashMap<String, String>>) {
        gattServiceData.value = serviceData
    }

    fun updateGattCharacteristicData(characteristicData: List<ArrayList<HashMap<String, String>>>) {
        gattCharacteristicData.value = characteristicData
    }
}
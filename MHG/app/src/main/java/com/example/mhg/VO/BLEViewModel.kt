package com.example.mhg.VO

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mhg.ReportGoalFragment

class BLEViewModel : ViewModel() {
//    var init = false
//    var mBtAdapter : BluetoothAdapter? = null
//    var mDevice : BluetoothDevice? = null
//    var mDeviceList : MutableLiveData<ArrayList<BluetoothDevice>> = MutableLiveData()
//    var mDeviceInfoList : ArrayList<ReportGoalFragment.BluetoothDeviceInfo> = arrayListOf()
//    var txValue = byteArrayOf() ?: null
//    var devices = MutableLiveData(mutableSetOf<BluetoothDevice>())
//    var selectedDevice : MutableLiveData<BluetoothDevice?> = MutableLiveData()
//    val gattServiceData: MutableList<HashMap<String, String>> = mutableListOf() // 서비스 목록
//    var gattCharacteristicData: MutableLiveData<MutableList<ArrayList<HashMap<String, String>>>> = MutableLiveData() // 특성 목록
//    var mGattCharacteristics = mutableListOf<MutableList<BluetoothGattCharacteristic>>()
//
//    var byteArrayData = MutableLiveData(mutableListOf<String>())
//    // gatt 객체 저장
//    var gattManager : GattManager? = null
//    // read한 것들 전부 넣기.
//
//
//    fun addDevice(device : BluetoothDevice) {
//        val updatedDevices = devices.value ?: mutableSetOf()
//        updatedDevices.add(device)
//        devices.value = updatedDevices
//    }
//
//    fun addByteArrayData(byteArray: String) {
//        val updateByteArray = byteArrayData.value ?: mutableListOf()
//        updateByteArray.add(byteArray)
//        byteArrayData.value = updateByteArray
//    }
//
//    fun reset() {
//        devices.value = mutableSetOf()
//        selectedDevice.value = null
//        gattServiceData.clear()
//        gattCharacteristicData.value = mutableListOf()
//        mGattCharacteristics.clear()
//        byteArrayData.value = mutableListOf()
//        gattManager = null
//    }
}
package com.example.mhg.`object`

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.ContentValues
import android.util.Log
import java.util.UUID

class GattManager(private val gatt: BluetoothGatt) {
//    @SuppressLint("MissingPermission")
//    fun readCharacteristic(serviceUUID: String, characteristicUUID: String) {
//        val service = gatt.getService(UUID.fromString(serviceUUID))
//        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUUID))
//        Log.w("gatt Charac", "$characteristic")
//        if (characteristic != null) {
//            gatt.readCharacteristic(characteristic)
//            Log.w("gatt readCharac", "${gatt.readCharacteristic(characteristic)}")
//        }
//    }
    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        gatt.readCharacteristic(characteristic)
    }
}

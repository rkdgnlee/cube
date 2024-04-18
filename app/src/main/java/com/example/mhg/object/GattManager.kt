package com.example.mhg.`object`

import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.SharedPreferences

class GattManager(private val gatt: BluetoothGatt) {
    val bluetoothGatt : BluetoothGatt = gatt
}
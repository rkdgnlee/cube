package com.example.mhg

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import androidx.annotation.RequiresPermission

class GattCallback: BluetoothGattCallback() {
    // 기기 연결 상태가 변경될 때 호출 newState는 새로운 연결 상태.
    private var services: List<BluetoothGattService> = emptyList()
    // 기기 연결 후 기기에서 가져올 수 있는 서비스 찾았을 때 호출
    @SuppressLint("MissingPermission")
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        super.onConnectionStateChange(gatt, status, newState)
        if (status != BluetoothGatt.GATT_SUCCESS) {
            // TODO: handle error
            return
        }
        if (newState == BluetoothGatt.STATE_CONNECTED) { // 장치가 연결 됨.
            // TODO: handle the fact that we've just connected
            gatt?.discoverServices()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) { // readCharacteristic() 메서드를 호출한 후, 특성 읽기 작업이 완료되면 호출
        super.onServicesDiscovered(gatt, status)
        services = gatt?.services!!
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, value, status)
//            if (characteristic.uuid == myCharacteristicUUID) {
//                Log.w("bluetooth", String(characteristic.value))
//                readCharacterisitic(myCharacteristicUUID ,characteristic.uuid)
//            }
    }
}
package com.example.mhg.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Base64
import android.util.Log
import java.lang.IllegalArgumentException
import java.util.UUID

class BluetoothLeService : Service() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    inner class LocalBinder : Binder() {
        fun getService() : BluetoothLeService {
            return this@BluetoothLeService
        }
    }
    private val binder = LocalBinder()

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun initialize(): Boolean {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }

    private var bluetoothGatt : BluetoothGatt? = null
    @SuppressLint("MissingPermission")
    fun connect(address: String): Boolean {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)

                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                Log.w("어댑터 초기화 O" , "bluetoothGatt Success")
                return true
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.")
                return false
            }
            // connect to the GATT server on the device
        } ?: run {
            Log.w("어댑터 초기화 X", "BluetoothAdapter not initialized")
            return false
        }
    }
    private var connectionState = BluetoothAdapter.STATE_DISCONNECTED
    private var services: List<BluetoothGattService> = emptyList()
    val servicesToCharacteristics = mutableMapOf<UUID, MutableList<String>>()
    // -----! gatt callback 함수 시작 !------
    private val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                connectionState = STATE_CONNECTED
                broadcastUpdate(ACTION_GATT_CONNECTED)
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // disconnected from the GATT Server
                connectionState = STATE_DISCONNECTED
                broadcastUpdate(ACTION_GATT_DISCONNECTED)
            }
        }
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) { // readCharacteristic() 메서드를 호출한 후, 특성 읽기 작업이 완료되면 호출
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                services = gatt?.services!!
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
                Log.w("GATT서비스목록", services.toString())
                services.forEach { service ->
                    service.characteristics.forEach{ characteristic ->
                        readCharacteristic(characteristic)
                    }
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic.let { charac ->
                    val data = charac.value
                    if (data != null) {
                        val encodedData = Base64.encodeToString(data, Base64.DEFAULT)
                        servicesToCharacteristics.getOrPut(charac.service.uuid) { mutableListOf() }.add(encodedData)
                        Log.w("byteArray값", "$data")
                    }
                }
                val intent = Intent(ACTION_DATA_AVAILABLE)
                intent.putExtra("servicesToCharacteristics", servicesToCharacteristics.toString())
                sendBroadcast(intent)
            }
        }
    } // -----! gatt callback 함수 끝 !------
    fun getSupportedGattServices(): List<BluetoothGattService>? {
        return bluetoothGatt?.services
    }



    // TODO GATT 서비스는 기기에서 읽을 수 있는 특성 목록을 제공합니다. 데이터를 쿼리하려면 BluetoothGatt에서 readCharacteristic() 함수를 호출하여 읽으려는 BluetoothGattCharacteristic를 전달합니다.
    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.let { gatt ->
            gatt.readCharacteristic(characteristic)
        } ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
            return
        }
    }
    companion object {
        const val ACTION_GATT_CONNECTED =
            "com.example.mhg.service.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.mhg.service.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.mhg.service.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE = "com.example.mhg.service.ACTION_DATA_AVAILABLE"
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2

    }
    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }

}
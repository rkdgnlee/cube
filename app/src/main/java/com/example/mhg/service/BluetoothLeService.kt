package com.example.mhg.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.STATE_CONNECTED
import android.bluetooth.BluetoothAdapter.STATE_DISCONNECTED
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.ContentValues.TAG
import android.content.Intent
import android.nfc.NfcAdapter.EXTRA_DATA
import android.os.Binder
import android.os.IBinder
import android.util.Base64
import android.util.Log
import com.example.mhg.SampleGattAttributes
import java.lang.IllegalArgumentException
import java.util.Arrays
import java.util.LinkedList
import java.util.Queue
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
    fun connect(address: String): BluetoothGatt? {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                Log.w("어댑터 초기화 O" , "bluetoothGatt Success")
                return bluetoothGatt
            } catch (exception: IllegalArgumentException) {
                Log.w(TAG, "Device not found with provided address.")
                return null
            }
            // connect to the GATT server on the device
        } ?: run {
            Log.w("어댑터 초기화 X", "BluetoothAdapter not initialized")
            return null
        }
    }
    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
    private var connectionState = BluetoothAdapter.STATE_DISCONNECTED
    private var services: List<BluetoothGattService> = emptyList()
    val servicesToCharacteristics = mutableMapOf<UUID, MutableList<String>>()
    private val readQueue: Queue<BluetoothGattCharacteristic> = LinkedList()

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
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                services = gatt?.services!!
                Log.w("GATT서비스목록", services.toString())
                services.forEach { service ->
                    service.characteristics.forEach{ characteristic ->
                        Log.w("readCharac", "${characteristic}")
                        Log.w("Properties", "${characteristic.properties}")
                    }
                }

                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if(status == BluetoothGatt.GATT_SUCCESS){
                Log.d(TAG,"GATT Callback --> onCharacteristicWrite OK")
                if (characteristic != null) {
                    Log.w("read after write", "${readCharacteristic(characteristic)}")
                }
            }
        }
//
//        override fun onCharacteristicChanged(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            value: ByteArray
//        ) {
//            super.onCharacteristicChanged(gatt, characteristic, value)
//            Log.w("readCallback실행", "$characteristic")
//        }
        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Log.w("readCallback실행", "$characteristic")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                characteristic.let { charac ->
                    val data = charac?.value
                    if (data != null) {
                        val encodedData = Base64.encodeToString(data, Base64.DEFAULT)
                        servicesToCharacteristics.getOrPut(charac.service.uuid) { mutableListOf() }.add(encodedData)
                        Log.w("byteArray값", "data: ${Arrays.toString(data)}")

                    }
                }

            }
        }
//        @SuppressLint("MissingPermission")
//        override fun onCharacteristicRead(
//            gatt: BluetoothGatt,
//            characteristic: BluetoothGattCharacteristic,
//            value: ByteArray,
//            status: Int
//        ) {
//            super.onCharacteristicRead(gatt, characteristic, value, status)
//            Log.w("readCallback실행", "$characteristic")
//
//            readQueue.remove()
//            if (readQueue.size > 0) {
//                gatt.readCharacteristic(readQueue.peek())
//            }
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                characteristic.let { charac ->
//                    val data = charac.value
//                    if (data != null) {
//                        val encodedData = Base64.encodeToString(data, Base64.DEFAULT)
//                        servicesToCharacteristics.getOrPut(charac.service.uuid) { mutableListOf() }.add(encodedData)
//                        Log.w("byteArray값", "$data")
//
//                    }
//                }
//
//            }
//        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    } // -----! gatt callback 함수 끝 !------

    //-----------! read, write 시작 !-------------
    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.readCharacteristic(characteristic) ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")
            return
        }
    }
    @SuppressLint("MissingPermission")
    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data:ByteArray) {
        characteristic.value = data
        bluetoothGatt?.writeCharacteristic(characteristic)
        Log.w("writeSuccess", "writeSuccess")
    }
    //-----------! read, write 끝 !-----------
    fun getSupportedGattServices(): List<BluetoothGattService>? {
        return bluetoothGatt?.services
    }

    fun getGatt(): BluetoothGatt? {
        return bluetoothGatt
    }
    @SuppressLint("MissingPermission")
    fun setCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) {
        bluetoothGatt?.let { gatt ->
            if (EXAMPLE_CHARACTERISTIC_UUID == characteristic.uuid.toString()) {
                val descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) // 일반 적인 config
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt.writeDescriptor(descriptor)
            }
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
        const val EXAMPLE_CHARACTERISTIC_UUID = "fe2c1236-8366-4814-8eb0-01de32100bea"
        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2

    }
    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        sendBroadcast(intent)
    }
    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        // This is special handling for the Heart Rate Measurement profile. Data
        // parsing is carried out as per profile specifications.
        when (characteristic.uuid) {
            UUID.fromString(EXAMPLE_CHARACTERISTIC_UUID) -> {
                val flag = characteristic.properties
                val format = when (flag and 0x01) {
                    0x01 -> {
                        Log.d(TAG, "Heart rate format UINT16.")
                        BluetoothGattCharacteristic.FORMAT_UINT16
                    }
                    else -> {
                        Log.d(TAG, "Heart rate format UINT8.")
                        BluetoothGattCharacteristic.FORMAT_UINT8
                    }
                }
                val heartRate = characteristic.getIntValue(format, 1)
                Log.d(TAG, String.format("Received heart rate: %d", heartRate))
                intent.putExtra(EXTRA_DATA, (heartRate).toString())
            }
            else -> {
                // For all other profiles, writes the data formatted in HEX.
                val data: ByteArray? = characteristic.value
                if (data?.isNotEmpty() == true) {
                    val hexString: String = data.joinToString(separator = " ") {
                        String.format("%02X", it)
                    }
                    intent.putExtra(EXTRA_DATA, "$data\n$hexString")
                }
            }
        }
        sendBroadcast(intent)
    }
}
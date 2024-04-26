package com.tangoplus.tangoq.Service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothProfile
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import java.util.UUID

class BluetoothLeService : Service() {
    private var bluetoothAdapter: BluetoothAdapter? = null
    val EXTRA_DATA = "com.example.mhg.service.EXTRA_DATA"
    private var mBluetoothGatt: BluetoothGatt? = null
    inner class LocalBinder : Binder() {
        fun getService() : BluetoothLeService {
            return this@BluetoothLeService
        }
    }
    private val binder: IBinder = LocalBinder()

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

    @SuppressLint("MissingPermission")
    fun connect(address: String): BluetoothGatt? {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                mBluetoothGatt = device.connectGatt(this, true, bluetoothGattCallback)
                Log.w("어댑터 초기화 O" , "bluetoothGatt Success")
                return mBluetoothGatt
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
        mBluetoothGatt?.disconnect()

    }
    @SuppressLint("MissingPermission")
    fun deleteDevice() {
        mBluetoothGatt?.close()
        mBluetoothGatt = null
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
                mBluetoothGatt?.discoverServices()
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
                Log.w("service", "serviceFinished")
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED)
            }
        }

//        @SuppressLint("MissingPermission")
//        override fun onCharacteristicWrite(
//            gatt: BluetoothGatt?,
//            characteristic: BluetoothGattCharacteristic?,
//            status: Int
//        ) {
//            super.onCharacteristicWrite(gatt, characteristic, status)
//            Log.d("writeCallback실행","GATT Callback --> onCharacteristicWrite OK")
//        }


//        @Deprecated("Deprecated in Java")
//        override fun onCharacteristicRead(
//            gatt: BluetoothGatt?,
//            characteristic: BluetoothGattCharacteristic?,
//            status: Int
//        ) {
//            super.onCharacteristicRead(gatt, characteristic, status)
//            Log.w("readCallback실행", "$characteristic")
//        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            Log.d("CharacRead", "onCharacteristicRead")
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }

        @SuppressLint("MissingPermission")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            Log.w("CharacChanged","onCharacteristicChanged")
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }

    }
    // -----! gatt callback 함수 끝 !------

    //-----------! read, write 시작 !-------------
//    @SuppressLint("MissingPermission")
//    fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
//        getGatt()?.readCharacteristic(characteristic) ?: run {
//            Log.w(TAG, "BluetoothGatt not initialized")
//            Return
//        }
//    }
    @SuppressLint("MissingPermission")
    fun readCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        mBluetoothGatt?.readCharacteristic(characteristic) ?: run {
            Log.w(TAG, "BluetoothGatt not initialized")

        }
    }
//    @SuppressLint("MissingPermission")
//    fun writeCharacteristic(characteristic: BluetoothGattCharacteristic, data:ByteArray) {
//        characteristic.value = data
//        bluetoothGatt?.writeCharacteristic(characteristic)
//        Log.w("write", "writefunStart")
//    }

    //-----------! read, write 끝 !-----------
    fun getSupportedGattServices(): List<BluetoothGattService>? {
        return mBluetoothGatt?.services
    }

    fun getGatt(): BluetoothGatt? {
        return mBluetoothGatt
    }
    //    @SuppressLint("MissingPermission")
//    fun setCharacteristicNotification(
//        characteristic: BluetoothGattCharacteristic,
//        enabled: Boolean
//    ) {
//        bluetoothGatt?.let { gatt ->
//            if (CHARACTERISTIC_UUID == characteristic.uuid.toString()) {
//                val descriptor = characteristic.getDescriptor(UUID.fromString(BLEGattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) // 일반 적인 config
//                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
//                gatt.writeDescriptor(descriptor)
//                Log.w("descriptor", "${descriptor.value}")
//
//                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY > 0) {
//                    Log.w("알림notify", "true")
//                }
//                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE > 0) {
//                    Log.w("알림I", "true")
//                }
//            }
//        }
//    }
    private fun showMessage(msg: String) {
        Log.e("enableTxNotification", msg)
    }
    @SuppressLint("MissingPermission")
    fun enableTxNotification() {
        Log.d("BluetoothLeService", "enableTxNotification() - 1")
        if (mBluetoothGatt == null) {
            showMessage("mBluetoothGatt null$mBluetoothGatt")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        Log.d("BluetoothLeService", "enableTxNotification() - 2")
        val RxService: BluetoothGattService =
            mBluetoothGatt!!.getService(RX_SERVICE_UUID)
        if (RxService == null) {
            showMessage("Rx service not found!")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        Log.d("BluetoothLeService", "enableTxNotification() - 3")
        val TxChar = RxService.getCharacteristic(NOTIFY_CHAR_UUID)
        if (TxChar == null) {
            showMessage("Tx characteristic not found!")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }


        mBluetoothGatt!!.setCharacteristicNotification(TxChar, true)
        Log.d("BluetoothLeService", "enableTxNotification() - 4")
        val descriptor = TxChar.getDescriptor(CCCD)
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        mBluetoothGatt!!.writeDescriptor(descriptor)
        Log.d("BluetoothLeService", "enableTxNotification() - 5")
    }
    companion object {
        val CCCD = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val RX_SERVICE_UUID = UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb")
        val NOTIFY_CHAR_UUID = UUID.fromString("0000fff4-0000-1000-8000-00805f9b34fb")
        val RX_CHAR_UUID = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb")

        const val ACTION_GATT_CONNECTED =
            "com.example.mhg.service.ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED =
            "com.example.mhg.service.ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.mhg.service.ACTION_GATT_SERVICES_DISCOVERED"
        const val ACTION_DATA_AVAILABLE =
            "com.example.mhg.service.ACTION_DATA_AVAILABLE"
        const val DEVICE_DOES_NOT_SUPPORT_UART =
            "com.example.mhg.service.DEVICE_DOES_NOT_SUPPORT_UART"

        private const val STATE_DISCONNECTED = 0
        private const val STATE_CONNECTED = 2

    }
    private fun broadcastUpdate(action: String) {
        val intent = Intent(action)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    @SuppressLint("MissingPermission")
    fun writeRxCharacteristic(value: ByteArray?) {
        val RxService: BluetoothGattService =
            mBluetoothGatt!!.getService(RX_SERVICE_UUID)
        showMessage("mBluetoothGatt null?: $mBluetoothGatt")

        val RxChar = RxService.getCharacteristic(RX_CHAR_UUID)
        if (RxChar == null) {
            showMessage("Rx characteristic not found")
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART)
            return
        }
        RxChar.setValue(value)
        val status: Boolean = mBluetoothGatt!!.writeCharacteristic(RxChar)
        Log.w("BluetoothLeService", "write TXChar - status=$status")
    }

    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic) {
        val intent = Intent(action)

        if (NOTIFY_CHAR_UUID == characteristic.uuid) {
            intent.putExtra(EXTRA_DATA, characteristic.value)
        } else {

        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
//        val intent = Intent(action)
//        when (characteristic.uuid) {
//            UUID.fromString(BLEGattAttributes.READ_WRITE) -> {
//                val flag = characteristic.properties
//                val format = when (flag and 0x01) {
//                    0x01 -> {
//                        Log.d(TAG, "Exercise Management format UINT16.")
//                        BluetoothGattCharacteristic.FORMAT_UINT16
//                    }
//                    else -> {
//                        Log.d(TAG, "Exercise Management format UINT8.")
//                        BluetoothGattCharacteristic.FORMAT_UINT8
//                    }
//                }
//                val heartRate = characteristic.getIntValue(format, 1)
//                Log.d(TAG, String.format("Received Exercise Management: %d", heartRate))
//                intent.putExtra(EXTRA_DATA, (heartRate).toString())
//            }
//            UUID.fromString((BLEGattAttributes.NOTIFY)) -> {
//                val flag = characteristic.properties
//                val format = when (flag and 0x01) {
//                    0x01 -> {
//                        Log.d(TAG, "Exercise Management format UINT16.")
//                        BluetoothGattCharacteristic.FORMAT_UINT16
//                    }
//                    else -> {
//                        Log.d(TAG, "Exercise Management format UINT8.")
//                        BluetoothGattCharacteristic.FORMAT_UINT8
//                    }
//                }
//                val heartRate = characteristic.getIntValue(format, 1)
//                Log.d(TAG, String.format("Received Exercise Management: %d", heartRate))
//                intent.putExtra(EXTRA_DATA, (heartRate).toString())
//            }
//            else -> {
//                // For all other profiles, writes the data formatted in HEX.
//                val data: ByteArray? = characteristic.value
//                if (data?.isNotEmpty() == true) {
//                    val hexString: String = data.joinToString(separator = " ") {
//                        String.format("%02X", it)
//                    }
//                    intent.putExtra(EXTRA_DATA, "$data\n$hexString")
//                }
//            }
//        }
//        sendBroadcast(intent)
    }

}
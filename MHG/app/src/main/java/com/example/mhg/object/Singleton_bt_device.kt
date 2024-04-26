package com.example.mhg.`object`

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.example.mhg.ReportGoalFragment
import com.example.mhg.service.BluetoothLeService
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized

class Singleton_bt_device private constructor(context:Context) {
    var init = false
    var mBtAdapter : BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    var mDevice : BluetoothDevice? = null
    var mDeviceList : MutableLiveData<ArrayList<BluetoothDevice>> = MutableLiveData()
    var mDeviceInfoList : ArrayList<ReportGoalFragment.BluetoothDeviceInfo> = arrayListOf()
    var txValue = byteArrayOf() ?: null
    init {

    }

//    fun autoConnect(address:String) : Boolean {
//        var mService: BluetoothLeService? = null
//        mService?.connect(address)
//        return true
//
//    }
    companion object {
        private var INSTANCE : Singleton_bt_device? = null

        @OptIn(InternalCoroutinesApi::class)
        fun getInstance(context: Context) : Singleton_bt_device =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Singleton_bt_device(context).also { INSTANCE = it }
            }
    }
}
package com.example.mhg

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mhg.Adapter.BLEListAdapter
import com.example.mhg.VO.BLEViewModel
import com.example.mhg.databinding.FragmentReportGoalBinding
import com.example.mhg.`object`.Singleton_bt_device
import com.example.mhg.`object`.CommonDefines
import com.example.mhg.`object`.TedPermissionWrapper
import com.example.mhg.service.BluetoothLeService
import java.text.SimpleDateFormat
import java.util.Date



class ReportGoalFragment : Fragment(), BLEListAdapter.onDeviceClickListener {
    lateinit var binding: FragmentReportGoalBinding
    private val viewModel: BLEViewModel by activityViewModels()
    var mDeviceInfoList: ArrayList<BluetoothDeviceInfo> = arrayListOf()
    private var mDevice: BluetoothDevice? = null
    private var isReceiverRegistered = false
    private  val REQUEST_SELECT_DEVICE = 1
    private  val REQUEST_ENABLE_BT = 2
    private var mBtAdapter: BluetoothAdapter? = null
    private var mDeviceList: ArrayList<BluetoothDevice>? = arrayListOf()
    private var mScanning = false
    private var mHandler: Handler? = null
    private  val SCAN_PERIOD: Long = 5000
    lateinit var singleton_bt_device : Singleton_bt_device
    private val UART_PROFILE_CONNECTED = 20
    private  val UART_PROFILE_DISCONNECTED = 21
    var mState = UART_PROFILE_DISCONNECTED
    var mService: BluetoothLeService? = null
    val adapter = BLEListAdapter(mDeviceInfoList, this)
    private var mPreTime: Long = 0
    private val sharedPref : SharedPreferences by lazy {
        requireActivity().getSharedPreferences("TangoQ", Context.MODE_PRIVATE)
    }

    fun saveDeviceAddress(address: String) {
        sharedPref.edit().putString("device_address", address).apply()
    }
    // variables
    private var mRealtimeOrWrite: Byte = 0x00 // 0x01: realtime, 0x00: write


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReportGoalBinding.inflate(inflater)
        return binding.root
    }
    @SuppressLint("MissingPermission", "NotifyDataSetChanged", "SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ------------------------! 싱글턴에서 가져오기 !--------------------------
        singleton_bt_device = Singleton_bt_device.getInstance(requireContext())

        mHandler = Handler()
        Log.w("init", "${singleton_bt_device.init}")
        TedPermissionWrapper.checkPermission(requireContext())
        mBtAdapter = singleton_bt_device.mBtAdapter // bluetoothadapter는 남아있음. mService
        if (mBtAdapter == null) {
            Toast.makeText(requireContext(), "Bluetooth is not avaliable", Toast.LENGTH_LONG).show()
            return
        }

        singleton_bt_device.mDeviceList.value = arrayListOf()
        if (singleton_bt_device.init == false) {
//            control_init()
            service_init()
            Log.w("serviceInit", "serviceInit Success !")
            singleton_bt_device.init = true
        }
        if (!mBtAdapter!!.isEnabled()) {
            Log.i(TAG, "BT not enabled yet")
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        }

        if (!requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(requireContext(), "Bluetooth low energy not supported", Toast.LENGTH_LONG).show()

        }

        //scanLeDevice(true);
        // TextView 키보드 UI 가릴때
        //scanLeDevice(true);
        // -----! scan 버튼 시작 !-----
        binding.btnScan.setOnClickListener {
            clearDevice()
            scanLeDevice(false)
            scanLeDevice(true)
            binding.rvDeviceList.visibility= View.VISIBLE
        }
        binding.btnConnect.setOnClickListener {
            if (mState == UART_PROFILE_DISCONNECTED) {
                try {
                    if (mService?.connect(mDevice!!.getAddress()) == true) {
                        scanLeDevice(false)
                        binding.btnConnect.setEnabled(false)
                        binding.btnDisconnect.setEnabled(true)
                        binding.rvDeviceList.visibility = View.GONE
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, ex.toString())
                    Toast.makeText(requireContext(), "연결 실패!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(requireContext(), "이미 디바이스에 연결되어 있습니다.", Toast.LENGTH_LONG)
                    .show()
            }
        }
        binding.btnDisconnect.setOnClickListener {
            if (mState == UART_PROFILE_CONNECTED) {
                mService?.disconnect()
            }
        }

        // -------------------------------! 연결 후 데이터 교환 시작 !-------------------------------
        binding.btnSend.setOnClickListener {
            if (singleton_bt_device.mDevice != null && mState == UART_PROFILE_CONNECTED) {
                val send_data: String = binding.etSendData.getText().toString()
                val buf = send_data.toByteArray()
                Log.d(TAG, "[send data - str] $send_data")
                var strTemp = ""
                var i = 0
                while (i < buf.size) {
                    strTemp += String.format("%02X ", buf[i])
                    i++
                }
                Log.d(TAG, "[send data - byte] $strTemp")
                binding.tvRecvData.append("[send] $strTemp\r\n")
                mService?.writeRxCharacteristic(buf)
            }
        }
        binding.btnSendGetCount.setOnClickListener {
            if (singleton_bt_device.mDevice != null && mState == UART_PROFILE_CONNECTED) {
                val curTime = System.currentTimeMillis()
                mPreTime = curTime
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                val date = Date(curTime)
                var strTemp = "[Request time] " + sdf.format(date)
                //binding.tvRecvData.append(strTemp + "\r\n");
                Log.d(TAG, strTemp)
                val buf = byteArrayOf(0x82.toByte(), 0x0D.toByte(), 0x0A.toByte())
                strTemp = ""
                var i = 0
                while (i < buf.size) {
                    strTemp += String.format("%02X ", buf[i])
                    i++
                }
                Log.d(TAG, "[send data - byte] $strTemp")
                binding.tvRecvData.append("[send] $strTemp\r\n")
                mService?.writeRxCharacteristic(buf)
            }
        }
        binding.btnSendSyncAck.setOnClickListener {
            if (mDevice != null && mState == UART_PROFILE_CONNECTED) {
                val buf = byteArrayOf(0x84.toByte(), 0x0D.toByte(), 0x0A.toByte())
                var strTemp = ""
                var i = 0
                while (i < buf.size) {
                    strTemp += String.format("%02X ", buf[i])
                    i++
                }
                Log.d(TAG, "[send data - byte] $strTemp")
                binding.tvRecvData.append("[send] $strTemp\r\n")
                mService?.writeRxCharacteristic(buf)
            }
        }
        binding.btnSendSyncStart.setOnClickListener {
            if (mDevice != null && mState == UART_PROFILE_CONNECTED) {
                val buf = byteArrayOf(0x83.toByte(), 0x0D.toByte(), 0x0A.toByte())
                var strTemp = ""
                var i = 0
                while (i < buf.size) {
                    strTemp += String.format("%02X ", buf[i])
                    i++
                }
                Log.d(TAG, "[send data - byte] $strTemp")
                binding.tvRecvData.append("[send] $strTemp\r\n")
                mService?.writeRxCharacteristic(buf)
            }
        }

        // -----! RVadapter 시작 !-----

        binding.rvDeviceList.adapter = adapter
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.rvDeviceList.layoutManager = linearLayoutManager
        adapter.notifyDataSetChanged()
        // -----! RVadapter 끝 !-----

        val txValue = byteArrayOf(0x00, 0x00, 0x63.toByte(), 0xB0.toByte(), 0xBD.toByte(), 0x4D.toByte())
        //byte[] txValue = new byte[] { (byte)0x82, (byte)0x06, (byte)0x00, (byte) 0x57};
        //byte[] txValue = new byte[] { (byte)0x82, (byte)0x06, (byte)0x00, (byte) 0x57};
        val time = CommonDefines.convertLittleEndianInt(txValue, 2, 4)
        val bTime = CommonDefines.convertBigEndianInt(txValue, 2, 4)
        Log.d(TAG, String.format("time:%d %d", time, bTime))

        val date = Date()
        date.setTime(time * 1000L)
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        //df.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        //df.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        Log.d(TAG, "Date little: " + df.format(date))
        date.setTime(bTime * 1000L)
        Log.d(TAG, "Date big: " + df.format(date))
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            mHandler?.postDelayed(Runnable {
                mScanning = false
                mBtAdapter?.stopLeScan(mLeScanCallback)
                binding.btnScan.setEnabled(true)
            }, SCAN_PERIOD)
            mScanning = true
            mBtAdapter?.startLeScan(mLeScanCallback)
            binding.btnScan.setEnabled(false)
        } else {
            mScanning = false
            mBtAdapter?.stopLeScan(mLeScanCallback)
            binding.btnScan.setEnabled(true)
        }
    }

    @SuppressLint("MissingPermission")
    private val mLeScanCallback =
        LeScanCallback { device, rssi, scanRecord ->
            Log.d(TAG, "dev name: " + device.getName() + ", addr: " + device.getAddress() + ", rssi: " + rssi)
            if (isAdded) {
                requireActivity().runOnUiThread(Runnable {
                    if (device.name != null)
                        addDevice(device, rssi)
                })
            }

        }

    @SuppressLint("MissingPermission")
    private fun addDevice(device: BluetoothDevice, rssi: Int) {
        var deviceFound = false
        for (listDev in singleton_bt_device.mDeviceList.value!!) {
            if (listDev.getAddress() == device.getAddress()) {
                deviceFound = true
                break
            }
        }
        if (!deviceFound) {
            singleton_bt_device.mDeviceList.value!!.add(device)
            val deviceName = device.name ?: "N/A"
            val deviceAddress = device.address ?: "N/A"
            mDeviceInfoList.add(
                BluetoothDeviceInfo(
                    deviceName,
                    deviceAddress,
                    rssi.toString() + "",
                    device
                )
            )
            adapter.notifyDataSetChanged()
        }
    }

    private fun clearDevice() {
        if (singleton_bt_device.mDevice != null) {
            singleton_bt_device.mDevice = null
        }
        singleton_bt_device.mDeviceList.value?.clear()
        singleton_bt_device.mDeviceInfoList.clear()
        adapter.notifyDataSetChanged()
    }

    private fun service_init() {
        val bindIntent: Intent = Intent(requireContext(), BluetoothLeService::class.java)
        requireActivity().bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mUartStatusChangeReceiver, makeGattUpdateIntentFilter())
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            intentFilter.addAction(BluetoothLeService.DEVICE_DOES_NOT_SUPPORT_UART)
            return intentFilter
    }

    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = (service as BluetoothLeService.LocalBinder).getService()
            Log.d(TAG, "onServiceConnection mService=" + mService)
            if (!mService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
//                finish()
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
        }
    }

    private val mUartStatusChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothLeService.ACTION_GATT_CONNECTED) {
                Log.d(TAG, "UART GATT Connected")
                mState = UART_PROFILE_CONNECTED
                binding.btnConnect.setEnabled(false)
                binding.btnConnect.setBackgroundColor(binding.btnConnect.resources.getColor(R.color.grey600))
                binding.btnDisconnect.setEnabled(true)
                binding.btnDisconnect.setBackgroundColor(binding.btnDisconnect.resources.getColor(R.color.mainColor))
                singleton_bt_device.mDevice = mDevice
                saveDeviceAddress(mDevice?.address.toString())
                Log.w("macAd저장", "${sharedPref.getString("device_address", null)}")

            } else if (action == BluetoothLeService.ACTION_GATT_DISCONNECTED) {
                Log.d(TAG, "UART Gatt Disconnected")
                mState = UART_PROFILE_DISCONNECTED
                mService?.close()
                requireActivity().runOnUiThread(Runnable {
                    binding.tvDeviceName.text = "기기 이름"
                    binding.tvMacAddress.text = "기기 주소"
                    binding.btnConnect.setEnabled(true)
                    binding.btnConnect.setBackgroundColor(binding.btnConnect.resources.getColor(R.color.mainColor))
                    binding.btnDisconnect.setEnabled(false)
                    binding.btnDisconnect.setBackgroundColor(binding.btnDisconnect.resources.getColor(R.color.grey600))

                    mDevice = null
                    singleton_bt_device.mDevice = null
                    Toast.makeText(requireContext(), "연결 종료!", Toast.LENGTH_LONG).show()
                })

            } else if (action == BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED) {
                mService?.enableTxNotification()

            } else if (action == BluetoothLeService.ACTION_DATA_AVAILABLE) {
                singleton_bt_device.txValue = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
                val txValue = singleton_bt_device.txValue
                requireActivity().runOnUiThread(Runnable {
                    try {
                        if (binding.tvRecvData.getLineCount() > 255) binding.tvRecvData.setText("")
                        val recvData = String(txValue!!)
                        val curTime = System.currentTimeMillis()
                        val duringTime: Float = (curTime - mPreTime) / 1000.0f
                        mPreTime = curTime
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                        val date = Date(curTime)
                        val strTemp = "[ProcessTime] " + sdf.format(date) + "\r\n" +
                                "\tResult Time: " + duringTime + "\r\n"
                        binding.tvRecvData.append(strTemp);
                        Log.d("dataAvailable", strTemp)
                        val hexData = StringBuilder()
                        for (b in txValue) {
                            hexData.append(String.format("%02x ", b.toInt() and 0xFF))
                        }
                        Log.d(TAG, String.format("RX Data(%d): %s", txValue.size, hexData.toString()))

                        val decimalData = StringBuilder()
                        for (b in txValue) {
                            decimalData.append(String.format("%d ", b.toInt() and 0xFF))
                        }
                        Log.d(TAG, String.format("RX Data(%d): %s", txValue.size, decimalData.toString()))
//                        if (cb_send_hex?.isChecked() == true)
                        binding.tvRecvData.append("[recv] $hexData\r\n")
                        binding.tvRecvData.append("[recv] $decimalData\r\n")
//                        else binding.tvRecvData!!.append(
//                            "[recv] $recvData\r\n"
//                        ) // + "\r\n");
                        binding.svRecvData.post(Runnable { binding.svRecvData.fullScroll(View.FOCUS_DOWN) })
                        recv_process(txValue)

                        //long time = CommonDefines.convertBigEndianInt(txValue, 2, 4);
                        //Log.d(TAG, String.format("time:%d", time));
                    } catch (e: Exception) {
                        Log.e(TAG, e.toString())
                    }
                })
            } else if (action == BluetoothLeService.DEVICE_DOES_NOT_SUPPORT_UART) {
                Log.d(TAG, "Device doesn't support UART")
                mService?.disconnect()
            }
        }
    }

    private fun recv_process(rxData: ByteArray?) {
        val cmd = rxData!![0]
        val ack = rxData[1]
        var time: Long = 0
        var index = 0
        when (cmd) {
            CommonDefines.CMD_GET_COUNT -> {
                if (ack != CommonDefines.CMD_ACK) return
                val get_count = CommonDefines.convertBigEndianInt(rxData, 2, 2).toInt()
                Log.d(TAG, "[GET COUNT] $get_count")
            }

            else -> {
                if (rxData.size == 10) {
                    index = CommonDefines.convertBigEndianInt(rxData, 0, 2).toInt()
                    time = CommonDefines.convertBigEndianInt(rxData, 2, 4)
                    val date = Date()
                    date.setTime(time * 1000L)
                    val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    Log.d(TAG, "[SYNC START] index: " + index + ", Date: " + df.format(date) + ", " + time)
                }
            }
        }
    }

//    private fun control_init() {
//        tv_device_name = view?.findViewById<View>(R.id.tvDeviceName) as TextView
//        tv_mac_address = view?.findViewById<View>(R.id.tvMacAddress) as TextView
//        btn_connect = view?.findViewById<View>(R.id.btnConnect) as Button
//        btn_disconnect = view?.findViewById<View>(R.id.btnDisconnect) as Button
//        tb_send_data = view?.findViewById<View>(R.id.etSendData) as EditText
//        btn_send = view?.findViewById<View>(R.id.btnSend) as Button
//        tv_recv_data = view?.findViewById<View>(R.id.tvRecvData) as TextView
//        sv_recv_data = view?.findViewById<View>(R.id.svRecvData) as NestedScrollView
//        cb_send_hex = view?.findViewById<View>(R.id.cb_send_hex) as CheckBox
//        btn_send_get_count = view?.findViewById<View>(R.id.btnSendGetCount) as Button
//        btn_send_sync_start = view?.findViewById<View>(R.id.btnSendSyncStart) as Button
//        btn_send_sync_ack = view?.findViewById<View>(R.id.btnSendSyncAck) as Button
//        btn_send_time = view?.findViewById<View>(R.id.btnSendTime) as Button
//        btn_send_realtime_or_write = view?.findViewById<View>(R.id.btnSendRealTimeOrWrite) as Button

//        btn_connect!!.setOnClickListener(onClickListener)
//        btn_disconnect!!.setOnClickListener(onClickListener)
//        btn_send!!.setOnClickListener(onClickListener)
//        cb_send_hex!!.setOnClickListener(onClickListener)
//        btn_send_get_count!!.setOnClickListener(onClickListener)
//        btn_send_sync_start!!.setOnClickListener(onClickListener)
//        btn_send_sync_ack!!.setOnClickListener(onClickListener)
//        btn_send_time!!.setOnClickListener(onClickListener)
//        btn_send_realtime_or_write!!.setOnClickListener(onClickListener)
//        mLayoutManager = LinearLayoutManager(requireContext())
//        recyclerView!!.setLayoutManager(mLayoutManager)
//        recyclerView = view?.findViewById<View>(R.id.rvDeviceList) as RecyclerView
//        mAdapter = DeviceAdapter(mDeviceInfoList)
//        recyclerView!!.setAdapter(mAdapter)
//    }

//    var onClickListener = View.OnClickListener { v ->
//        when (v.id) {
//            R.id.btn_send_realtime_or_write -> if (mDevice != null && mState == UART_PROFILE_CONNECTED) {
//                if (mRealtimeOrWrite.toInt() == 0x00) mRealtimeOrWrite =
//                    0x01 else mRealtimeOrWrite = 0x00
//                val buf = ByteArray(4)
//                buf[0] = CommonDefines.CMD_REALTIME_OR_WRITE
//                buf[1] = mRealtimeOrWrite
//                buf[2] = 0x0D
//                buf[3] = 0x0A
//                var strTemp = ""
//                var i = 0
//                while (i < buf.size) {
//                    strTemp += String.format("%02X ", buf[i])
//                    i++
//                }
//                Log.d(
//                    TAG,
//                    String.format(
//                        "[send data - realtime or write(%02X))] %s",
//                        mRealtimeOrWrite,
//                        strTemp
//                    )
//                )
//                tv_recv_data?.append("[send] $strTemp\r\n")
//                mService?.writeRxCharacteristic(buf)
//            }

//            R.id.btn_send_time -> if (mDevice != null && mState == UART_PROFILE_CONNECTED) {
//                val date = Date()
//                Log.d(
//                    TAG, "[TIME] year:" + (date.year + 1900)
//                            + ", month:" + (date.month + 1)
//                            + ", day:" + date.date
//                            + ", hour:" + date.hours
//                            + ", min:" + date.minutes
//                            + ", sec:" + date.seconds
//                )
//                val buf = ByteArray(9)
//                buf[0] = CommonDefines.CMD_SET_TIME
//                buf[1] = (date.year - 100).toByte()
//                buf[2] = (date.month + 1).toByte()
//                buf[3] = date.date.toByte()
//                buf[4] = date.hours.toByte()
//                buf[5] = date.minutes.toByte()
//                buf[6] = date.seconds.toByte()
//                buf[7] = 0x0D
//                buf[8] = 0x0A
//                var strTemp = ""
//                var i = 0
//                while (i < buf.size) {
//                    strTemp += String.format("%02X ", buf[i])
//                    i++
//                }
//                Log.d(TAG, "[send data - time] $strTemp")
//                tv_recv_data?.append("[send] $strTemp\r\n")
//                mService?.writeRxCharacteristic(buf)
//            }

//            R.id.btn_send_sync_ack -> if (mDevice != null && mState == UART_PROFILE_CONNECTED) {
//                val buf = byteArrayOf(0x84.toByte(), 0x0D.toByte(), 0x0A.toByte())
//                var strTemp = ""
//                var i = 0
//                while (i < buf.size) {
//                    strTemp += String.format("%02X ", buf[i])
//                    i++
//                }
//                Log.d(TAG, "[send data - byte] $strTemp")
//                tv_recv_data?.append("[send] $strTemp\r\n")
//                mService?.writeRxCharacteristic(buf)
//            }

//            R.id.btn_send_sync_start -> if (mDevice != null && mState == UART_PROFILE_CONNECTED) {
//                val buf = byteArrayOf(0x83.toByte(), 0x0D.toByte(), 0x0A.toByte())
//                var strTemp = ""
//                var i = 0
//                while (i < buf.size) {
//                    strTemp += String.format("%02X ", buf[i])
//                    i++
//                }
//                Log.d(TAG, "[send data - byte] $strTemp")
//                tv_recv_data?.append("[send] $strTemp\r\n")
//                mService?.writeRxCharacteristic(buf)
//            }

//            R.id.btnSendGetCount -> if (viewModel.mDevice != null && mState == UART_PROFILE_CONNECTED) {
//                val curTime = System.currentTimeMillis()
//                mPreTime = curTime
//                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
//                val date = Date(curTime)
//                var strTemp = "[Request time] " + sdf.format(date)
//                //tv_recv_data.append(strTemp + "\r\n");
//                Log.d(TAG, strTemp)
//                val buf = byteArrayOf(0x82.toByte(), 0x0D.toByte(), 0x0A.toByte())
//                strTemp = ""
//                var i = 0
//                while (i < buf.size) {
//                    strTemp += String.format("%02X ", buf[i])
//                    i++
//                }
//                Log.d(TAG, "[send data - byte] $strTemp")
//                tv_recv_data?.append("[send] $strTemp\r\n")
//                mService?.writeRxCharacteristic(buf)
//            }

//            R.id.cb_send_hex -> {
//                Log.d(TAG, "checkbox hex:" + cb_send_hex?.isChecked())
//                if (cb_send_hex?.isChecked() == true) {
//                    val send_data: String = tb_send_data?.getText().toString()
//                }
//            }

//            R.id.btnSend -> if (viewModel.mDevice != null && mState == UART_PROFILE_CONNECTED) {
//                val send_data: String = tb_send_data?.getText().toString()
//                val buf = send_data.toByteArray()
//                Log.d(TAG, "[send data - str] $send_data")
//                var strTemp = ""
//                var i = 0
//                while (i < buf.size) {
//                    strTemp += String.format("%02X ", buf[i])
//                    i++
//                }
//                Log.d(TAG, "[send data - byte] $strTemp")
//                tv_recv_data?.append("[send] $strTemp\r\n")
//                mService?.writeRxCharacteristic(buf)
//            }

//            R.id.btnConnect -> if (mDevice != null) {
//                if (mState == UART_PROFILE_DISCONNECTED) {
//                    try {
//                        if (mService?.connect(mDevice!!.getAddress()) == true) {
//                            scanLeDevice(false)
//                            btn_connect?.setEnabled(false)
//                            btn_disconnect?.setEnabled(true)
//                        }
//                    } catch (ex: Exception) {
//                        Log.e(TAG, ex.toString())
//                        Toast.makeText(requireContext(), "연결 실패!", Toast.LENGTH_LONG).show()
//                    }
//                } else {
//                    Toast.makeText(requireContext(), "이미 디바이스에 연결되어 있습니다.", Toast.LENGTH_LONG)
//                        .show()
//                }
//            }
//            else {
//                Toast.makeText(requireContext(), "연결할 장치를 선택해 주세요.", Toast.LENGTH_LONG).show()
//            }
//            R.id.btnDisconnect -> if (mState == UART_PROFILE_CONNECTED) {
//                mService?.disconnect()
//            }
//        }
//    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume()")
        if (!isReceiverRegistered) {
            if (!mBtAdapter?.isEnabled()!!) {
                Log.i(TAG, "onResume() - BT not enabled yet")
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
            }
        }

    }

    @SuppressLint("MissingPermission")
    override fun onPause() {
        super.onPause()
        if (isReceiverRegistered) {
//            requireContext().unregisterReceiver(mUartStatusChangeReceiver)
//            isReceiverRegistered = false
            mBtAdapter?.stopLeScan(mLeScanCallback)
        }
    }
    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy() but, enabled mBtAdapter")
        if (isReceiverRegistered) {
//            try {
//                LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mUartStatusChangeReceiver)
//            } catch (e: Exception) {
//                Log.e(TAG, e.toString())
//            }
            mBtAdapter?.stopLeScan(mLeScanCallback)
//            requireActivity().unbindService(mServiceConnection)
//            if (mService != null) {
//                mService!!.stopSelf()
//                mService = null
//            }
//            isReceiverRegistered = false
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(
            TAG,
            "onActivityResult() requestCode: $requestCode, resultCode: $resultCode"
        )
        when (requestCode) {
            REQUEST_SELECT_DEVICE -> if (resultCode == Activity.RESULT_OK && data != null) {
                //mDevice =
            }

            REQUEST_ENABLE_BT -> if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(requireContext(), "Bluetooth has turned on ", Toast.LENGTH_LONG).show()
            } else {
                Log.d(TAG, "BT not enabled")
                Toast.makeText(requireContext(), "Problem in BT Turning ON ", Toast.LENGTH_LONG).show()
//                finish()
            }

            else -> Log.e(TAG, "wrong requestCode")
        }
    }
    @SuppressLint("SetTextI18n")
    override fun onDeviceClick(device: BluetoothDeviceInfo) {
        // 아이템 클릭 시 동작
        binding.tvDeviceName.setText("기기 이름: " + device.device_name)
        binding.tvMacAddress.setText("기기 주소: " + device.mac_address)
        mDevice = device.device
    }

    override fun onDeviceLongClick(device: BluetoothDeviceInfo): Boolean {
        Log.e(TAG, "[LongClick] Dev Name: " + device.device_name + ", mac address: " + device.mac_address + ", rssi: " + device.rssi)
        return true
    }

    class BluetoothDeviceInfo(
        var device_name: String,
        var mac_address: String,
        var rssi: String,
        var device: BluetoothDevice
    )


//    internal class DeviceAdapter(private val deviceList: List<BluetoothDeviceInfo>) :
//        RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {
//        inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
//            var tv_device_name: TextView
//            var tv_mac_address: TextView
//            var tv_rssi: TextView
//
//            init {
//                tv_device_name = v.findViewById<View>(R.id.tvBtName) as TextView
//                tv_mac_address = v.findViewById<View>(R.id.tvBtAddress) as TextView
//                tv_rssi = v.findViewById<View>(R.id.tvBleSearched) as TextView
//            }
//        }
//
//        override fun onCreateViewHolder(
//            parent: ViewGroup,
//            viewType: Int
//        ): ViewHolder {
//            val v: View = LayoutInflater.from(parent.context)
//                .inflate(R.layout.rv_ble_list, parent, false)
//            return ViewHolder(v)
//        }
//
//        @SuppressLint("SetTextI18n")
//        override fun onBindViewHolder(
//            holder: ViewHolder,
//            @SuppressLint("RecyclerView") position: Int
//        ) {
//            val item = deviceList[position]
//            holder.tv_device_name.text = item.device_name
//            holder.tv_mac_address.text = item.mac_address
//            holder.tv_rssi.text = item.rssi
//            holder.itemView.setOnClickListener {
//                Log.e(
//                    TAG,
//                    "Pos: " + position + ", Dev Name: " + item.device_name + ", mac address: " + item.mac_address + ", rssi: " + item.rssi
//                )
//                tv_device_name?.setText("기기 이름: " + item.device_name)
//                tv_mac_address?.setText("기기 주소: " + item.mac_address)
//                mDevice = item.device
//            }
//            holder.itemView.setOnLongClickListener {
//                Log.e(
//                    TAG,
//                    "[LongClick] Pos: " + position + ", Dev Name: " + item.device_name + ", mac address: " + item.mac_address + ", rssi: " + item.rssi
//                )
//                true
//            }
//        }
//
//        override fun getItemCount(): Int {
//            return deviceList.size
//        }
//    }
}
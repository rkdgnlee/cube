package com.example.mhg

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.room.Room
import com.example.mhg.VO.UserViewModel
import com.example.mhg.databinding.ActivityMainBinding
import com.example.mhg.`object`.Singleton_bt_device
import com.example.mhg.`object`.Singleton_t_user
import com.example.mhg.`object`.TedPermissionWrapper
import com.example.mhg.service.BluetoothLeService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.text.SimpleDateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private val sharedPref : SharedPreferences by lazy {
        getSharedPreferences("TangoQ", Context.MODE_PRIVATE)
    }
    lateinit var bt_device : Singleton_bt_device
    var mService: BluetoothLeService? = null
    private var mDevice: BluetoothDevice? = null
    private var mBtAdapter : BluetoothAdapter? = null
    private  val REQUEST_ENABLE_BT = 2
    private val UART_PROFILE_CONNECTED = 20
    private  val UART_PROFILE_DISCONNECTED = 21
    var mState = UART_PROFILE_DISCONNECTED
    private var mPreTime: Long = 0
    val viewModel: UserViewModel by viewModels()
    @SuppressLint("CommitTransaction")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val t_userData = Singleton_t_user.getInstance(this)
        viewModel.User.value = t_userData.jsonObject
        Log.e("싱글톤>뷰모델", "${viewModel.User.value}")
        // --------! ble 정보로 자동 연결 시작 !---------

        bt_device = Singleton_bt_device.getInstance(this)
        val deviceAddress = sharedPref.getString("device_address", null)
        mBtAdapter = bt_device.mBtAdapter

        Log.w("macAd저장", "$deviceAddress")
        TedPermissionWrapper.checkPermission(this)
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not avaliable", Toast.LENGTH_LONG).show()
            return
        }
//        bt_device.mDeviceList.value = arrayListOf()
        if (bt_device.init == false) {
//            control_init()
            service_init()
            Log.w("serviceInit", "serviceInit Success !")
            bt_device.init = true
        }
        if (!mBtAdapter!!.isEnabled()) {
            Log.i(ContentValues.TAG, "BT not enabled yet")
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth low energy not supported", Toast.LENGTH_LONG).show()
        }
        if (mScanning == false) {
            scanLeDevice(true)
        }
//        if (deviceAddress != null) {
//            if (mState == UART_PROFILE_DISCONNECTED) {
//                try {
//                    if (mService?.connect(deviceAddress) == true) {
//                        if (bt_device.mDevice != null && mState == UART_PROFILE_CONNECTED) {
//                            Log.v("connect성공", "connect 성공")
//                            var mPreTime: Long = 0
//                            val curTime = System.currentTimeMillis()
//                            mPreTime = curTime
//                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
//                            val date = Date(curTime)
//                            var strTemp = "[Request time] " + sdf.format(date)
//                            //binding.tvRecvData.append(strTemp + "\r\n");
//                            Log.d(ContentValues.TAG, strTemp)
//                            val buf = byteArrayOf(0x82.toByte(), 0x0D.toByte(), 0x0A.toByte())
//                            strTemp = ""
//                            var i = 0
//                            while (i < buf.size) {
//                                strTemp += String.format("%02X ", buf[i])
//                                i++
//                            }
//                            Log.d(ContentValues.TAG, "[send data - byte] $strTemp")
//
//                            Toast.makeText(this@MainActivity, "[send] $strTemp\r\n", Toast.LENGTH_SHORT).show()
////                        binding.tvRecvData.append("[send] $strTemp\r\n")
//                            mService?.writeRxCharacteristic(buf)
//                        }
//                    }
//                } catch (ex: Exception) {
//                    Log.e(ContentValues.TAG, ex.toString())
//                    Toast.makeText(this, "연결 실패!", Toast.LENGTH_LONG).show()
//                    Log.w("leService", "Null if문 실행 안됨")
//                }
//            } else {
//                Toast.makeText(this, "이미 디바이스에 연결되어 있습니다.", Toast.LENGTH_LONG)
//                    .show()
//            }
////
//        }



        // --------! ble 정보로 자동 끝 !---------

//        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        // -----! 화면 초기화 !-----
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().apply {
                replace(R.id.flMain, HomeFragment())
                commit()
            }
        }

        // -----! 알람 경로 설정 !-----
        val fromAlarmActivity = intent.getBooleanExtra("fromAlarmActivity", false)
        Log.v("TAG", "$fromAlarmActivity")
        if (fromAlarmActivity) {
            val fragmentId = intent.getStringExtra("fragmentId")
            Log.v("TAG", "$fragmentId")
            if (fragmentId != null) {
                val fragment = FragmentFactory.createFragmentById(fragmentId)
                Log.v("TAG", "$fragment")
                when (fragmentId) {
//                    fragment에서 bnb 색 변하게 하기
                    "home_beginner", "home_expert", "home_intermediate" -> {
                        val homeFragment = HomeFragment.newInstance(fragmentId)
                        binding.bnbMain.selectedItemId = R.id.home
                        setCurrentFragment(homeFragment)

                    }
                    "report_skeleton", "report_detail", "report_goal" -> {
                        val reportFragment = ReportFragment.newInstance(fragmentId)
                        binding.bnbMain.selectedItemId = R.id.report
                        setCurrentFragment(reportFragment)
                    }
                    "pick" -> {
                        binding.bnbMain.selectedItemId = R.id.pick
                        setCurrentFragment(fragment)
                    }
                    "profile" -> {
                        binding.bnbMain.selectedItemId = R.id.profile
                        setCurrentFragment(fragment)
                    }
                }
            }
        }


        // -----! 바텀 네비 바 경로 설정 -----!
//        binding.bnbMain.setOnNavigationItemReselectedListener(null)
        binding.bnbMain.setOnItemSelectedListener {
            when(it.itemId) {
                // ---- fragment 경로 지정 시작 ----
                R.id.home -> setCurrentFragment(HomeFragment())
                R.id.report -> setCurrentFragment(ReportFragment())
                R.id.pick -> setCurrentFragment(PickFragment())
                R.id.profile -> setCurrentFragment(ProfileFragment())
            }
            true
        }
        binding.bnbMain.setOnItemReselectedListener {
            when(it.itemId) {
                // ---- fragment 경로 지정 시작 ----
                R.id.home -> {}
                R.id.report -> {}
                R.id.pick -> {}
                R.id.profile -> {}
            }
        }
        // ---- fragment 경로 지정  끝----
        binding.imgbtnAlarm.setOnClickListener {
            val intent = Intent(this, AlarmActivity::class.java)
            startActivity(intent)
        }
    }
    private var mHandler: Handler? = null
    private var mScanning = false
    private  val SCAN_PERIOD: Long = 5000
    private var mConnectionState = false
    fun setCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.flMain, fragment)
//            addToBackStack(null)
            commit()
        }
    private val onBackPressedCallback = object:OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {

        }
    }
    @SuppressLint("MissingPermission")
    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            mScanning = true
            mBtAdapter?.startLeScan(mLeScanCallback)
        } else {
            mScanning = false
            mBtAdapter?.stopLeScan(mLeScanCallback)
        }
    }
    @SuppressLint("MissingPermission")
    private val mLeScanCallback =
        BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
            val deviceAddress = sharedPref.getString("device_address", null)
            if (device.address == deviceAddress) {
                mDevice = device
                Log.d(
                    ContentValues.TAG,
                    "dev name: " + device.getName() + ", addr: " + device.getAddress() + ", rssi: " + rssi
                )
                scanLeDevice(false)


//                if (mState == UART_PROFILE_DISCONNECTED && mConnectionState == false) {
//                    Log.v("mDevice", "현재 mDevice: ${mDevice?.address}")
//                    Log.v("mService", "현재 mService상태: $mService")
//                    mService?.connect(mDevice!!.getAddress())
//                    Log.v("connect", "시도")
//                    mConnectionState = true
//                }
            }

//            if (isAdded) {
//                requireActivity().runOnUiThread(Runnable {
//                    if (device.name != null)
//                        addDevice(device, rssi)
//                })
//            }

        }
    private fun service_init() {
        val bindIntent: Intent = Intent(this, BluetoothLeService::class.java)
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mUartStatusChangeReceiver, makeGattUpdateIntentFilter())
    }
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            mService = (service as BluetoothLeService.LocalBinder).getService()
            Log.d(ContentValues.TAG, "onServiceConnection mService=" + mService)
            mService!!.initialize()
            Log.v("mDevice", "현재 mDevice: ${mDevice?.address}")
            Log.v("mService", "현재 mService상태: $mService")
            Log.v("mBtAdapter", "현재 mBtAdapter상태: ${bt_device.mBtAdapter}, $mBtAdapter")
            if (mDevice != null || mDevice != null) {
                mService!!.connect(mDevice!!.getAddress())
            }
            if (!mService!!.initialize()) {
                Log.e(ContentValues.TAG, "Unable to initialize Bluetooth")
//                finish()
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
        }
    }

    private fun makeGattUpdateIntentFilter(): IntentFilter {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        intentFilter.addAction(BluetoothLeService.ACTION_FIND_CHARACTERISTIC_FINISHED)
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
        intentFilter.addAction(BluetoothLeService.DEVICE_DOES_NOT_SUPPORT_UART)

        return intentFilter
    }
    private val mUartStatusChangeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (action == BluetoothLeService.ACTION_GATT_CONNECTED) {
                Log.d(ContentValues.TAG, "UART GATT Connected")
                mState = UART_PROFILE_CONNECTED
//                binding.btnConnect.setEnabled(false)
//                binding.btnConnect.setBackgroundColor(binding.btnConnect.resources.getColor(R.color.grey600))
//                binding.btnDisconnect.setEnabled(true)
//                binding.btnDisconnect.setBackgroundColor(binding.btnDisconnect.resources.getColor(R.color.mainColor))
//                singleton_bt_device.mDevice = mDevice
//                saveDeviceAddress(mDevice?.address.toString())
                Log.w("BLE", "BLE 자동 연결 성공 ")
                Log.w("macAd저장", "${sharedPref.getString("device_address", "default")}")

            } else if (action == BluetoothLeService.ACTION_GATT_DISCONNECTED) {
//                Log.d(ContentValues.TAG, "UART Gatt Disconnected")
//                mState = UART_PROFILE_DISCONNECTED
//                mService?.close()
//                requireActivity().runOnUiThread(Runnable {
//                    binding.tvDeviceName.text = "기기 이름"
//                    binding.tvMacAddress.text = "기기 주소"
//                    binding.btnConnect.setEnabled(true)
//                    binding.btnConnect.setBackgroundColor(binding.btnConnect.resources.getColor(R.color.mainColor))
//                    binding.btnDisconnect.setEnabled(false)
//                    binding.btnDisconnect.setBackgroundColor(binding.btnDisconnect.resources.getColor(R.color.grey600))
//
//                    mDevice = null
//                    singleton_bt_device.mDevice = null
//                    Toast.makeText(requireContext(), "연결 종료!", Toast.LENGTH_LONG).show()
//                })

            } else if (action == BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED) {
                mService?.enableTxNotification()

            } else if (action == BluetoothLeService.ACTION_FIND_CHARACTERISTIC_FINISHED) {
                Log.v("broadcast", "FCF action success?") // success 여기까지 됨.
                if (mDevice != null && mState == UART_PROFILE_CONNECTED) {
                    Log.v("connect성공", "FIND_CHARACTERISTIC_FINISHED")
                    var mPreTime: Long = 0
                    val curTime = System.currentTimeMillis()
                    mPreTime = curTime
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                    val date = Date(curTime)
                    var strTemp = "[Request time] " + sdf.format(date)
                    //binding.tvRecvData.append(strTemp + "\r\n");
                    Log.d(ContentValues.TAG, strTemp)
                    val buf = byteArrayOf(0x82.toByte(), 0x0D.toByte(), 0x0A.toByte())
                    strTemp = ""
                    var i = 0
                    while (i < buf.size) {
                        strTemp += String.format("%02X ", buf[i])
                        i++
                    }
                    Log.d(ContentValues.TAG, "[send data - byte] $strTemp")

                    Toast.makeText(this@MainActivity, "[send] $strTemp\r\n", Toast.LENGTH_LONG).show()
//                        binding.tvRecvData.append("[send] $strTemp\r\n")
                    mService?.writeRxCharacteristic(buf)
                }
            } else if (action == BluetoothLeService.ACTION_DATA_AVAILABLE) {
                bt_device.txValue = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA)
                val txValue = bt_device.txValue
                runOnUiThread(Runnable {
                    try {
//                        if (binding.tvRecvData.getLineCount() > 255) binding.tvRecvData.setText("")
                        val recvData = String(txValue!!)
                        val curTime = System.currentTimeMillis()
                        val duringTime: Float = (curTime - mPreTime) / 1000.0f
                        mPreTime = curTime
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")
                        val date = Date(curTime)
                        val strTemp = "[ProcessTime] " + sdf.format(date) + "\r\n" +
                                "\tResult Time: " + duringTime + "\r\n"
//                        binding.tvRecvData.append(strTemp);
                        Log.d("dataAvailable", strTemp)
                        val hexData = StringBuilder()
                        for (b in txValue) {
                            hexData.append(String.format("%02x ", b.toInt() and 0xFF))
                        }
                        Log.d(ContentValues.TAG, String.format("RX Data(%d): %s", txValue.size, hexData.toString()))

                        val decimalData = StringBuilder()
                        for (b in txValue) {
                            decimalData.append(String.format("%d ", b.toInt() and 0xFF))
                        }
                        Log.d(ContentValues.TAG, String.format("RX Data(%d): %s", txValue.size, decimalData.toString()))
//                        if (cb_send_hex?.isChecked() == true)
//                        binding.tvRecvData.append("[recv] $hexData\r\n")
//                        binding.tvRecvData.append("[recv] $decimalData\r\n")
                        Toast.makeText(this@MainActivity, "[recv] $hexData\r\n", Toast.LENGTH_SHORT).show()
                        Toast.makeText(this@MainActivity, "[recv] $decimalData\r\n", Toast.LENGTH_SHORT).show()

//                        else binding.tvRecvData!!.append(
//                            "[recv] $recvData\r\n"
//                        ) // + "\r\n");
//                        binding.svRecvData.post(Runnable { binding.svRecvData.fullScroll(View.FOCUS_DOWN) })
//                        recv_process(txValue)

                        //long time = CommonDefines.convertBigEndianInt(txValue, 2, 4);
                        //Log.d(TAG, String.format("time:%d", time));
                    } catch (e: Exception) {
                        Log.e(ContentValues.TAG, e.toString())
                    }
                })
            } else if (action == BluetoothLeService.DEVICE_DOES_NOT_SUPPORT_UART) {
                Log.d(ContentValues.TAG, "Device doesn't support UART")
                mService?.disconnect()
            }
        }
    }
}
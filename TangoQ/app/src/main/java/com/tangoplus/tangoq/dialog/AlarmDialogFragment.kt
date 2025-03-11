package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.tangoplus.tangoq.MainActivity
import com.tangoplus.tangoq.adapter.AlarmRVAdapter
import com.tangoplus.tangoq.callback.SwipeHelperCallback
import com.tangoplus.tangoq.vo.MessageVO
import com.tangoplus.tangoq.databinding.FragmentAlarmDialogBinding
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.listener.OnAlarmClickListener
import com.tangoplus.tangoq.listener.OnAlarmDeleteListener
import com.tangoplus.tangoq.db.Singleton_t_user
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates

class AlarmDialogFragment : DialogFragment(), OnAlarmClickListener, OnAlarmDeleteListener {
    lateinit var binding : FragmentAlarmDialogBinding
    private lateinit var swipeHelperCallback: SwipeHelperCallback
    private lateinit var alarmRVAdapter : AlarmRVAdapter
    private var alarmList = mutableListOf<MessageVO>()
    private lateinit var pm: PreferencesManager

    private var userSn by Delegates.notNull<Int>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAlarmDialogBinding.inflate(inflater)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility", "NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pm = PreferencesManager(requireContext())
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        binding.ibtnAlarmBack.setOnClickListener { dismiss() }
        userSn = userJson?.optInt("sn") ?:0
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
//                Log.v("알람리스트", "${pm.getAlarms(userSn)}")
                alarmList = pm.getAlarms(userSn)
                alarmList.sortByDescending { it.timeStamp }
                // ------! alarm touchhelper 연동 시작 !------
                if (alarmList.isEmpty()) {
                    binding.tvAlarm.visibility = View.VISIBLE
                } else {
                    binding.tvAlarm.visibility = View.GONE
                }

                alarmRVAdapter = AlarmRVAdapter(this@AlarmDialogFragment, alarmList, this@AlarmDialogFragment, this@AlarmDialogFragment)
                swipeHelperCallback = SwipeHelperCallback().apply {
                    setClamp(250f)
                }

                val itemTouchHelper = ItemTouchHelper(swipeHelperCallback)
                itemTouchHelper.attachToRecyclerView(binding.rvAlarm)
                binding.rvAlarm.apply {
                    layoutManager = LinearLayoutManager(requireContext().applicationContext)
                    adapter = alarmRVAdapter
                    setOnTouchListener{ _, _ ->
                        swipeHelperCallback.removePreviousClamp(binding.rvAlarm)
                        false
                    }
                } // -----! alarm touchhelper 연동 끝 !-----

            }
        }

        binding.tvAlarmClear.setOnClickListener {
            alarmList.clear()
            pm.deleteAllAlarms(userSn)
            alarmRVAdapter.notifyDataSetChanged()
        }
    }

    override fun onAlarmClick(fragmentId: String) {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("fragmentId", fragmentId)
        intent.putExtra("fromAlarmActivity", true)
        startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onAlarmDelete(timeStamp: Long?) {
        // ------! 삭제 후 스와이프 초기화 시작 !------
        swipeHelperCallback.removePreviousClamp(binding.rvAlarm)
        binding.rvAlarm.adapter?.notifyDataSetChanged()
        binding.rvAlarm.post {
            binding.rvAlarm.invalidateItemDecorations()
        }
        alarmList.remove(alarmList.find { it.timeStamp == timeStamp })
        pm.deleteAlarm(userSn, timeStamp ?: 0L)


        // ------! 삭제 후 스와이프 초기화 끝 !------
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}
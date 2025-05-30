package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.broadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.viewmodel.ExerciseViewModel
import com.tangoplus.tangoq.databinding.FragmentPlayThumbnailDialogBinding
import com.tangoplus.tangoq.fragment.ExtendedFunctions.setOnSingleClickListener
import com.tangoplus.tangoq.function.WifiManager
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import com.tangoplus.tangoq.viewmodel.ProgressViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.core.net.toUri
import androidx.core.graphics.drawable.toDrawable

class PlayThumbnailDialogFragment : DialogFragment() {
    lateinit var binding : FragmentPlayThumbnailDialogBinding
    private var videoUrl = ""
    val evm: ExerciseViewModel by activityViewModels()
    val pvm: PlayViewModel by activityViewModels()
    val progressVm: ProgressViewModel by activityViewModels()
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var playbackPosition = 0L
    private lateinit var prefs : PreferencesManager
    private lateinit var wm : WifiManager
    interface DialogCloseListener {
        fun onDialogClose()
    }

    private var dialogCloseListener: DialogCloseListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppTheme_DialogFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPlayThumbnailDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // api35이상 화면 크기 조절
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            // 상태 표시줄 높이만큼 상단 패딩 적용
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val bundle = arguments
        // 값 받아오기
        pvm.exerciseData = bundle?.getParcelable("ExerciseUnit")
        pvm.isProgram = false
        pvm.isProgram = bundle?.getBoolean("isProgram") ?: false
        pvm.uvpSn = bundle?.getInt("uvpSn") ?: 0
        wm = WifiManager(requireContext())

        // ------# like 있는지 판단 #------
        prefs = PreferencesManager(requireContext())
//        var isLike = false
//        if (prefs.existLike(pvm.exerciseData?.exerciseId.toString())) {
//            isLike = true
//            binding.ibtnPTDLike.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_like_enabled))
//        }
        // -----! 각 설명들 textView에 넣기 !-----
        videoUrl = pvm.exerciseData?.videoFilepath.toString()

        binding.tvPTDName.text = pvm.exerciseData?.exerciseName.toString()
        binding.tvPTDRelatedJoint.text = pvm.exerciseData?.relatedJoint.toString()

        binding.tvPTDTime.text = "${pvm.exerciseData?.duration?.toInt()?.div(60)}분 ${pvm.exerciseData?.duration?.toInt()?.rem(60)}초"
        binding.tvPTDStage.text = pvm.exerciseData?.exerciseStage
        binding.tvPTDFrequency.text = pvm.exerciseData?.exerciseFrequency.toString()
        binding.tvPTRelatedSymptom.text = pvm.exerciseData?.relatedSymptom

        binding.tvPTDMethod.text = pvm.exerciseData?.exerciseMethod.toString()
        binding.tvPTDCaution.text = pvm.exerciseData?.exerciseCaution.toString()
        binding.tvPTDRelatedMuscle.text = pvm.exerciseData?.relatedMuscle.toString()

        Glide.with(requireContext())
            .load("${pvm.exerciseData?.imageFilePath}")
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .override(1080)
            .into(binding.ivPTD)

        val networkType = wm.checkNetworkType()
//        Log.v("networkType", networkType)

        // ------! 관련 관절, 근육 recyclerview 시작 !------
        val fullMuscleList = pvm.exerciseData?.relatedMuscle?.replace("(", ", ")
            ?.replace(")", "")
            ?.split(", ")
            ?.toMutableList()
//        Log.v("fullMuscleList", "$fullMuscleList")
        val muscleAdapter = StringRVAdapter(this@PlayThumbnailDialogFragment, fullMuscleList, null,"muscle", evm)
        binding.rvPTMuscle.adapter = muscleAdapter
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPTMuscle.layoutManager = layoutManager
        // ------! 관련 관절, 근육 recyclerview 끝 !------

        // -----! 하단 운동 시작 버튼 시작 !-----
        binding.btnPTDPlay.setOnSingleClickListener {
            when (networkType) {
                "WIFI", "ETERNET" -> {
                    clickedPlayButton()
                }
                else -> {
                    MaterialAlertDialogBuilder(requireContext(), R.style.ThemeOverlay_App_MaterialAlertDialog).apply {
                        setTitle("데이터 사용 알림")
                        setMessage("셀룰러 네트워크 데이터를 사용하면 추가 요금이 부과될 수 있습니다.")
                        setPositiveButton("재생") { _, _ ->
                            clickedPlayButton()
                        }
                        setNegativeButton("취소") { _, _ ->

                        }
                    }.show()
                }
            }
        } // ------! 하단 운동 시작 버튼 끝 !------

        binding.ibtnPTDExit.setOnSingleClickListener {
            dismiss()
        }
        // ------! 관련 운동 횡 rv 시작 !------
        lifecycleScope.launch {
            val responseArrayList = evm.allExercises
            try {
                val verticalDataList = responseArrayList?.filter {
                    it.relatedJoint?.contains(pvm.exerciseData?.relatedJoint?.split(", ")?.get(0) ?: "") == true
                }
                val shuffledList = verticalDataList?.shuffled()?.take(10.coerceAtMost(verticalDataList.size))?.toMutableList()
                val adapter = ExerciseRVAdapter(this@PlayThumbnailDialogFragment, shuffledList, null, null,  null,"PTD")
                binding.rvPTn.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.rvPTn.layoutManager = linearLayoutManager
            } catch (e: IndexOutOfBoundsException) {
                Log.e("PlayIndex", "${e.message}")
            } catch (e: IllegalArgumentException) {
                Log.e("PlayIllegal", "${e.message}")
            } catch (e: IllegalStateException) {
                Log.e("PlayIllegal", "${e.message}")
            }catch (e: NullPointerException) {
                Log.e("PlayNull", "${e.message}")
            } catch (e: java.lang.Exception) {
                Log.e("PlayException", "${e.message}")
            }
        }

        // ------! 관련 운동 횡 rv 끝 !------

        // ------# share #------
        binding.ibtnPTDShare.setOnSingleClickListener {
            val url = "https://tangopluscompany.github.io/deep-link-redirect/#/1?exercise=${pvm.exerciseData?.exerciseId}".toUri()
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_TEXT, url.toString())
            intent.type = "text/plain" // 공유할 데이터의 타입을 설정 (텍스트)
            startActivity(Intent.createChooser(intent, "공유하기")) // 공유할 앱을 선택할 수 있도록 Chooser 추가
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        simpleExoPlayer?.playWhenReady = true
    }

    private fun clickedPlayButton() {
        val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
        intent.putExtra("video_url", videoUrl)
        intent.putExtra("exercise_id", pvm.exerciseData?.exerciseId)
        intent.putExtra("total_duration", pvm.exerciseData?.duration?.toInt())

        when (pvm.isProgram) {
            true -> {
                intent.putExtra("isEVP", false)
                intent.putExtra("isProgram", true)
                intent.putExtra("uvpSn", pvm.uvpSn)
                intent.putExtra("current_position", progressVm.currentProgresses.find { it.uvpSn == pvm.uvpSn }?.cycleProgress?.toLong())
                intent.putExtra("currentWeek", progressVm.currentWeek + 1)
                intent.putExtra("currentSeq", progressVm.currentSequence + 1)
            }
            false -> {
                intent.putExtra("isEVP", true)
                intent.putExtra("isProgram", false)
                intent.putExtra("uvpSn", 0)
            }
        }
        startActivityForResult(intent, 8080)

        // ------! 운동 하나 전부 다 보고 나서 feedback한개만 켜지게 !------
        pvm.isDialogShown.value = false
        val frequencyLength = pvm.exerciseData?.exerciseFrequency
        if (frequencyLength != null) {
            if (frequencyLength.length >= 3) {
                setNotificationAlarm("Tango Q", "최근에 하신 스트레칭은 \n저녁에 하시면 효과가 더 좋답니다!", 19)
                Log.v("notification 완료", "Success make plan to send notification Alarm")
            }
        }
    }

    // ------# 일시중지 #------
    override fun onStop() {
        super.onStop()
        simpleExoPlayer?.stop()
        simpleExoPlayer?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer?.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("playbackPosition", simpleExoPlayer?.currentPosition ?: 0L)
        outState.putInt("currentWindow", simpleExoPlayer?.currentWindowIndex ?: 0)
        outState.putBoolean("playWhenReady", simpleExoPlayer?.playWhenReady ?: true)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 8080 && resultCode == Activity.RESULT_OK) {
            val currentPosition = data?.getLongExtra("current_position", 0)
            videoUrl = data?.getStringExtra("video_url").toString()
            if (currentPosition != null) {
                playbackPosition = currentPosition
            }
        }
    }
//매일 아침, 저녁 잠자기 전에 실시
    @SuppressLint("ScheduleExactAlarm")
    private fun setNotificationAlarm(title:String, text: String, hour: Int) {
        val alarmManager = requireActivity().getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // 오늘 저녁 8시의 시간을 설정
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    if (calendar.timeInMillis <= System.currentTimeMillis()) {
        calendar.add(Calendar.DAY_OF_MONTH, 1)
    }
    val requestCode = System.currentTimeMillis().toInt()

    val intent = Intent(requireContext(), AlarmReceiver::class.java).apply {
        putExtra("title", title)
        putExtra("text", text)
    }
    val pendingIntent = PendingIntent.getBroadcast(requireContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE)
    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)

//    Log.v("현재 시간", "${calendar.time}, intent: ${intent.getStringExtra("title")}, ${intent.getStringExtra("text")}")
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dialogCloseListener?.onDialogClose()  // Dialog가 닫힐 때 콜백 호출
    }
}
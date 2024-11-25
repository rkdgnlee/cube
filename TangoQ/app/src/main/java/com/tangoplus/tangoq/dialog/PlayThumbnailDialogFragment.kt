package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.tangoplus.tangoq.PlayFullScreenActivity
import com.tangoplus.tangoq.PlaySkeletonActivity
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.StringRVAdapter
import com.tangoplus.tangoq.broadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.function.PreferencesManager
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.viewmodel.ExerciseViewModel

import com.tangoplus.tangoq.databinding.FragmentPlayThumbnailDialogBinding
import com.tangoplus.tangoq.mediapipe.MathHelpers.isTablet
import com.tangoplus.tangoq.`object`.Singleton_t_user
import com.tangoplus.tangoq.viewmodel.PlayViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

class PlayThumbnailDialogFragment : DialogFragment() {
    lateinit var binding : FragmentPlayThumbnailDialogBinding
    private var videoUrl = "http://gym.tangostar.co.kr/data/contents/videos/걷기.mp4"
    val evm: ExerciseViewModel by activityViewModels()
    val pvm: PlayViewModel by activityViewModels()
    private var simpleExoPlayer: SimpleExoPlayer? = null
    private var playbackPosition = 0L
    private lateinit var prefs : PreferencesManager

    interface DialogCloseListener {
        fun onDialogClose()
    }

    private var dialogCloseListener: DialogCloseListener? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPlayThumbnailDialogBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val userJson = Singleton_t_user.getInstance(requireContext()).jsonObject
        val bundle = arguments
        val exerciseData = bundle?.getParcelable<ExerciseVO>("ExerciseUnit")

        // ------# like 있는지 판단 #------
        prefs = PreferencesManager(requireContext())
        var isLike = false
        if (prefs.existLike(exerciseData?.exerciseId.toString())) {
            isLike = true
            binding.ibtnPTDLike.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_like_enabled))
        }

        binding.ibtnPTDLike.setOnClickListener {
            if (isLike) {
                prefs.deleteLike(exerciseData?.exerciseId.toString())
                binding.ibtnPTDLike.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_like_disabled))
                isLike = false
            } else {
                prefs.storeLike(exerciseData?.exerciseId.toString())
                binding.ibtnPTDLike.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_like_enabled))
                isLike = true
            }
        }

        // -----! 각 설명들 textView에 넣기 !-----
        videoUrl = exerciseData?.videoFilepath.toString()
        Log.v("videoUrl", "videoUrl: ${videoUrl}, exerciseName: ${exerciseData?.exerciseName}")
        binding.tvPTDName.text = exerciseData?.exerciseName.toString()
        binding.tvPTDRelatedJoint.text = exerciseData?.relatedJoint.toString()

        binding.tvPTDTime.text = "${exerciseData?.videoDuration} 초"
        binding.tvPTDStage.text = exerciseData?.exerciseStage
        binding.tvPTDFrequency.text = exerciseData?.exerciseFrequency.toString()
        binding.tvPTRelatedSymptom.text = exerciseData?.relatedSymptom
//        binding.tvPTDInitialPosture.text = exerciseData?.exerciseInitialPosture.toString()
        binding.tvPTDMethod.text = exerciseData?.exerciseMethod.toString()
        binding.tvPTDCaution.text = exerciseData?.exerciseCaution.toString()
        binding.tvPTDRelatedMuscle.text = exerciseData?.relatedMuscle.toString()
//        binding.tvPTDIntensity.text = exerciseData?.exerciseIntensity.toString()

//        playbackPosition = intent.getLongExtra("current_position", 0L)
        initPlayer()

        // ------! 관련 관절, 근육 recyclerview 시작 !------
        val fullmuscleList = exerciseData?.relatedMuscle?.replace("(", ", ")
            ?.replace(")", "")
            ?.split(", ")
            ?.toMutableList()

        val displayMuscleList = fullmuscleList?.chunked(2)
            ?.map { it.first() }
            ?.toMutableList()

        val muscleAdapter = StringRVAdapter(this@PlayThumbnailDialogFragment, displayMuscleList, "muscle", evm)
        binding.rvPTMuscle.adapter = muscleAdapter
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPTMuscle.layoutManager = layoutManager
        // ------! 관련 관절, 근육 recyclerview 끝 !------

        // -----! 하단 운동 시작 버튼 시작 !-----
        binding.btnPTDPlay.setOnClickListener {
            val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
            intent.putExtra("video_url", videoUrl)
            intent.putExtra("exercise_id", exerciseData?.exerciseId)
            intent.putExtra("total_duration", exerciseData?.videoDuration?.toInt())
            startActivityForResult(intent, 8080)

            // ------! 운동 하나 전부 다 보고 나서 feedback한개만 켜지게 !------
            pvm.isDialogShown.value = false
            val frequencyLength = exerciseData?.exerciseFrequency
            if (frequencyLength != null) {
                if (frequencyLength.length >= 3) {
                    setNotificationAlarm("Tango Q", "최근에 하신 스트레칭은 \n저녁에 하시면 효과가 더 좋답니다!", 19)
                    Log.v("notification 완료", "Success make plan to send notification Alarm")
                }
            }

        } // ------! 하단 운동 시작 버튼 끝 !------

//        // ------# 전체화면 구현 로직 시작 #------
        val exitButton = binding.pvPTD.findViewById<ImageButton>(R.id.exo_exit)
        exitButton.setOnClickListener {
            dismiss()

        }

        val exoPlay = binding.pvPTD.findViewById<ImageButton>(R.id.btnPlay)
        val exoPause = binding.pvPTD.findViewById<ImageButton>(R.id.btnPause)
        exoPause?.setOnClickListener {
            if (simpleExoPlayer?.isPlaying == true) {
                simpleExoPlayer?.pause()
                exoPause.visibility = View.GONE
                exoPlay.visibility = View.VISIBLE
            }
        }
        exoPlay?.setOnClickListener {
            if (simpleExoPlayer?.isPlaying == false) {
                simpleExoPlayer?.play()
                exoPause.visibility = View.VISIBLE
                exoPlay.visibility = View.GONE
            }
        }
        // ------! 앞으로 감기 뒤로 감기 시작 !------
        val replay5 = binding.pvPTD.findViewById<ImageButton>(R.id.exo_replay_5)
        val forward5 = binding.pvPTD.findViewById<ImageButton>(R.id.exo_forward_5)
        replay5.setOnClickListener {
            val replayPosition = simpleExoPlayer?.currentPosition?.minus(5000)
            if (replayPosition != null) {
                simpleExoPlayer?.seekTo((if (replayPosition < 0) 0 else replayPosition))
            }
        }
        forward5.setOnClickListener {
            val forwardPosition = simpleExoPlayer?.currentPosition?.plus(5000)
            if (forwardPosition != null) {
                if (forwardPosition < (simpleExoPlayer?.duration?.minus(5000) ?: 0)) {
                    simpleExoPlayer?.seekTo(forwardPosition)
                } else {
                    simpleExoPlayer?.pause()
                }
            }
        } // ------! 앞으로 감기 뒤로 감기 끝 !------

        // ------! 관련 운동 횡 rv 시작 !------
        lifecycleScope.launch {
            val responseArrayList = evm.allExercises
            try {
                val verticalDataList = responseArrayList.filter {
                    it.relatedJoint?.contains(exerciseData?.relatedJoint?.split(", ")?.get(0) ?: "") == true
                }.subList(0, 10).toMutableList()
                val adapter = ExerciseRVAdapter(this@PlayThumbnailDialogFragment, verticalDataList, null, null, null,"recommend")
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

        // ------! ai코칭 시작 !------
        if (exerciseData?.exerciseId in listOf("74", "129", "133", "134", "171", "197", "202") ) {
            binding.btnPTDAIPlay.visibility = View.VISIBLE
        } else {
            binding.btnPTDAIPlay.visibility = View.GONE
        }
        binding.btnPTDAIPlay.setOnClickListener {

            if (isTablet(requireContext())) {
                val intent = Intent(requireContext(), PlaySkeletonActivity::class.java)

                val exerciseIds = mutableListOf(exerciseData?.exerciseId)
                val videoUrls = mutableListOf(videoUrl)
                intent.putStringArrayListExtra("exercise_ids", ArrayList(exerciseIds))
                intent.putStringArrayListExtra("video_urls", ArrayList(videoUrls))
                intent.putExtra("total_time", exerciseData?.videoDuration?.toInt())
                startActivity(intent)
            } else {
                context.let { Toast.makeText(it, "태블릿 기기에서 운동을 추천드립니다", Toast.LENGTH_SHORT).show() }
            }


        }
        // ------! ai코칭 끝 !------

        // ------# share #------
        binding.ibtnPTDShare.setOnClickListener {
            val url = Uri.parse("https://tangopluscompany.github.io/deep-link-redirect/#/1?exercise=${exerciseData?.exerciseId}")
            val intent = Intent(Intent.ACTION_SEND)
            intent.putExtra(Intent.EXTRA_TEXT, url.toString())
            intent.type = "text/plain" // 공유할 데이터의 타입을 설정 (텍스트)
            startActivity(Intent.createChooser(intent, "공유하기")) // 공유할 앱을 선택할 수 있도록 Chooser 추가
        }
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        simpleExoPlayer?.playWhenReady = true
    }

    private fun initPlayer(){
        simpleExoPlayer = SimpleExoPlayer.Builder(requireContext()).build()
        binding.pvPTD.player = simpleExoPlayer
        buildMediaSource().let {
            simpleExoPlayer?.prepare(it)
        }
        simpleExoPlayer?.seekTo(playbackPosition)
    }

    private fun buildMediaSource() : MediaSource {
        val dataSourceFactory = DefaultDataSourceFactory(requireContext(), "sample")
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoUrl))

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
            initPlayer()
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

    Log.v("현재 시간", "${calendar.time}, intent: ${intent.getStringExtra("title")}, ${intent.getStringExtra("text")}")
    }

    fun setDialogCloseListener(listener: DialogCloseListener) {
        this.dialogCloseListener = listener
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dialogCloseListener?.onDialogClose()  // Dialog가 닫힐 때 콜백 호출
    }
}
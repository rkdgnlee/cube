package com.tangoplus.tangoq.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
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
import com.tangoplus.tangoq.adapter.ExerciseRVAdapter
import com.tangoplus.tangoq.`object`.NetworkExercise
import com.tangoplus.tangoq.R
import com.tangoplus.tangoq.adapter.MuscleRVAdpater
import com.tangoplus.tangoq.broadcastReceiver.AlarmReceiver
import com.tangoplus.tangoq.db.PreferencesManager
import com.tangoplus.tangoq.data.ExerciseVO
import com.tangoplus.tangoq.data.ExerciseViewModel
import com.tangoplus.tangoq.data.UserViewModel

import com.tangoplus.tangoq.databinding.FragmentPlayThumbnailDialogBinding
import com.tangoplus.tangoq.`object`.DeepLinkUtil
import com.tangoplus.tangoq.`object`.Singleton_t_history
import com.tangoplus.tangoq.`object`.Singleton_t_user
import kotlinx.coroutines.launch
import java.util.Calendar

class PlayThumbnailDialogFragment : DialogFragment() {
    lateinit var binding : FragmentPlayThumbnailDialogBinding
    private var videoUrl = "http://gym.tangostar.co.kr/data/contents/videos/걷기.mp4"
    val viewModel: ExerciseViewModel by activityViewModels()
    val uViewModel : UserViewModel by activityViewModels()
    private var simpleExoPlayer: SimpleExoPlayer? = null
//    private var playWhenReady = true
//    private var currentWindow = 0
    private var playbackPosition = 0L
    private lateinit var singletonInstance: Singleton_t_history

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
        singletonInstance = Singleton_t_history.getInstance(requireContext())
        val bundle = arguments
        val exerciseData = bundle?.getParcelable<ExerciseVO>("ExerciseUnit")

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
        val displayMuscleList = fullmuscleList?.chunked(2)?.map { it.first() }?.toMutableList()



        Log.v("muscleList", "$displayMuscleList")
        val muscleAdapter = MuscleRVAdpater(this@PlayThumbnailDialogFragment, displayMuscleList)
        binding.rvPTMuscle.adapter = muscleAdapter
        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvPTMuscle.layoutManager = layoutManager
        // ------! 관련 관절, 근육 recyclerview 끝 !------

        // ------! 공유 버튼 시작 !------
        binding.ibtnPTDShare.setOnClickListener {
            if (exerciseData != null) {
                shareContent(exerciseData)
            }
        } // ------! 공유 버튼 끝 !------




        // -----! 하단 운동 시작 버튼 시작 !-----
        binding.btnPTDPlay.setOnClickListener {
            val intent = Intent(requireContext(), PlayFullScreenActivity::class.java)
            intent.putExtra("exercise_id", exerciseData?.exerciseId)
            intent.putExtra("video_url", videoUrl)
            intent.putExtra("total_time", exerciseData?.videoDuration?.toInt())
            startActivityForResult(intent, 8080)

            // ------! 운동 하나 전부 다 보고 나서 feedback한개만 켜지게 !------
            viewModel.isDialogShown.value = false

            // ------! 오늘의 운동 갯수에 넣기 시작 !------
            val prefsManager = PreferencesManager(requireContext())
            val userEmail = userJson?.optString("user_email").toString()
            if (prefsManager.getStoredInt(userEmail) < 5) {
                prefsManager.incrementStoredInt(userEmail)
                uViewModel.dailyProgress.value = prefsManager.getStoredInt(userEmail)
            } // ------! 오늘의 운동 갯수에 넣기  끝 !------

            if (exerciseData?.exerciseFrequency?.length!! >= 3) {
                setNotificationAlarm("Tango Q", "최근에 하신 스트레칭은 \n저녁에 하시면 효과가 더 좋답니다!", 20)
                Log.v("notification 완료", "Success make plan to send notification Alarm")
            }
        } // ------! 하단 운동 시작 버튼 끝 !------

//        // ------! 전체화면 구현 로직 시작 !------
        val exitButton = binding.pvPTD.findViewById<ImageButton>(R.id.exo_exit)
        exitButton.setOnClickListener {
            dismiss()
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
                if (forwardPosition < simpleExoPlayer?.duration?.minus(5000)!!) {
                    simpleExoPlayer?.seekTo(forwardPosition)
                } else {
                    simpleExoPlayer!!.pause()
                }
            }
        } // ------! 앞으로 감기 뒤로 감기 끝 !------





        // ------! 관련 운동 횡 rv 시작 !------
        lifecycleScope.launch {
            val responseArrayList =
                NetworkExercise.fetchExerciseJson(getString(R.string.IP_ADDRESS_t_exercise_description))
            try {
                // 전체데이터에서 현재 데이터(exerciseData)와 비교함.
                val verticalDataList = responseArrayList.filter { it.relatedJoint!!.contains(
                    exerciseData?.relatedJoint!!.split(", ")[0])}.toMutableList()
                val adapter = ExerciseRVAdapter(this@PlayThumbnailDialogFragment, verticalDataList, singletonInstance.viewingHistory?.toList() ?: listOf(),
                    "recommend")
                binding.rvPTn.adapter = adapter
                val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
                binding.rvPTn.layoutManager = linearLayoutManager
            } catch (e: Exception) {
                Log.e(ContentValues.TAG, "Error storing exercises", e)
            }
        }

        // ------! 관련 운동 횡 rv 끝 !------

    }
    // ------! 딥링크로 앱 공유 시작 !------

    // 딥링크 생성
    fun generateDeepLink(exercise: ExerciseVO): String {
        val encodedData = DeepLinkUtil.encodeExercise(exercise)
        return "tangoq://tangoq.com/content/$encodedData"
    }

    // 딥링크 공유
    fun shareContent(content: ExerciseVO) {
        val deepLink = generateDeepLink(content)
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, deepLink)
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }
    // ------! 딥링크로 앱 공유 끝 !------

    override fun onResume() {
        super.onResume()
        // full Screen code
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
    // 일시중지
    override fun onStop() {
        super.onStop()
        simpleExoPlayer?.stop()
        simpleExoPlayer?.playWhenReady = false
    }

    override fun onDestroy() {
        super.onDestroy()
        simpleExoPlayer?.release()
//        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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
            playbackPosition = currentPosition!!
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
}
package com.tangoplus.tangoq.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray

class PlayViewModel : ViewModel() {

    var isResume =  false
    private var playbackPosition = 0L
    private var windowIndex = 0
    private var playWhenReady = true
    var videoUrl: String? = null

    // playFullScreen에서 url, ids 담기
    var baseUrls = arrayListOf<String>()
    var sns: MutableList<String>? = null
    var uvpSns : MutableList<String>? = null
    var weekNumber = 0
    var cycle = 0

    var simpleExoPlayer: SimpleExoPlayer? = null
    var currentMediaSourceIndex = 0
    var currentVideoDuration = 0L
    var dynamicJa = JSONArray()
    fun savePlayerState(player: SimpleExoPlayer, url: String = "") {
        playbackPosition = player.currentPosition
        playWhenReady = player.playWhenReady
        windowIndex = player.currentWindowIndex
        videoUrl = url
    }

    fun getPlaybackPosition() = playbackPosition
    fun getPlayWhenReady() = playWhenReady
    fun getWindowIndex() = windowIndex
    fun setPlaybackPosition(position : Long) { playbackPosition = position }
    fun setWindowIndex(index: Int) { windowIndex = index }

    // 재생목록 재생 후 피드백 담기
    var totalProgressDuration = 0
    var exerciseLog = Triple(0, 0, 0) // 진행시간, 갯수, 총 시간.
    var isDialogShown = MutableLiveData(false)
    var isEVP = false

    // Play Thumbnail
    var isProgram = false
    var uvpSn = 0
    var uvpSnCurrentPosition = 0


    private var leftPlayBackPosition  = 0L
    private var rightPlayBackPosition = 0L
    fun setLeftPlaybackPosition(position : Long) { leftPlayBackPosition = position }
    fun setRightPlaybackPosition(position: Long) { rightPlayBackPosition = position }
    fun getLeftPlaybackPosition() = leftPlayBackPosition
    fun getRightPlaybackPosition() = rightPlayBackPosition

    override fun onCleared() {
        super.onCleared()
        simpleExoPlayer?.release()
        simpleExoPlayer = null
    }
}
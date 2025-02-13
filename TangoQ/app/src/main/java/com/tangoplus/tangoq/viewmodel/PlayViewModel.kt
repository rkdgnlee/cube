package com.tangoplus.tangoq.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.android.exoplayer2.SimpleExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow

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
    var isUnit = false


}
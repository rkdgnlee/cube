package com.tangoplus.tangoq.data

data class EpisodeVO(
    val programId: String?,
    val isFinish : Boolean,
    val doingExercises: MutableList<HistoryUnitVO>,
) // 에피소드는 회차를 말
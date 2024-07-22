package com.tangoplus.tangoq.listener

import com.tangoplus.tangoq.data.ExerciseVO

interface OnExerciseAddClickListener {
    fun onExerciseAddClick(exerciseVO: ExerciseVO, select: Boolean)
}
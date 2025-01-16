package com.tangoplus.tangoq.vo

import android.os.Parcel
import android.os.Parcelable

data class ProgramVO(
    var programSn :Int = 0,
    var programName : String? = "",
    var programStage : String? = "",
    var programCount : String? = "",
    var programTime : Int = 0,
    var programFrequency : Int = 0,
    var programWeek : Int = 0,
    var exerciseTime : Int = 0,
    var exercises : MutableList<ExerciseVO>? = mutableListOf()
): Parcelable {
    constructor(parcel: Parcel) : this(
        programSn = parcel.readInt(),
        programName = parcel.readString(),
        programStage = parcel.readString(),
        programCount = parcel.readString(),
        programTime = parcel.readInt(),
        programFrequency = parcel.readInt(),
        programWeek = parcel.readInt(),
        exerciseTime = parcel.readInt(),
        exercises = mutableListOf<ExerciseVO>().apply {
            parcel.readList(this, ExerciseVO::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(programSn)
        parcel.writeString(programName)
        parcel.writeString(programStage)
        parcel.writeString(programCount)
        parcel.writeInt(programTime)
        parcel.writeInt(programFrequency)
        parcel.writeInt(programWeek)
        parcel.writeInt(exerciseTime)
        parcel.writeList(exercises)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ProgramVO> {
        override fun createFromParcel(parcel: Parcel): ProgramVO {
            return ProgramVO(parcel)
        }

        override fun newArray(size: Int): Array<ProgramVO?> {
            return arrayOfNulls(size)
        }
    }
}
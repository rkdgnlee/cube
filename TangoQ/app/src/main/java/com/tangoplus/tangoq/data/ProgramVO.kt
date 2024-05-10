package com.tangoplus.tangoq.data

import android.os.Parcel
import android.os.Parcelable

data class ProgramVO(
    var programImageUrl : String? = "",
    var programName : String? = "",
    var programTime : String? = "",
    var programStage : String? = "",
    var programCount : String? = "",
    var exercises : MutableList<ExerciseVO>?
): Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createTypedArrayList(ExerciseVO.CREATOR)
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(programImageUrl)
        dest.writeString(programName)
        dest.writeString(programTime)
        dest.writeString(programStage)
        dest.writeString(programCount)
        dest.writeTypedList(exercises)
    }
    companion object CREATOR: Parcelable.Creator<ProgramVO> {
            override fun createFromParcel(parcel: Parcel): ProgramVO {
                return ProgramVO(parcel)
            }

            override fun newArray(size: Int): Array<ProgramVO?> {
                return arrayOfNulls(size)
            }
    }
}
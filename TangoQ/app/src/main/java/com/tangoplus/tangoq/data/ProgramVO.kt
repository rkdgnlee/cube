package com.tangoplus.tangoq.data

import android.os.Parcel
import android.os.Parcelable

data class ProgramVO(
    var programSn :Int = 0,
    var imgThumbnails : MutableList<String>?,
    var programName : String? = "",
    var programTime : Int = 0,
    var programStage : String? = "",
    var programCount : String? = "",
    var exercises : MutableList<ExerciseVO>?
): Parcelable {

    constructor(parcel: Parcel): this(
        parcel.readInt(),
        parcel.createStringArrayList(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.createTypedArrayList(ExerciseVO.CREATOR)
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(programSn)
        dest.writeStringList(imgThumbnails)
        dest.writeString(programName)
        dest.writeInt(programTime)
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
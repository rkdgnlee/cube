package com.example.mhg.VO

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PickItemVO (
    val pickName : String? = "",
    val pickExplainTitle : String? = "",
    val pickExplain : String? = "",
    val pickDisclosure: String? = "",
    val exercises : MutableList<ExerciseVO>?
) : Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createTypedArrayList(ExerciseVO.CREATOR)
    )
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(pickName)
        dest.writeString(pickExplainTitle)
        dest.writeString(pickExplain)
        dest.writeString(pickDisclosure)
        dest.writeTypedList(exercises)
    }
    companion object CREATOR : Parcelable.Creator<PickItemVO> {
        override fun createFromParcel(parcel: Parcel): PickItemVO {
            return PickItemVO(parcel)
        }

        override fun newArray(size: Int): Array<PickItemVO?> {
            return arrayOfNulls(size)
        }
    }
}
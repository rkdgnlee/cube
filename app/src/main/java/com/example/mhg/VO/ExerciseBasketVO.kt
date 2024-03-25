package com.example.mhg.VO

import android.os.Parcel
import android.os.Parcelable

data class ExerciseBasketVO (
    val basketName : String? = "",
    val basketExplainTitle : String? = "",
    val basketExplain : String? = "",
    val basketDisclosure: String? = "",
    val exercises : List<ExerciseItemVO>?
) : Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createTypedArrayList(ExerciseItemVO.CREATOR)
    )
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(basketName)
        dest.writeString(basketExplainTitle)
        dest.writeString(basketExplain)
        dest.writeString(basketDisclosure)
        dest.writeTypedList(exercises)
    }
    companion object CREATOR : Parcelable.Creator<ExerciseBasketVO> {
        override fun createFromParcel(parcel: Parcel): ExerciseBasketVO {
            return ExerciseBasketVO(parcel)
        }

        override fun newArray(size: Int): Array<ExerciseBasketVO?> {
            return arrayOfNulls(size)
        }
    }
}
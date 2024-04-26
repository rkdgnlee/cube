package com.example.mhg.VO

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PickItemVO (
    var favoriteSn : Int = 0,
    var favoriteName : String? = "",
    var favoriteExplainTitle : String? = "",
    var favoriteExplain : String? = "",
    val favoriteDisclosure: String? = "",
    var exercises : MutableList<ExerciseVO>?
) : Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createTypedArrayList(ExerciseVO.CREATOR)
    )
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(favoriteSn)
        dest.writeString(favoriteName)
        dest.writeString(favoriteExplainTitle)
        dest.writeString(favoriteExplain)
        dest.writeString(favoriteDisclosure)
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
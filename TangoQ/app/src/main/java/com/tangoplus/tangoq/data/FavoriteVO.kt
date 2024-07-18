package com.tangoplus.tangoq.data

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FavoriteVO (
    var imgThumbnails : MutableList<String>?,
    var favoriteSn : Int = 0,
    var favoriteName : String? = "",
    var favoriteRegDate : String? = "",
    var favoriteTotalTime : String? = "",
    var favoriteTotalCount : String? = "",
    var exercises : MutableList<ExerciseVO>?
) : Parcelable {
    constructor(parcel: Parcel): this(
        parcel.createStringArrayList(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createTypedArrayList(ExerciseVO.CREATOR)
    )
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStringList(imgThumbnails)
        dest.writeInt(favoriteSn)
        dest.writeString(favoriteName)
        dest.writeString(favoriteRegDate)
        dest.writeString(favoriteTotalTime)
        dest.writeString(favoriteTotalCount)
        dest.writeTypedList(exercises)
    }
    companion object CREATOR : Parcelable.Creator<FavoriteVO> {
        override fun createFromParcel(parcel: Parcel): FavoriteVO {
            return FavoriteVO(parcel)
        }

        override fun newArray(size: Int): Array<FavoriteVO?> {
            return arrayOfNulls(size)
        }
    }
}
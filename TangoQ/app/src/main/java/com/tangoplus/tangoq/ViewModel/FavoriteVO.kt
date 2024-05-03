package com.tangoplus.tangoq.ViewModel

import android.os.Parcel
import android.os.Parcelable

data class FavoriteVO (
    var imgThumbnailList : MutableList<String>?,
    val sn : Int,
    var name: String?,
    val regDate : String?,
    var time: String?,
    var count: String?
) : Parcelable {
    constructor(parcel: Parcel) : this (
        parcel.createStringArrayList(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
    )

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStringList(imgThumbnailList)
        dest.writeInt(sn)
        dest.writeString(name)
        dest.writeString(regDate)
        dest.writeString(time)
        dest.writeString(count)

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
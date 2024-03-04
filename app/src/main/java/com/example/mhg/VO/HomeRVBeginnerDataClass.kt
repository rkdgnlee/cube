package com.example.mhg.VO

import android.os.Parcel
import android.os.Parcelable

data class HomeRVBeginnerDataClass(
    var imgUrl: String? = "",
    var name: String?,
    var duration: Int? = 0,
    val uri: String? = null,
    val explanation: String? = ""
) : Parcelable {

//담는거
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readValue(Int::class.java.classLoader) as? Int,
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(imgUrl)
        dest.writeString(name)
        duration?.let { dest.writeInt(it) }
        dest.writeString(uri)
        dest.writeString(explanation)

    }
// 불러오는 거
    companion object CREATOR : Parcelable.Creator<HomeRVBeginnerDataClass> {
        override fun createFromParcel(parcel: Parcel): HomeRVBeginnerDataClass {
            return HomeRVBeginnerDataClass(parcel)
        }

        override fun newArray(size: Int): Array<HomeRVBeginnerDataClass?> {
            return arrayOfNulls(size)
        }
    }

}
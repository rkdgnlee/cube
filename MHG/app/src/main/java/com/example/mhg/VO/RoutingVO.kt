package com.example.mhg.VO

import android.os.Parcel
import android.os.Parcelable

data class RoutingVO (
    val title: String = "",
    val route: String = ""
): Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString(),
    ) {

    }
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(title)
        dest.writeString(route)

    }
    companion object CREATOR : Parcelable.Creator<RoutingVO> {
        override fun createFromParcel(source: Parcel): RoutingVO {
            return RoutingVO(source)
        }

        override fun newArray(size: Int): Array<RoutingVO?> {
            return arrayOfNulls(size)
        }
    }

}
package com.example.mhg.VO

import android.os.Parcel
import android.os.Parcelable

data class HomeRVBeginnerDataClass(
    var imgUrl: String? = "",
    var exerciseName: String?,
    var exerciseDescription: String? = "",
    var relatedJoint: String? = "",
    var relatedMuscle: String?,
    var relatedSymptom: String?,
    var exerciseStage: String? = "",
    var exerciseFequency: String? = "",
    var exerciseIntensity: String? = "",
    var exerciseInitialPosture: String? = "",
    var exerciseMethod: String? = "",
    var exerciseCaution: String? = "",
    var videoAlternativeName: String? = "",
    var videoFilepath: String? = "",
    var videoTime: String? = ""

) : Parcelable {

//담는거
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
    )

    override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
            dest.writeString(imgUrl)
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
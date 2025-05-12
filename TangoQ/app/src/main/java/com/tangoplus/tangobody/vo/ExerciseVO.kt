package com.tangoplus.tangobody.vo

import android.annotation.SuppressLint
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExerciseVO(
    var exerciseId : String? = "",
    var exerciseName: String? = "",
    var exerciseTypeId: String? = "",
    var exerciseTypeName: String? = "",
    var exerciseCategoryId: String? = "",
    var exerciseCategoryName: String? = "",
    var relatedJoint: String? = "",
    var relatedMuscle: String? = "",
    var relatedSymptom: String? = "",
    var exerciseStage: String? = "",
    var exerciseFrequency: String? = "",
    var exerciseIntensity: String? = "",
    var exerciseInitialPosture: String? = "",
    var exerciseMethod: String? = "",
    var exerciseCaution: String? = "",
    var videoFilepath: String? = "",
    var duration: String? = "",
    var imageFilePath: String? = "",

    var select: Boolean = false
) : Parcelable {

    //담는거
    @SuppressLint("NewApi")
    @RequiresApi(Build.VERSION_CODES.Q)
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

        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readBoolean()
    )

    override fun describeContents(): Int = 0

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(exerciseId)
        dest.writeString(exerciseName)
        dest.writeString(exerciseTypeId)
        dest.writeString(exerciseTypeName)
        dest.writeString(exerciseCategoryId)
        dest.writeString(exerciseCategoryName)
        dest.writeString(relatedJoint)
        dest.writeString(relatedMuscle)
        dest.writeString(relatedSymptom)
        dest.writeString(exerciseStage)
        dest.writeString(exerciseFrequency)
        dest.writeString(exerciseIntensity)
        dest.writeString(exerciseInitialPosture)
        dest.writeString(exerciseMethod)
        dest.writeString(exerciseCaution)
        dest.writeString(videoFilepath)
        dest.writeString(duration)
        dest.writeString(imageFilePath)
        dest.writeBoolean(select)

    }
    // 불러오는 거
    companion object CREATOR : Parcelable.Creator<ExerciseVO> {
        @RequiresApi(Build.VERSION_CODES.Q)
        override fun createFromParcel(parcel: Parcel): ExerciseVO {
            return ExerciseVO(parcel)
        }

        override fun newArray(size: Int): Array<ExerciseVO?> {
            return arrayOfNulls(size)
        }
    }

}
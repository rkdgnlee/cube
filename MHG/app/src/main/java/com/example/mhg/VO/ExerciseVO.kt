package com.example.mhg.VO

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExerciseVO(
    var imgUrl: String? = "",
    var exerciseName: String? = "",
    var exerciseDescription: String? = "",
    var exerciseDescriptionId: Int = 0,
    var relatedJoint: String? = "",
    var relatedMuscle: String? = "",
    var relatedSymptom: String? = "",
    var exerciseStage: String? = "",
    var exerciseFequency: String? = "",
    var exerciseIntensity: String? = "",
    var exerciseInitialPosture: String? = "",
    var exerciseMethod: String? = "",
    var exerciseCaution: String? = "",
    var videoAlternativeName: String? = "",
    var videoFilepath: String? = "",
    var videoTime: String? = "",
    var exerciseTypeId: String? = "",
    var exerciseTypeName: String? = "",
    var quantity: Int = 0
) : Parcelable {

//담는거
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
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
        parcel.readInt()
    )

    override fun describeContents(): Int = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeString(imgUrl)
            dest.writeString(exerciseName)
            dest.writeString(exerciseDescription)
            dest.writeInt(exerciseDescriptionId)
            dest.writeString(relatedJoint)
            dest.writeString(relatedMuscle)
            dest.writeString(relatedSymptom)
            dest.writeString(exerciseStage)
            dest.writeString(exerciseFequency)
            dest.writeString(exerciseIntensity)
            dest.writeString(exerciseInitialPosture)
            dest.writeString(exerciseMethod)
            dest.writeString(exerciseCaution)
            dest.writeString(videoAlternativeName)
            dest.writeString(videoFilepath)
            dest.writeString(videoTime)
            dest.writeString(exerciseTypeId)
            dest.writeString(exerciseTypeName)
            dest.writeInt(quantity)

    }
// 불러오는 거
    companion object CREATOR : Parcelable.Creator<ExerciseVO> {
        override fun createFromParcel(parcel: Parcel): ExerciseVO {
            return ExerciseVO(parcel)
        }

        override fun newArray(size: Int): Array<ExerciseVO?> {
            return arrayOfNulls(size)
        }
    }

}
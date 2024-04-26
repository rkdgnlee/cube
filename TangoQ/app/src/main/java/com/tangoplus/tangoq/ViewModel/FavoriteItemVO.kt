package com.tangoplus.tangoq.ViewModel

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FavoriteItemVO (
    var favoriteSn : Int = 0,
    var favoriteName : String? = "",
    var favoriteExplain : String? = "",
    val favoriteDisclosure: String? = "",
    var exercises : MutableList<ExerciseVO>?
) : Parcelable {
    constructor(parcel: Parcel): this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.createTypedArrayList(ExerciseVO.CREATOR)
    )
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeInt(favoriteSn)
        dest.writeString(favoriteName)
        dest.writeString(favoriteExplain)
        dest.writeString(favoriteDisclosure)
        dest.writeTypedList(exercises)
    }
    companion object CREATOR : Parcelable.Creator<FavoriteItemVO> {
        override fun createFromParcel(parcel: Parcel): FavoriteItemVO {
            return FavoriteItemVO(parcel)
        }

        override fun newArray(size: Int): Array<FavoriteItemVO?> {
            return arrayOfNulls(size)
        }
    }
}
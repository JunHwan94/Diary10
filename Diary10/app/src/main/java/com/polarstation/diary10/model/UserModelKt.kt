package com.polarstation.diary10.model

import android.os.Parcel
import android.os.Parcelable

data class UserModelKt(val userName: String, val profileImageUrl: String, val uid: String, val hash: String, val comment: String = "", val pushToken: String = ""): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!) {
    }
    constructor(): this("", "", "", "")

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(userName)
        parcel.writeString(profileImageUrl)
        parcel.writeString(comment)
        parcel.writeString(uid)
        parcel.writeString(pushToken)
    }

    override fun describeContents(): Int = 0


    companion object CREATOR : Parcelable.Creator<UserModelKt> {
        override fun createFromParcel(parcel: Parcel): UserModelKt {
            return UserModelKt(parcel)
        }

        override fun newArray(size: Int): Array<UserModelKt?> {
            return arrayOfNulls(size)
        }
    }
}
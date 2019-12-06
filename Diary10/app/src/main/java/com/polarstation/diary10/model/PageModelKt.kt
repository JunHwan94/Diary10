package com.polarstation.diary10.model

import android.os.Parcel
import android.os.Parcelable

data class PageModelKt(val content: String, val imageUrl: String = "", val createTime: Long, val key: String = ""): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readLong(),
            parcel.readString()!!) {
    }

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(content)
        parcel.writeString(imageUrl)
        parcel.writeLong(createTime)
        parcel.writeString(key)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<PageModelKt> {
        override fun createFromParcel(parcel: Parcel): PageModelKt {
            return PageModelKt(parcel)
        }

        override fun newArray(size: Int): Array<PageModelKt?> {
            return arrayOfNulls(size)
        }
    }
}
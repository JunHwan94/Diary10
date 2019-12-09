package com.polarstation.diary10.model

import android.os.Parcel
import android.os.Parcelable

data class PageModel(val content: String, val imageUrl: String = "", val createTime: Long, val key: String = ""): Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readLong(),
            parcel.readString()!!) {
    }
    constructor(content: String, imageUrl: String, createTime: Long): this(content, imageUrl, createTime, "")
    constructor(): this("", "", -1L, "")

    override fun writeToParcel(parcel: Parcel, i: Int) {
        parcel.writeString(content)
        parcel.writeString(imageUrl)
        parcel.writeLong(createTime)
        parcel.writeString(key)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<PageModel> {
        override fun createFromParcel(parcel: Parcel): PageModel {
            return PageModel(parcel)
        }

        override fun newArray(size: Int): Array<PageModel?> {
            return arrayOfNulls(size)
        }
    }
}
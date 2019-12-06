package com.polarstation.diary10.model;

import android.os.Parcel;
import android.os.Parcelable;

public class PageModel implements Parcelable {
    private String content;
    private String imageUrl = "";
//    private String encodedString;
    private long createTime;
    private String key;

    public static class Builder{
        private String content;
        private String imageUrl;
//        private String encodedString;
        private long createTime;

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

//        public Builder setEncodedString(String encodedString){
//            this.encodedString = encodedString;
//            return this;
//        }

        public Builder setCreateTime(long createTime) {
            this.createTime = createTime;
            return this;
        }

        public PageModel build(){
            return new PageModel(this);
        }
    }
    public PageModel(){}

    private PageModel(Builder builder){
        content = builder.content;
        imageUrl = builder.imageUrl;
//        encodedString = builder.encodedString;
        createTime = builder.createTime;
    }

    private PageModel(Parcel parcel){
        content = parcel.readString();
        imageUrl = parcel.readString();
//        encodedString = parcel.readString();
        createTime = parcel.readLong();
        key = parcel.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){
        @Override
        public Object createFromParcel(Parcel parcel) {
            return new PageModel(parcel);
        }

        @Override
        public Object[] newArray(int size) {
            return new UserModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(content);
        parcel.writeString(imageUrl);
//        parcel.writeString(encodedString);
        parcel.writeLong(createTime);
        parcel.writeString(key);
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

//    public String getEncodedString(){
//        return encodedString;
//    }

    public long getCreateTime() {
        return createTime;
    }

    public String getKey(){
        return key;
    }

    public String toString(){
        return String.valueOf("content : " + content
//        + "\nencodedString : " + encodedString
        + "\ncreateTime : " + createTime);

    }
}

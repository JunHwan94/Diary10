package com.polarstation.diary10.model;

import android.os.Parcel;
import android.os.Parcelable;

public class UserModel implements Parcelable {
    private String userName;
    private String profileImageUrl = "";
    private String comment;
    private String uid;
    private String hash;
    private String pushToken;

    public static class Builder{
        private String userName;
        private String comment = "";
        private String uid;
        private String profileImageUrl;
        private String hash;

        public Builder setUserName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder setComment(String comment) {
            this.comment = comment;
            return this;
        }

        public Builder setUid(String uid) {
            this.uid = uid;
            return this;
        }

        public Builder setProfileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
            return this;
        }

        public Builder setHash(String hash){
            this.hash = hash;
            return this;
        }

        public UserModel build(){
            return new UserModel(this);
        }
    }

    private UserModel(Builder builder){
        userName = builder.userName;
        profileImageUrl = builder.profileImageUrl;
        comment = builder.comment;
        uid = builder.uid;
        hash = builder.hash;
    }

    public UserModel(){}

    private UserModel(Parcel parcel){
        userName = parcel.readString();
        profileImageUrl = parcel.readString();
        comment = parcel.readString();
        uid = parcel.readString();
        pushToken = parcel.readString();
        hash = parcel.readString();
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator(){
        @Override
        public Object createFromParcel(Parcel parcel) {
            return new UserModel(parcel);
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
        parcel.writeString(userName);
        parcel.writeString(profileImageUrl);
        parcel.writeString(comment);
        parcel.writeString(uid);
        parcel.writeString(pushToken);
    }

    public String getUserName() {
        return userName;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getComment() {
        return comment;
    }

    public String getUid() {
        return uid;
    }

    public String getPushToken(){
        return pushToken;
    }

    public String getHash(){
        return hash;
    }
}

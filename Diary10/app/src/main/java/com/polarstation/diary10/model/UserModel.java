package com.polarstation.diary10.model;

public class UserModel {
    private String userName;
    private String profileImageUrl;
    private String comment;
    private String uid;

    public static class Builder{
        private String userName;
        private String comment = "";
        private String uid;
        private String profileImageUrl;

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

        public UserModel build(){
            return new UserModel(this);
        }
    }

    public UserModel(){}

    private UserModel(Builder builder){
        userName = builder.userName;
        profileImageUrl = builder.profileImageUrl;
        comment = builder.comment;
        uid = builder.uid;
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
}

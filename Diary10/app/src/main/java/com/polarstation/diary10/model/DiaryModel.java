package com.polarstation.diary10.model;

import java.util.HashMap;
import java.util.Map;

public class DiaryModel {
    private String title;
    private String uid;
    private String coverImageUrl;
    private boolean isPrivate;
    private long createTime;
    private String key;
    private Map<String, PageModel> pages = new HashMap<>();

    public static class Builder{
        private String title;
        private String uid;
        private String coverImageUrl;
        private boolean isPrivate;
        private long createTime;

        public Builder setTitle(String title){
            this.title = title;
            return this;
        }

        public Builder setUid(String uid){
            this.uid = uid;
            return this;
        }

        public Builder setCoverImageUrl(String coverImageUrl){
            this.coverImageUrl = coverImageUrl;
            return this;
        }

        public Builder setIsPrivate(boolean isPrivate){
            this.isPrivate = isPrivate;
            return this;
        }

        public Builder setCreateTime(long createTime){
            this.createTime = createTime;
            return this;
        }

        public DiaryModel build(){
            return new DiaryModel(this);
        }
    }

    public DiaryModel(){}

    private DiaryModel(Builder builder){
        title = builder.title;
        uid = builder.uid;
        coverImageUrl = builder.coverImageUrl;
        isPrivate = builder.isPrivate;
        createTime = builder.createTime;
    }

    public String getTitle() {
        return title;
    }

    public String getUid() {
        return uid;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public long getCreateTime(){
        return createTime;
    }

    public String getKey(){
        return key;
    }

    public Map<String, PageModel> getPages() {
        return pages;
    }
}

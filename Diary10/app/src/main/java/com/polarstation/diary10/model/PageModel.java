package com.polarstation.diary10.model;

import java.util.HashMap;
import java.util.Map;

public class PageModel {
    private String content;
    private String imageUrl;
    private Object createTime;
    private Map<String,Object> likeUserMap = new HashMap<>();

    public static class Builder{
        private String content;
        private String imageUrl;
        private Object createTime;

        public Builder setContent(String content) {
            this.content = content;
            return this;
        }

        public Builder setImageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public Builder setCreateTime(Object createTime) {
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
        createTime = builder.createTime;
    }

    public String getContent() {
        return content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public Object getCreateTime() {
        return createTime;
    }
}

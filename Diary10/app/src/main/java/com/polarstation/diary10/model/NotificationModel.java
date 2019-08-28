package com.polarstation.diary10.model;

public class NotificationModel {
    private String to;
    private Notification notification = new Notification();
    private Data data = new Data();

    public NotificationModel(String to){
        this.to = to;
    }

    public Notification getNotification(){
        return notification;
    }

    public Data getData(){
        return data;
    }

    public static class Notification{
        private String title;
        private String text;

        public Notification(){
        }

        public void setTitle(String title){
            this.title = title;
        }

        public void setText(String text){
            this.text = text;
        }

        public String getTitle(){
            return title;
        }

        public String getText(){
            return text;
        }
    }

    public static class Data{
        private String title;
        private String text;

        public Data() {
        }

        public void setTitle(String title){
            this.title = title;
        }

        public void setText(String text){
            this.text = text;
        }
    }
}

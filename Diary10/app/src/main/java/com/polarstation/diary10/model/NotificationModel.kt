package com.polarstation.diary10.model

data class NotificationModel(val to: String, val notification: Notification, val data: Data){
    constructor(to: String) : this(to, Notification("", ""), Data("", ""))
    data class Notification(var title: String, var text: String)
    data class Data(var title: String, var text: String)
}
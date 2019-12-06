package com.polarstation.diary10.model

data class DiaryModel(val title: String, val uid: String, val coverImageUrl: String, val isPrivate: Boolean,
                      val createTime: Long, val key: String = "", val pages: Map<String, PageModel> = HashMap(),
                      val likeUsers: Map<String, Boolean> = HashMap()){
    constructor(): this("", "", "", false,
            -1L, "")
}

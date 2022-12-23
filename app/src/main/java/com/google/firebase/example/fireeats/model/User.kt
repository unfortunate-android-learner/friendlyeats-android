package com.google.firebase.example.fireeats.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    //val id: String? = null,
    val name: String? = null,
    val chats: List<String>? = emptyList()
): java.io.Serializable {
    companion object {

        //const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_CHATS = "chats"
    }
}
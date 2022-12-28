package com.google.firebase.example.fireeats.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Chat(
    //val id: String? = null,
    val name: String? = null,
    val chat_members: List<String>? = emptyList()
): java.io.Serializable {
    companion object {

        //const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_CHAT_MEMBERS = "chat_members"
    }
}
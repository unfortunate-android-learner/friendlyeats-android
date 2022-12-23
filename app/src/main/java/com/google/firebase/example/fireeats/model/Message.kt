package com.google.firebase.example.fireeats.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Message(
    val message: String? = null,
    val sender: String? = null,
    val time: String? = null,

    @ServerTimestamp
    val timestamp: Timestamp? = null
) {
    companion object {
        const val FIELD_MESSAGE = "message"
        const val FIELD_SENDER = "sender"
        const val FIELD_TIME = "time"
        const val FIELD_TIMESTAMP = "timestamp"
    }
}
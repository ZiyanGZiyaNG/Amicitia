package com.example.amicitia.ui.menu.chat

import com.google.firebase.Timestamp

data class Room(
    val roomId: String = "",
    val members: List<String> = emptyList(),
    val lastMessageAt: Timestamp? = null,
    val lastMessageText: String? = null,
    val lastMessageType: String? = null,
    val lastReadAt: Map<String, Timestamp>? = null
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val type: String = "text",
    val text: String = "",
    val createdAt: Timestamp? = null
)
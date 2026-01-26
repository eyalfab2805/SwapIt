package com.example.swapit.data

data class Conversation(
    val id: String = "",
    val itemId: String = "",
    val participants: Map<String, Boolean> = emptyMap(),
    val participantNicknames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L,
    val createdAt: Long = 0L
)

data class ChatMessage(
    val id: String = "",
    val senderUid: String = "",
    val text: String = "",
    val createdAt: Long = 0L
)

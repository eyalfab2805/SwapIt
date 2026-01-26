package com.example.swapit.ui.chat

data class ConversationRow(
    val conversationId: String = "",
    val itemId: String = "",
    val itemTitle: String = "",
    val itemImageUrl: String = "",
    val otherUid: String = "",
    val otherNickname: String = "",
    val lastMessage: String = "",
    val lastMessageAt: Long = 0L
)

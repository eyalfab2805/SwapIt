package com.example.swapit.data

import com.example.swapit.ui.chat.ConversationRow
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object ChatRepository {

    private val db = FirebaseDatabase.getInstance().reference

    private fun convId(itemId: String, a: String, b: String): String {
        val s = listOf(a, b).sorted()
        val safeItemId = itemId.replace(Regex("[.#$\\[\\]/]"), "_")
        return "${safeItemId}_${s[0]}_${s[1]}"
    }

    fun listenMyConversations(onUpdate: (List<ConversationRow>) -> Unit): ValueEventListener {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        val ref = db.child("userConversations").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { snap ->
                    val cid = snap.key ?: return@mapNotNull null

                    val itemId = snap.child("itemId").getValue(String::class.java).orEmpty()
                    val itemTitle = snap.child("itemTitle").getValue(String::class.java).orEmpty()
                    val itemImageUrl = snap.child("itemImageUrl").getValue(String::class.java).orEmpty()

                    val otherUid = snap.child("otherUid").getValue(String::class.java).orEmpty()
                    val otherNickname = snap.child("otherNickname").getValue(String::class.java).orEmpty()
                    val lastMessage = snap.child("lastMessage").getValue(String::class.java).orEmpty()
                    val lastMessageAt = snap.child("lastMessageAt").getValue(Long::class.java) ?: 0L

                    ConversationRow(
                        conversationId = cid,
                        itemId = itemId,
                        itemTitle = itemTitle,
                        itemImageUrl = itemImageUrl,
                        otherUid = otherUid,
                        otherNickname = otherNickname,
                        lastMessage = lastMessage,
                        lastMessageAt = lastMessageAt
                    )
                }.sortedByDescending { it.lastMessageAt }

                onUpdate(list)
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(listener)
        return listener
    }

    fun stopListeningMyConversations(listener: ValueEventListener) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("userConversations").child(uid).removeEventListener(listener)
    }

    fun listenItemAvailability(conversationId: String, onUpdate: (available: Boolean) -> Unit): ValueEventListener {
        val ref = db.child("conversations").child(conversationId).child("itemId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val itemId = snapshot.getValue(String::class.java).orEmpty()
                if (itemId.isBlank()) {
                    onUpdate(false)
                    return
                }

                db.child("items").child(itemId).child("status").get()
                    .addOnSuccessListener { st ->
                        val status = st.getValue(String::class.java).orEmpty()
                        onUpdate(status == "active")
                    }
                    .addOnFailureListener { onUpdate(false) }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        return listener
    }

    fun stopListeningItemAvailability(conversationId: String, listener: ValueEventListener) {
        db.child("conversations").child(conversationId).child("itemId").removeEventListener(listener)
    }

    fun deleteConversation(
        conversationId: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        db.child("conversations").child(conversationId).get()
            .addOnSuccessListener { snap ->
                val buyerUid = snap.child("buyerUid").getValue(String::class.java).orEmpty()
                val sellerUid = snap.child("sellerUid").getValue(String::class.java).orEmpty()
                if (buyerUid.isBlank() || sellerUid.isBlank()) {
                    onFailure()
                    return@addOnSuccessListener
                }

                val updates = hashMapOf<String, Any?>(
                    "/userConversations/$buyerUid/$conversationId" to null,
                    "/userConversations/$sellerUid/$conversationId" to null,
                    "/messages/$conversationId" to null,
                    "/conversations/$conversationId" to null
                )

                db.updateChildren(updates)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure() }
            }
            .addOnFailureListener { onFailure() }
    }

    fun createOrGetConversation(
        itemId: String,
        itemTitle: String,
        itemImageUrl: String,
        ownerUid: String,
        ownerNickname: String,
        onSuccess: (conversationId: String) -> Unit,
        onFailure: () -> Unit
    ) {
        val meUid = FirebaseAuth.getInstance().currentUser?.uid ?: run { onFailure(); return }
        if (meUid == ownerUid) { onFailure(); return }

        val conversationId = convId(itemId, meUid, ownerUid)
        val conversationRef = db.child("conversations").child(conversationId)

        conversationRef.get()
            .addOnSuccessListener { snap ->
                if (snap.exists()) {
                    onSuccess(conversationId)
                    return@addOnSuccessListener
                }

                db.child("users").child(meUid).child("profile").child("nickname").get()
                    .addOnSuccessListener { nickSnap ->
                        val myNick = nickSnap.getValue(String::class.java)?.trim().orEmpty().ifBlank { "User" }.take(30)
                        val otherNick = ownerNickname.trim().ifBlank { "User" }.take(30)

                        val safeTitle = itemTitle.trim().ifBlank { "Item" }.take(80)
                        val safeImage = itemImageUrl.trim()

                        val now = System.currentTimeMillis()

                        val conv = mapOf(
                            "itemId" to itemId,
                            "buyerUid" to meUid,
                            "buyerNickname" to myNick,
                            "sellerUid" to ownerUid,
                            "sellerNickname" to otherNick,
                            "createdAt" to now,
                            "lastMessage" to "",
                            "lastMessageAt" to 0L
                        )

                        conversationRef.setValue(conv)
                            .addOnSuccessListener {
                                val updates = hashMapOf<String, Any>(
                                    "/userConversations/$meUid/$conversationId" to mapOf(
                                        "itemId" to itemId,
                                        "itemTitle" to safeTitle,
                                        "itemImageUrl" to safeImage,
                                        "otherUid" to ownerUid,
                                        "otherNickname" to otherNick,
                                        "lastMessage" to "",
                                        "lastMessageAt" to 0L,
                                        "lastSeenAt" to 0L
                                    ),
                                    "/userConversations/$ownerUid/$conversationId" to mapOf(
                                        "itemId" to itemId,
                                        "itemTitle" to safeTitle,
                                        "itemImageUrl" to safeImage,
                                        "otherUid" to meUid,
                                        "otherNickname" to myNick,
                                        "lastMessage" to "",
                                        "lastMessageAt" to 0L,
                                        "lastSeenAt" to 0L
                                    )
                                )

                                db.updateChildren(updates)
                                    .addOnSuccessListener { onSuccess(conversationId) }
                                    .addOnFailureListener { onFailure() }
                            }
                            .addOnFailureListener { onFailure() }
                    }
                    .addOnFailureListener { onFailure() }
            }
            .addOnFailureListener { onFailure() }
    }

    fun listenMessages(
        conversationId: String,
        onUpdate: (List<ChatMessage>) -> Unit
    ): ValueEventListener {
        val ref = db.child("messages").child(conversationId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.getValue(ChatMessage::class.java) }
                    .sortedBy { it.createdAt }
                onUpdate(list)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        return listener
    }

    fun stopListening(conversationId: String, listener: ValueEventListener) {
        db.child("messages").child(conversationId).removeEventListener(listener)
    }

    fun sendMessage(
        conversationId: String,
        text: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run { onFailure(); return }
        val cleanText = text.trim()
        if (cleanText.isBlank()) { onFailure(); return }

        val now = System.currentTimeMillis()
        val msgRef = db.child("messages").child(conversationId).push()
        val msgId = msgRef.key ?: run { onFailure(); return }

        val msg = ChatMessage(
            id = msgId,
            senderUid = uid,
            text = cleanText,
            createdAt = now
        )

        db.child("conversations").child(conversationId).get()
            .addOnSuccessListener { snap ->
                val buyerUid = snap.child("buyerUid").getValue(String::class.java).orEmpty()
                val sellerUid = snap.child("sellerUid").getValue(String::class.java).orEmpty()
                if (buyerUid.isBlank() || sellerUid.isBlank()) { onFailure(); return@addOnSuccessListener }

                val updates = hashMapOf<String, Any>(
                    "/messages/$conversationId/$msgId" to msg,
                    "/conversations/$conversationId/lastMessage" to cleanText,
                    "/conversations/$conversationId/lastMessageAt" to now,
                    "/userConversations/$buyerUid/$conversationId/lastMessage" to cleanText,
                    "/userConversations/$buyerUid/$conversationId/lastMessageAt" to now,
                    "/userConversations/$sellerUid/$conversationId/lastMessage" to cleanText,
                    "/userConversations/$sellerUid/$conversationId/lastMessageAt" to now
                )

                updates["/userConversations/$uid/$conversationId/lastSeenAt"] = now

                db.updateChildren(updates)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure() }
            }
            .addOnFailureListener { onFailure() }
    }

    fun markConversationSeen(conversationId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val now = System.currentTimeMillis()
        db.child("userConversations").child(uid).child(conversationId).child("lastSeenAt").setValue(now)
    }

    fun listenUnreadCount(onUpdate: (Int) -> Unit): ValueEventListener {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {}
            override fun onCancelled(error: DatabaseError) {}
        }

        val ref = db.child("userConversations").child(uid)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var unread = 0
                for (c in snapshot.children) {
                    val lastMessageAt = c.child("lastMessageAt").getValue(Long::class.java) ?: 0L
                    val lastSeenAt = c.child("lastSeenAt").getValue(Long::class.java) ?: 0L
                    if (lastMessageAt > lastSeenAt) unread++
                }
                onUpdate(unread)
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        ref.addValueEventListener(listener)
        return listener
    }

    fun stopListeningUnreadCount(listener: ValueEventListener) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        db.child("userConversations").child(uid).removeEventListener(listener)
    }
}

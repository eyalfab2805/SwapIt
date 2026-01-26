package com.example.swapit.data

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

object ItemRepository {

    private val db = FirebaseDatabase.getInstance().reference
    private val itemsRef = db.child("items")

    private val _items = MutableLiveData<List<Item>>(emptyList())
    val items: LiveData<List<Item>> = _items

    private var itemsListener: ValueEventListener? = null
    private var swipesListener: ValueEventListener? = null

    private var swipesRef: DatabaseReference? = null
    private var activeUid: String? = null

    private val swipedIds = mutableSetOf<String>()

    private fun uidOrNull(): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    private fun userSwipesRef(uid: String): DatabaseReference =
        db.child("users").child(uid).child("swipes")

    private fun userItemsRef(uid: String): DatabaseReference =
        db.child("users").child(uid).child("userItems")

    private fun profileNickRef(uid: String): DatabaseReference =
        db.child("users").child(uid).child("profile").child("nickname")

    data class FeedFilters(
        val category: String? = null,
        val center: ItemLocation? = null,
        val radiusKm: Double? = null
    )

    private var filters: FeedFilters = FeedFilters()

    fun setFilters(newFilters: FeedFilters) {
        filters = newFilters
        refreshItemsIfNeeded()
    }

    fun clearFilters() {
        filters = FeedFilters()
        refreshItemsIfNeeded()
    }

    fun start() {
        val uid = uidOrNull() ?: return

        if (activeUid != null && activeUid != uid) stop()
        if (itemsListener != null || swipesListener != null) return

        activeUid = uid
        swipesRef = userSwipesRef(uid)

        swipesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                swipedIds.clear()
                snapshot.children.forEach { child ->
                    child.key?.let { swipedIds.add(it) }
                }
                if (itemsListener == null) startItemsListener() else refreshItemsIfNeeded()
            }

            override fun onCancelled(error: DatabaseError) {
                stop()
            }
        }

        swipesRef!!.addValueEventListener(swipesListener!!)
    }

    private fun startItemsListener() {
        itemsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val uid = uidOrNull()

                val list = snapshot.children.mapNotNull { child ->
                    child.getValue(Item::class.java)?.copy(id = child.key ?: "")
                }
                    .filter { item -> !swipedIds.contains(item.id) && item.ownerUid != uid }
                    .filter { item -> passesFilters(item) }
                    .sortedByDescending { it.createdAt }

                _items.value = list
            }

            override fun onCancelled(error: DatabaseError) {
                stop()
            }
        }

        itemsRef.addValueEventListener(itemsListener!!)
    }

    private fun refreshItemsIfNeeded() {
        val uid = uidOrNull()
        val current = _items.value ?: return

        _items.value = current
            .filter { item -> !swipedIds.contains(item.id) && item.ownerUid != uid }
            .filter { item -> passesFilters(item) }
    }

    fun swipe(itemId: String, action: String) {
        val uid = uidOrNull() ?: return
        userSwipesRef(uid).child(itemId).setValue(action)

        swipedIds.add(itemId)
        refreshItemsIfNeeded()
    }


    private fun fetchNickname(uid: String, onDone: (String) -> Unit) {
        profileNickRef(uid).get()
            .addOnSuccessListener { snap ->
                val nick = snap.getValue(String::class.java)
                onDone(nick?.takeIf { it.isNotBlank() } ?: uid.take(6))
            }
            .addOnFailureListener {
                onDone(uid.take(6))
            }
    }

    fun addItem(
        title: String,
        desc: String,
        category: String,
        location: ItemLocation,
        imageUris: List<Uri>,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val uid = uidOrNull() ?: return onFailure()
        if (title.isBlank() || desc.isBlank() || category.isBlank()) return onFailure()
        if (imageUris.isEmpty()) return onFailure()

        val newRef = itemsRef.push()
        val itemId = newRef.key ?: return onFailure()

        val storageRoot = FirebaseStorage.getInstance().reference
        val uploadedRefs = mutableListOf<com.google.firebase.storage.StorageReference>()
        val urls = mutableListOf<String>()
        val createdAt = System.currentTimeMillis()

        fun cleanupAndFail() {
            val tasks = uploadedRefs.map { it.delete() }
            if (tasks.isNotEmpty()) {
                Tasks.whenAllComplete(tasks).addOnCompleteListener { onFailure() }
            } else {
                onFailure()
            }
        }

        fun writeItemToDb(ownerNickname: String) {
            val geohash = GeoHash.encode(location.lat, location.lng, precision = 8)

            val itemMap = hashMapOf<String, Any?>(
                "title" to title,
                "desc" to desc,
                "category" to category,
                "createdAt" to createdAt,
                "updatedAt" to createdAt,
                "ownerUid" to uid,
                "ownerNickname" to ownerNickname,
                "status" to "active",
                "imagesCount" to urls.size,
                "imageUrls" to urls,
                "location" to mapOf(
                    "lat" to location.lat,
                    "lng" to location.lng,
                    "label" to location.label
                ),
                "geo" to mapOf("geohash" to geohash)
            )

            val userItemSummary = hashMapOf<String, Any?>(
                "itemId" to itemId,
                "title" to title,
                "category" to category,
                "createdAt" to createdAt,
                "ownerNickname" to ownerNickname,
                "locationLabel" to location.label,
                "firstImageUrl" to (urls.firstOrNull() ?: "")
            )

            val updates = hashMapOf<String, Any?>(
                "/items/$itemId" to itemMap,
                "/users/$uid/userItems/$itemId" to userItemSummary
            )

            db.updateChildren(updates).addOnCompleteListener { task ->
                if (task.isSuccessful) onSuccess() else cleanupAndFail()
            }
        }

        fun uploadNext(i: Int) {
            if (i >= imageUris.size) {
                fetchNickname(uid) { nick -> writeItemToDb(nick) }
                return
            }

            val uri = imageUris[i]
            val imgRef = storageRoot
                .child("items")
                .child(uid)
                .child(itemId)
                .child("${UUID.randomUUID()}.jpg")

            imgRef.putFile(uri)
                .continueWithTask { t ->
                    if (!t.isSuccessful) throw (t.exception ?: RuntimeException("Upload failed"))
                    imgRef.downloadUrl
                }
                .addOnSuccessListener { downloadUrl ->
                    uploadedRefs.add(imgRef)
                    urls.add(downloadUrl.toString())
                    uploadNext(i + 1)
                }
                .addOnFailureListener { cleanupAndFail() }
        }

        uploadNext(0)
    }

    fun updateItem(
        itemId: String,
        title: String,
        desc: String,
        category: String,
        location: ItemLocation,
        keepImageUrls: List<String>,
        removedImageUrls: List<String>,
        newImageUris: List<Uri>,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val uid = uidOrNull() ?: return onFailure()
        if (itemId.isBlank()) return onFailure()
        if (title.isBlank() || desc.isBlank() || category.isBlank()) return onFailure()

        itemsRef.child(itemId).get()
            .addOnSuccessListener { snap ->
                val existing = snap.getValue(Item::class.java)?.copy(id = snap.key ?: "") ?: run {
                    onFailure()
                    return@addOnSuccessListener
                }

                if (existing.ownerUid != uid) {
                    onFailure()
                    return@addOnSuccessListener
                }

                if (keepImageUrls.isEmpty() && newImageUris.isEmpty()) {
                    onFailure()
                    return@addOnSuccessListener
                }

                val createdAt = existing.createdAt.takeIf { it > 0 } ?: System.currentTimeMillis()
                val now = System.currentTimeMillis()
                val geohash = GeoHash.encode(location.lat, location.lng, precision = 8)

                fetchNickname(uid) { ownerNickname ->
                    val storageRoot = FirebaseStorage.getInstance().reference
                    val uploadedRefs = mutableListOf<com.google.firebase.storage.StorageReference>()
                    val newUploadedUrls = mutableListOf<String>()

                    fun cleanupAndFail() {
                        val tasks = uploadedRefs.map { it.delete() }
                        if (tasks.isNotEmpty()) {
                            Tasks.whenAllComplete(tasks).addOnCompleteListener { onFailure() }
                        } else {
                            onFailure()
                        }
                    }

                    fun deleteRemovedThenWrite(finalUrls: List<String>) {
                        val storage = FirebaseStorage.getInstance()
                        val deleteTasks = removedImageUrls.distinct().mapNotNull { url ->
                            try {
                                storage.getReferenceFromUrl(url).delete()
                            } catch (_: Exception) {
                                null
                            }
                        }

                        val afterDelete =
                            if (deleteTasks.isNotEmpty()) Tasks.whenAllComplete(deleteTasks)
                            else Tasks.forResult(null)

                        afterDelete.addOnCompleteListener {
                            val updates = hashMapOf<String, Any?>(
                                "/items/$itemId/title" to title,
                                "/items/$itemId/desc" to desc,
                                "/items/$itemId/category" to category,
                                "/items/$itemId/location" to mapOf(
                                    "lat" to location.lat,
                                    "lng" to location.lng,
                                    "label" to location.label
                                ),
                                "/items/$itemId/geo" to mapOf("geohash" to geohash),
                                "/items/$itemId/updatedAt" to now,
                                "/items/$itemId/imagesCount" to finalUrls.size,
                                "/items/$itemId/imageUrls" to finalUrls,
                                "/items/$itemId/ownerNickname" to ownerNickname,
                                "/users/$uid/userItems/$itemId" to mapOf(
                                    "itemId" to itemId,
                                    "title" to title,
                                    "category" to category,
                                    "createdAt" to createdAt,
                                    "ownerNickname" to ownerNickname,
                                    "locationLabel" to location.label,
                                    "firstImageUrl" to (finalUrls.firstOrNull() ?: "")
                                )
                            )

                            db.updateChildren(updates).addOnCompleteListener { t ->
                                if (t.isSuccessful) onSuccess() else cleanupAndFail()
                            }
                        }
                    }

                    fun uploadNext(i: Int) {
                        if (i >= newImageUris.size) {
                            val finalUrls = (keepImageUrls + newUploadedUrls).distinct()
                            if (finalUrls.isEmpty()) {
                                cleanupAndFail()
                                return
                            }
                            deleteRemovedThenWrite(finalUrls)
                            return
                        }

                        val uri = newImageUris[i]
                        val imgRef = storageRoot
                            .child("items")
                            .child(uid)
                            .child(itemId)
                            .child("${UUID.randomUUID()}.jpg")

                        imgRef.putFile(uri)
                            .continueWithTask { t ->
                                if (!t.isSuccessful) throw (t.exception ?: RuntimeException("Upload failed"))
                                imgRef.downloadUrl
                            }
                            .addOnSuccessListener { downloadUrl ->
                                uploadedRefs.add(imgRef)
                                newUploadedUrls.add(downloadUrl.toString())
                                uploadNext(i + 1)
                            }
                            .addOnFailureListener { cleanupAndFail() }
                    }

                    uploadNext(0)
                }
            }
            .addOnFailureListener { onFailure() }
    }

    fun deleteItem(
        item: Item,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        val uid = uidOrNull() ?: return onFailure()
        if (item.ownerUid != uid) return onFailure()
        if (item.id.isBlank()) return onFailure()

        val storage = FirebaseStorage.getInstance()
        val deleteTasks = item.imageUrls.mapNotNull { url ->
            try {
                storage.getReferenceFromUrl(url).delete()
            } catch (_: Exception) {
                null
            }
        }

        val afterStorage =
            if (deleteTasks.isNotEmpty()) Tasks.whenAllComplete(deleteTasks)
            else Tasks.forResult(null)

        afterStorage.addOnCompleteListener {
            val updates = hashMapOf<String, Any?>(
                "/items/${item.id}" to null,
                "/users/$uid/userItems/${item.id}" to null
            )

            db.updateChildren(updates).addOnCompleteListener { t ->
                if (t.isSuccessful) onSuccess() else onFailure()
            }
        }
    }

    fun stop() {
        itemsListener?.let { itemsRef.removeEventListener(it) }
        itemsListener = null

        swipesListener?.let { l -> swipesRef?.removeEventListener(l) }
        swipesListener = null
        swipesRef = null

        swipedIds.clear()
        activeUid = null
        _items.value = emptyList()
    }

    private fun passesFilters(item: Item): Boolean {
        val cat = filters.category
        if (!cat.isNullOrBlank() && item.category != cat) return false

        val center = filters.center
        val radiusKm = filters.radiusKm
        if (center != null && radiusKm != null) {
            val loc = item.location ?: return false
            val d = distanceKm(center.lat, center.lng, loc.lat, loc.lng)
            if (d > radiusKm) return false
        }

        return true
    }

    private fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a =
            Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                    Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

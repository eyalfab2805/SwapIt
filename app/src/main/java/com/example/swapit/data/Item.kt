package com.example.swapit.data

data class Item(
    val id: String = "",
    val title: String = "",
    val desc: String = "",
    val category: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val ownerUid: String = "",
    val ownerNickname: String = "",
    val status: String = "active",
    val imagesCount: Int = 0,
    val imageUrls: List<String> = emptyList(),
    val location: ItemLocation? = null,
    val geo: Geo? = null
)

data class ItemLocation(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val label: String = ""
)

data class Geo(
    val geohash: String = ""
)

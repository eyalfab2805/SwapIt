package com.example.swapit.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.example.swapit.data.Item
import com.example.swapit.data.ItemRepository
import com.example.swapit.utils.GeoUtils

class SwipeViewModel : ViewModel() {

    data class ItemUi(
        val id: String,
        val title: String,
        val desc: String,
        val imageUrls: List<String>,
        val category: String,
        val locationLabel: String,
        val ownerUid: String,
        val ownerNickname: String
    )

    private val _currentItem = MediatorLiveData<ItemUi>()
    val currentItem: LiveData<ItemUi> = _currentItem

    private var index = 0
    private var itemsCache: List<Item> = emptyList()
    private var filteredCache: List<Item> = emptyList()

    private var filters: SwipeFilters = SwipeFilters()
    private var userLatLng: Pair<Double, Double>? = null

    init {
        ItemRepository.start()

        _currentItem.addSource(ItemRepository.items) { list ->
            itemsCache = list ?: emptyList()
            rebuildFiltered(resetIndex = true)
        }
    }

    fun like() = next()
    fun dislike() = next()

    fun setUserLocation(lat: Double, lng: Double) {
        userLatLng = lat to lng
        rebuildFiltered(resetIndex = true)
    }

    fun setFilters(newFilters: SwipeFilters) {
        filters = newFilters
        rebuildFiltered(resetIndex = true)
    }

    fun clearFilters() {
        filters = SwipeFilters()
        rebuildFiltered(resetIndex = true)
    }

    fun getFilters(): SwipeFilters = filters

    fun availableCategories(): List<String> =
        itemsCache.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

    private fun next() {
        index++
        publishCurrent()
    }

    private fun rebuildFiltered(resetIndex: Boolean) {
        filteredCache = itemsCache.filter { item ->
            val cats = filters.categories
            if (cats != null && item.category !in cats) return@filter false

            val maxKm = filters.maxDistanceKm
            if (maxKm != null) {
                val u = userLatLng ?: return@filter false
                val loc = item.location ?: return@filter false
                val d = GeoUtils.distanceKm(u.first, u.second, loc.lat, loc.lng)
                if (d > maxKm) return@filter false
            }

            true
        }

        if (resetIndex) index = 0
        publishCurrent()
    }

    private fun filtersActive(): Boolean {
        val hasDistance = filters.maxDistanceKm != null
        val hasCats = !filters.categories.isNullOrEmpty()
        return hasDistance || hasCats
    }

    private fun publishCurrent() {
        if (filteredCache.isEmpty() || index >= filteredCache.size) {
            val isFiltered = filtersActive()

            _currentItem.value = if (isFiltered) {
                ItemUi(
                    id = "",
                    title = "No items match your filters",
                    desc = "Try increasing the distance or selecting more categories.",
                    imageUrls = emptyList(),
                    category = "",
                    locationLabel = "",
                    ownerUid = "",
                    ownerNickname = ""
                )
            } else {
                ItemUi(
                    id = "",
                    title = "No more items",
                    desc = "You’ve seen everything 🎉",
                    imageUrls = emptyList(),
                    category = "",
                    locationLabel = "",
                    ownerUid = "",
                    ownerNickname = ""
                )
            }
            return
        }

        val item = filteredCache[index]
        _currentItem.value = ItemUi(
            id = item.id,
            title = item.title,
            desc = item.desc,
            imageUrls = item.imageUrls,
            category = item.category,
            locationLabel = item.location?.label.orEmpty(),
            ownerUid = item.ownerUid,
            ownerNickname = item.ownerNickname
        )
    }

    override fun onCleared() {
        super.onCleared()
        ItemRepository.stop()
    }
}

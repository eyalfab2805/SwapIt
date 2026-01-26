package com.example.swapit.ui.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.swapit.data.Item
import com.example.swapit.databinding.FragmentItemDetailsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase

class ItemDetailsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentItemDetailsBinding? = null
    private val binding get() = _binding!!

    private var map: GoogleMap? = null
    private var itemForMap: Item? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.mapContainer.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    v.parent?.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        val itemId = requireArguments().getString("itemId") ?: return

        FirebaseDatabase.getInstance().reference
            .child("items")
            .child(itemId)
            .get()
            .addOnSuccessListener { snap ->
                val item = snap.getValue(Item::class.java) ?: return@addOnSuccessListener
                render(item)
                setupMiniMap(item)
            }
    }

    private fun render(item: Item) {
        binding.tvTitle.text = item.title
        binding.tvDesc.text = item.desc
        binding.tvCategory.text = item.category
        binding.tvLocation.text = item.location?.label.orEmpty()
        binding.tvUploader.text =
            if (item.ownerNickname.isNotBlank()) "Uploaded by ${item.ownerNickname}" else ""

        if (item.imageUrls.isNotEmpty()) {
            binding.imagesPager.adapter = ItemImagesPagerAdapter(item.imageUrls)
        }
    }

    private fun setupMiniMap(item: Item) {
        val loc = item.location ?: return
        itemForMap = item

        val mapFragment =
            childFragmentManager.findFragmentById(binding.mapContainer.id) as SupportMapFragment?
                ?: SupportMapFragment.newInstance().also {
                    childFragmentManager.beginTransaction()
                        .replace(binding.mapContainer.id, it)
                        .commitNow()
                }

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        googleMap.uiSettings.isZoomGesturesEnabled = true
        googleMap.uiSettings.isScrollGesturesEnabled = true
        googleMap.uiSettings.isRotateGesturesEnabled = true
        googleMap.uiSettings.isTiltGesturesEnabled = true
        googleMap.uiSettings.isZoomControlsEnabled = true

        val loc = itemForMap?.location ?: return
        val latLng = LatLng(loc.lat, loc.lng)

        googleMap.addMarker(MarkerOptions().position(latLng))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map = null
        _binding = null
    }
}

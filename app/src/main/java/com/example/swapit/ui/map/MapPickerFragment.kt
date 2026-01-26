package com.example.swapit.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.swapit.databinding.FragmentMapPickerBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapPickerFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapPickerBinding? = null
    private val binding get() = _binding!!

    private var map: GoogleMap? = null
    private var selectedLatLng: LatLng? = null
    private var marker: Marker? = null

    private val fused by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = (result[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (result[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

            if (granted) {
                enableMyLocationLayerIfPossible()
                moveToCurrentLocation()
            } else {
                binding.tvHint.text = "Location permission denied. Tap map to choose manually."
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(binding.mapContainer.id) as SupportMapFragment?
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction()
                    .replace(binding.mapContainer.id, it)
                    .commitNow()
            }

        mapFragment.getMapAsync(this)

        binding.btnConfirm.isEnabled = false

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnConfirm.setOnClickListener {
            val latLng = selectedLatLng ?: return@setOnClickListener
            val label = reverseGeocodeLabel(latLng)

            parentFragmentManager.setFragmentResult(
                "pick_location",
                Bundle().apply {
                    putDouble("lat", latLng.latitude)
                    putDouble("lng", latLng.longitude)
                    putString("label", label)
                }
            )
            findNavController().popBackStack()
        }

        binding.btnMyLocation.setOnClickListener {
            ensureLocationPermissionThenMove()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        val defaultCenter = LatLng(32.0853, 34.7818)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCenter, 11f))

        enableMyLocationLayerIfPossible()

        googleMap.setOnMapClickListener { latLng ->
            selectLatLng(latLng)
            binding.tvHint.text = "Location selected ✅"
        }
    }

    private fun ensureLocationPermissionThenMove() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            enableMyLocationLayerIfPossible()
            moveToCurrentLocation()
        } else {
            requestLocationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun enableMyLocationLayerIfPossible() {
        val googleMap = map ?: return
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            try {
                googleMap.isMyLocationEnabled = true
            } catch (_: SecurityException) {}
        }
    }

    private fun moveToCurrentLocation() {
        val googleMap = map ?: return

        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    selectLatLng(latLng)
                    binding.tvHint.text = "Using your current location ✅"
                } else {
                    binding.tvHint.text = "Couldn't get location. Tap map to choose manually."
                }
            }.addOnFailureListener {
                binding.tvHint.text = "Couldn't get location. Tap map to choose manually."
            }
        } catch (_: SecurityException) {
            binding.tvHint.text = "Location permission missing."
        }
    }

    private fun selectLatLng(latLng: LatLng) {
        val googleMap = map ?: return

        selectedLatLng = latLng
        binding.btnConfirm.isEnabled = true

        marker?.remove()
        marker = googleMap.addMarker(
            MarkerOptions().position(latLng).title("Selected location")
        )
    }

    private fun reverseGeocodeLabel(latLng: LatLng): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val address = results?.firstOrNull()

            val city = address?.locality
            val area = address?.subLocality
            val country = address?.countryName

            listOfNotNull(area, city, country)
                .distinct()
                .joinToString(", ")
                .ifBlank { "${latLng.latitude}, ${latLng.longitude}" }
        } catch (_: Exception) {
            "${latLng.latitude}, ${latLng.longitude}"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        map = null
        marker = null
        _binding = null
    }
}

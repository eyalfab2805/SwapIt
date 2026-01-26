package com.example.swapit.ui.home

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.example.swapit.R
import com.example.swapit.databinding.BottomSheetSwipeFiltersBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlin.math.roundToInt

class SwipeFiltersBottomSheet(
    private val categories: List<String>,
    private val onNeedsLocation: (onReady: (lat: Double, lng: Double) -> Unit) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetSwipeFiltersBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SwipeViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetSwipeFiltersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val current = viewModel.getFilters()

        val currentDistance = current.maxDistanceKm ?: 0
        binding.sliderDistance.value = currentDistance.toFloat()
        updateDistanceLabel(currentDistance)

        binding.sliderDistance.addOnChangeListener { _, value, _ ->
            updateDistanceLabel(value.roundToInt())
        }

        val brand = ContextCompat.getColor(requireContext(), R.color.brand_green)
        val checkedFill = ColorStateList.valueOf(brand)
        val stroke = ColorStateList.valueOf(brand)

        binding.chipGroupCategories.removeAllViews()

        val safeCategories = categories.filter { it.isNotBlank() }.distinct()
        safeCategories.forEach { name ->
            val chip = Chip(requireContext()).apply {
                text = name
                isCheckable = true
                isCheckedIconVisible = false
                chipStrokeWidth = 1f
                chipStrokeColor = stroke
                setTextColor(brand)
                chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)

                if (current.categories?.contains(name) == true) {
                    setTextColor(Color.WHITE)
                    chipBackgroundColor = checkedFill
                    isChecked = true
                }

                setOnCheckedChangeListener { button, isChecked ->
                    val c = button as Chip
                    if (isChecked) {
                        c.setTextColor(Color.WHITE)
                        c.chipBackgroundColor = checkedFill
                    } else {
                        c.setTextColor(brand)
                        c.chipBackgroundColor = ColorStateList.valueOf(Color.TRANSPARENT)
                    }
                }
            }
            binding.chipGroupCategories.addView(chip)
        }

        binding.btnClear.setOnClickListener {
            viewModel.clearFilters()
            dismiss()
        }

        binding.btnApply.setOnClickListener {
            val km = binding.sliderDistance.value.roundToInt()
            val maxDistance = if (km == 0) null else km

            val selectedCats = mutableSetOf<String>()
            for (i in 0 until binding.chipGroupCategories.childCount) {
                val v = binding.chipGroupCategories.getChildAt(i)
                if (v is Chip && v.isChecked) selectedCats.add(v.text.toString())
            }
            val categoriesFilter = if (selectedCats.isEmpty()) null else selectedCats

            val newFilters = SwipeFilters(maxDistanceKm = maxDistance, categories = categoriesFilter)

            if (maxDistance == null) {
                viewModel.setFilters(newFilters)
                dismiss()
                return@setOnClickListener
            }

            onNeedsLocation { lat, lng ->
                viewModel.setUserLocation(lat, lng)
                viewModel.setFilters(newFilters)
                dismiss()
            }
        }
    }

    private fun updateDistanceLabel(km: Int) {
        binding.tvDistanceValue.text = if (km == 0) "Any" else "$km km"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

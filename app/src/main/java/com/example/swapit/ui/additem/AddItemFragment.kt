package com.example.swapit.ui.additem

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.swapit.R
import com.example.swapit.data.Item
import com.example.swapit.data.ItemLocation
import com.example.swapit.data.ItemRepository
import com.example.swapit.databinding.FragmentAddItemBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddItemFragment : Fragment() {

    private var _binding: FragmentAddItemBinding? = null
    private val binding get() = _binding!!

    private var selectedLocation: ItemLocation? = null
    private var editItemId: String? = null
    private var isEditMode: Boolean = false

    private val images: MutableList<EditableImage> = mutableListOf()
    private val removedExistingUrls: MutableSet<String> = mutableSetOf()

    private lateinit var imagesAdapter: EditImagesAdapter

    private val pickImages =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNullOrEmpty()) return@registerForActivityResult

            uris.forEach { uri ->
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            uris.forEach { uri -> images.add(EditableImage.New(uri)) }
            imagesAdapter.notifyDataSetChanged()
            updateImagesUi()

            Snackbar.make(binding.root, "Added ${uris.size} photos ✅", Snackbar.LENGTH_SHORT).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editItemId = savedInstanceState?.getString(KEY_EDIT_ITEM_ID) ?: arguments?.getString("itemId")
        isEditMode = !editItemId.isNullOrBlank()

        savedInstanceState?.let { state ->
            val lat = state.getDouble(KEY_LOC_LAT, Double.NaN)
            val lng = state.getDouble(KEY_LOC_LNG, Double.NaN)
            val label = state.getString(KEY_LOC_LABEL).orEmpty()
            if (!lat.isNaN() && !lng.isNaN()) {
                selectedLocation = ItemLocation(lat = lat, lng = lng, label = label)
            }

            images.clear()
            removedExistingUrls.clear()

            (state.getStringArrayList(KEY_EXISTING_URLS) ?: arrayListOf())
                .forEach { images.add(EditableImage.Existing(it)) }

            (state.getParcelableArrayList<Uri>(KEY_NEW_URIS) ?: arrayListOf())
                .forEach { images.add(EditableImage.New(it)) }

            removedExistingUrls.addAll(state.getStringArrayList(KEY_REMOVED_URLS) ?: arrayListOf())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ItemRepository.start()
        setupLocationResultListener()
        setupImagesRecycler()

        if (isEditMode) {
            binding.tvHeader.text = "Edit Item"
            binding.btnPublish.text = "Save"
            if (images.isEmpty()) loadItemForEdit() else updateImagesUi()
        } else {
            renderLocation()
            updateImagesUi()
        }

        binding.cardImage.setOnClickListener {
            pickImages.launch(arrayOf("image/*"))
        }

        binding.btnPickLocation.setOnClickListener {
            findNavController().navigate(R.id.mapPickerFragment)
        }

        binding.btnPublish.setOnClickListener {
            val title = binding.etTitle.text?.toString()?.trim().orEmpty()
            val desc = binding.etDesc.text?.toString()?.trim().orEmpty()
            val category = getSelectedCategory()
            val location = selectedLocation

            var ok = true

            if (title.isEmpty()) {
                binding.tilTitle.error = "Title is required"
                ok = false
            } else binding.tilTitle.error = null

            if (desc.isEmpty()) {
                binding.tilDesc.error = "Description is required"
                ok = false
            } else binding.tilDesc.error = null

            if (category.isNullOrBlank()) {
                Snackbar.make(binding.root, "Please select a category", Snackbar.LENGTH_SHORT).show()
                ok = false
            }

            if (location == null) {
                Snackbar.make(binding.root, "Please pick a location on the map", Snackbar.LENGTH_SHORT).show()
                ok = false
            }

            if (images.isEmpty()) {
                Snackbar.make(binding.root, "Please add at least 1 photo", Snackbar.LENGTH_SHORT).show()
                ok = false
            }

            if (!ok) return@setOnClickListener

            val keepUrls = images.mapNotNull { (it as? EditableImage.Existing)?.url }
            val newUris = images.mapNotNull { (it as? EditableImage.New)?.uri }

            binding.btnPublish.isEnabled = false

            if (!isEditMode) {
                if (newUris.isEmpty()) {
                    binding.btnPublish.isEnabled = true
                    Snackbar.make(binding.root, "Please add at least 1 photo", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                ItemRepository.addItem(
                    title = title,
                    desc = desc,
                    category = category!!,
                    location = location!!,
                    imageUris = newUris,
                    onSuccess = {
                        binding.btnPublish.isEnabled = true
                        Snackbar.make(binding.root, "Item published ✅", Snackbar.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.swipeFragment)
                    },
                    onFailure = {
                        binding.btnPublish.isEnabled = true
                        Snackbar.make(binding.root, "Publish failed", Snackbar.LENGTH_SHORT).show()
                    }
                )
            } else {
                val itemId = editItemId ?: run {
                    binding.btnPublish.isEnabled = true
                    Snackbar.make(binding.root, "Missing item id", Snackbar.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                ItemRepository.updateItem(
                    itemId = itemId,
                    title = title,
                    desc = desc,
                    category = category!!,
                    location = location!!,
                    keepImageUrls = keepUrls,
                    removedImageUrls = removedExistingUrls.toList(),
                    newImageUris = newUris,
                    onSuccess = {
                        binding.btnPublish.isEnabled = true
                        Snackbar.make(binding.root, "Saved ✅", Snackbar.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    },
                    onFailure = {
                        binding.btnPublish.isEnabled = true
                        Snackbar.make(binding.root, "Save failed", Snackbar.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }

    private fun setupImagesRecycler() {
        imagesAdapter = EditImagesAdapter(
            images = images,
            onRemove = { position ->
                if (position < 0 || position >= images.size) return@EditImagesAdapter

                if (images.size == 1) {
                    Snackbar.make(binding.root, "At least 1 photo is required", Snackbar.LENGTH_SHORT).show()
                    return@EditImagesAdapter
                }

                val removed = images[position]
                if (removed is EditableImage.Existing) removedExistingUrls.add(removed.url)

                images.removeAt(position)
                imagesAdapter.notifyItemRemoved(position)
                updateImagesUi()
            }
        )

        binding.rvImages.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rvImages.adapter = imagesAdapter
    }

    private fun updateImagesUi() {
        binding.rvImages.visibility = if (images.isEmpty()) View.GONE else View.VISIBLE

        val first = images.firstOrNull()
        when (first) {
            is EditableImage.Existing -> {
                binding.ivPreview.alpha = 1f
                binding.ivPreview.load(first.url) { crossfade(true) }
                binding.emptyPhotoOverlay.visibility = View.GONE
                binding.overlayScrim.visibility = View.GONE
            }
            is EditableImage.New -> {
                binding.ivPreview.alpha = 1f
                binding.ivPreview.setImageURI(first.uri)
                binding.emptyPhotoOverlay.visibility = View.GONE
                binding.overlayScrim.visibility = View.GONE
            }
            null -> {
                binding.ivPreview.setImageResource(android.R.drawable.sym_def_app_icon)
                binding.ivPreview.alpha = 0.18f
                binding.emptyPhotoOverlay.visibility = View.VISIBLE
                binding.overlayScrim.visibility = View.VISIBLE
            }
        }

        updateSaveEnabled()
    }

    private fun updateSaveEnabled() {
        val hasLocation = selectedLocation != null
        val hasCategory = !getSelectedCategory().isNullOrBlank()
        val hasTitle = !binding.etTitle.text?.toString()?.trim().isNullOrEmpty()
        val hasDesc = !binding.etDesc.text?.toString()?.trim().isNullOrEmpty()

        val hasAtLeastOnePhoto = images.isNotEmpty()
        val enabled = hasLocation && hasCategory && hasTitle && hasDesc && hasAtLeastOnePhoto

        binding.btnPublish.isEnabled = enabled
        binding.btnPublish.alpha = if (enabled) 1f else 0.6f
    }

    private fun loadItemForEdit() {
        val itemId = editItemId ?: return
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance().reference
            .child("items")
            .child(itemId)
            .get()
            .addOnSuccessListener { snap ->
                val item = snap.getValue(Item::class.java)?.copy(id = snap.key ?: "") ?: run {
                    Snackbar.make(binding.root, "Item not found", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    return@addOnSuccessListener
                }

                if (item.ownerUid != uid) {
                    Snackbar.make(binding.root, "You can edit only your items", Snackbar.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                    return@addOnSuccessListener
                }

                binding.etTitle.setText(item.title)
                binding.etDesc.setText(item.desc)
                setSelectedCategory(item.category)

                selectedLocation = item.location
                renderLocation()

                images.clear()
                removedExistingUrls.clear()
                item.imageUrls.forEach { url -> images.add(EditableImage.Existing(url)) }

                imagesAdapter.notifyDataSetChanged()
                updateImagesUi()
            }
            .addOnFailureListener {
                Snackbar.make(binding.root, "Failed to load item", Snackbar.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
    }

    private fun setSelectedCategory(category: String) {
        val group = binding.chipGroupCategory
        for (i in 0 until group.childCount) {
            val v = group.getChildAt(i)
            if (v is com.google.android.material.chip.Chip && v.text?.toString() == category) {
                v.isChecked = true
                return
            }
        }
    }

    private fun renderLocation() {
        val loc = selectedLocation
        binding.tvLocation.text =
            if (loc == null) "No location selected"
            else if (loc.label.isNotBlank()) loc.label
            else "(${loc.lat.format(5)}, ${loc.lng.format(5)})"
    }

    private fun getSelectedCategory(): String? {
        val checkedId = binding.chipGroupCategory.checkedChipId
        if (checkedId == View.NO_ID) return null
        val chip = binding.chipGroupCategory.findViewById<com.google.android.material.chip.Chip>(checkedId)
        return chip?.text?.toString()
    }

    private fun setupLocationResultListener() {
        parentFragmentManager.setFragmentResultListener(
            "pick_location",
            viewLifecycleOwner
        ) { _, bundle ->
            val lat = bundle.getDouble("lat")
            val lng = bundle.getDouble("lng")
            val label = bundle.getString("label").orEmpty()

            selectedLocation = ItemLocation(lat = lat, lng = lng, label = label)
            renderLocation()
            updateSaveEnabled()
        }
    }

    private fun Double.format(decimals: Int): String = "%.${decimals}f".format(this)

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putStringArrayList(
            KEY_EXISTING_URLS,
            ArrayList(images.mapNotNull { (it as? EditableImage.Existing)?.url })
        )
        outState.putParcelableArrayList(
            KEY_NEW_URIS,
            ArrayList(images.mapNotNull { (it as? EditableImage.New)?.uri })
        )
        outState.putStringArrayList(KEY_REMOVED_URLS, ArrayList(removedExistingUrls))

        val loc = selectedLocation
        if (loc != null) {
            outState.putDouble(KEY_LOC_LAT, loc.lat)
            outState.putDouble(KEY_LOC_LNG, loc.lng)
            outState.putString(KEY_LOC_LABEL, loc.label)
        }

        outState.putString(KEY_EDIT_ITEM_ID, editItemId)
        outState.putBoolean(KEY_IS_EDIT_MODE, isEditMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val KEY_LOC_LAT = "key_loc_lat"
        private const val KEY_LOC_LNG = "key_loc_lng"
        private const val KEY_LOC_LABEL = "key_loc_label"
        private const val KEY_EDIT_ITEM_ID = "key_edit_item_id"
        private const val KEY_IS_EDIT_MODE = "key_is_edit_mode"
        private const val KEY_EXISTING_URLS = "key_existing_urls"
        private const val KEY_NEW_URIS = "key_new_uris"
        private const val KEY_REMOVED_URLS = "key_removed_urls"
    }
}

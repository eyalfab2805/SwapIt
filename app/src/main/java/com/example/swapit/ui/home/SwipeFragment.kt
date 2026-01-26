package com.example.swapit.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.swapit.R
import com.example.swapit.data.Categories
import com.example.swapit.data.ChatRepository
import com.example.swapit.data.ItemRepository
import com.example.swapit.databinding.FragmentSwipeBinding
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlin.math.abs
import kotlin.math.min

class SwipeFragment : Fragment() {

    private var _binding: FragmentSwipeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SwipeViewModel by viewModels()

    private var startX = 0f
    private var screenWidth = 0
    private var isSwiping = false

    private val locationClient by lazy {
        LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    private val requestLocationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (!granted) {
                Snackbar.make(binding.root, "Location permission is required for distance filter", Snackbar.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSwipeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.root.post { screenWidth = binding.root.width }

        binding.btnFilters.setOnClickListener {
            SwipeFiltersBottomSheet(
                categories = Categories.all,
                onNeedsLocation = { onReady -> ensureLocationThen(onReady) }
            ).show(childFragmentManager, "SwipeFilters")
        }

        binding.itemCard.setOnClickListener {
            if (isSwiping) return@setOnClickListener

            val itemId = viewModel.currentItem.value?.id ?: return@setOnClickListener
            if (itemId.isBlank()) return@setOnClickListener

            findNavController().navigate(
                R.id.itemDetailsFragment,
                bundleOf("itemId" to itemId)
            )
        }

        viewModel.currentItem.observe(viewLifecycleOwner) { item ->
            binding.tvItemTitle.text = item.title
            binding.tvItemDesc.text = item.desc
            binding.tvItemCategory.text = item.category
            binding.tvItemLocation.text = item.locationLabel

            binding.ivItemImage.load(item.imageUrls.firstOrNull()) {
                crossfade(true)
            }

            resetCard()
        }

        binding.ivLike.setOnClickListener { animateSwipeAndCommit("like") }
        binding.ivDislike.setOnClickListener { animateSwipeAndCommit("dislike") }

        binding.itemCard.setOnTouchListener { v, event ->
            when (event.actionMasked) {

                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    isSwiping = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startX
                    if (abs(dx) > 20) isSwiping = true

                    binding.itemCard.translationX = dx
                    binding.itemCard.rotation = (dx / screenWidth) * 20f
                    binding.tvSwipeOverlay.text = if (dx > 0) "LIKE" else "NOPE"
                    binding.tvSwipeOverlay.alpha = min(1f, abs(dx) / (screenWidth * 0.25f))
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val dx = binding.itemCard.translationX
                    val threshold = screenWidth * 0.25f

                    if (abs(dx) > threshold) {
                        val action = if (dx > 0) "like" else "dislike"
                        animateSwipeAndCommit(action)
                    } else {
                        binding.itemCard.animate()
                            .translationX(0f)
                            .rotation(0f)
                            .setDuration(150)
                            .start()

                        binding.tvSwipeOverlay.animate()
                            .alpha(0f)
                            .setDuration(150)
                            .start()

                        if (!isSwiping) v.performClick()
                    }
                    true
                }

                else -> false
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun ensureLocationThen(onReady: (lat: Double, lng: Double) -> Unit) {
        if (!hasLocationPermission()) {
            requestLocationPermission.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }

        try {
            locationClient.lastLocation
                .addOnSuccessListener { loc ->
                    if (loc == null) {
                        Snackbar.make(binding.root, "Couldn't get current location. Try again.", Snackbar.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }
                    onReady(loc.latitude, loc.longitude)
                }
                .addOnFailureListener {
                    Snackbar.make(binding.root, "Couldn't get current location.", Snackbar.LENGTH_SHORT).show()
                }
        } catch (se: SecurityException) {
            Snackbar.make(binding.root, "Location permission is required for distance filter", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun animateSwipeAndCommit(action: String) {
        if (screenWidth == 0) return

        val direction = if (action == "like") 1 else -1

        binding.tvSwipeOverlay.text = if (action == "like") "LIKE" else "NOPE"
        binding.tvSwipeOverlay.alpha = 1f

        binding.itemCard.animate()
            .translationX(direction * (screenWidth * 1.2f))
            .rotation(direction * 20f)
            .setDuration(200)
            .withEndAction {
                onSwipe(action)
                resetCard()
            }
            .start()
    }

    private fun resetCard() {
        binding.itemCard.translationX = 0f
        binding.itemCard.rotation = 0f
        binding.tvSwipeOverlay.alpha = 0f
        isSwiping = false
    }

    private fun onSwipe(action: String) {
        val itemUi = viewModel.currentItem.value ?: return
        val itemId = itemUi.id
        if (itemId.isBlank()) return

        ItemRepository.swipe(itemId, action)

        if (action != "like") {
            viewModel.dislike()
            return
        }

        val ownerUid = itemUi.ownerUid
        val ownerNick = itemUi.ownerNickname.ifBlank { "User" }

        if (ownerUid.isBlank()) {
            Snackbar.make(binding.root, "Missing owner info", Snackbar.LENGTH_SHORT).show()
            viewModel.like()
            return
        }

        val itemTitle = itemUi.title
        val itemImageUrl = itemUi.imageUrls.firstOrNull().orEmpty()

        ChatRepository.createOrGetConversation(
            itemId = itemId,
            itemTitle = itemTitle,
            itemImageUrl = itemImageUrl,
            ownerUid = ownerUid,
            ownerNickname = ownerNick,
            onSuccess = { convId ->
                showComposeMessageSheet(ownerNick, convId)
                viewModel.like()
            },
            onFailure = {
                Snackbar.make(binding.root, "Chat creation failed (rules/path)", Snackbar.LENGTH_SHORT).show()
                viewModel.like()
            }
        )
    }

    private fun showComposeMessageSheet(ownerNickname: String, conversationId: String) {
        val dialog = BottomSheetDialog(requireContext())
        val v = layoutInflater.inflate(R.layout.bottom_sheet_compose_message, null, false)

        val tvTitle = v.findViewById<TextView>(R.id.tvTitle)
        val etMessage = v.findViewById<TextInputEditText>(R.id.etMessage)
        val btnSkip = v.findViewById<MaterialButton>(R.id.btnSkip)
        val btnSend = v.findViewById<MaterialButton>(R.id.btnSend)

        tvTitle.text = "Message to $ownerNickname"

        btnSkip.setOnClickListener { dialog.dismiss() }

        btnSend.setOnClickListener {
            val text = etMessage.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) {
                dialog.dismiss()
                return@setOnClickListener
            }

            btnSend.isEnabled = false
            ChatRepository.sendMessage(
                conversationId = conversationId,
                text = text,
                onSuccess = { dialog.dismiss() },
                onFailure = { dialog.dismiss() }
            )
        }

        dialog.setContentView(v)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

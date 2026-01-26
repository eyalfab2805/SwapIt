package com.example.swapit.ui.myitems

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.swapit.R
import com.example.swapit.data.Item
import com.example.swapit.data.ItemRepository
import com.example.swapit.databinding.FragmentMyItemsBinding
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MyItemsFragment : Fragment() {

    private var _binding: FragmentMyItemsBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseDatabase.getInstance().reference
    private val itemsRef = db.child("items")

    private var userItemsRef: DatabaseReference? = null
    private var userItemsListener: ValueEventListener? = null

    private val items = mutableListOf<Item>()
    private lateinit var adapter: MyItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        adapter = MyItemsAdapter(
            items = items,
            onItemClick = { itemId ->
                findNavController().navigate(
                    R.id.itemDetailsFragment,
                    bundleOf("itemId" to itemId)
                )
            },
            onEditClick = { item ->
                findNavController().navigate(
                    R.id.addItemFragment,
                    bundleOf("itemId" to item.id)
                )
            },
            onDeleteClick = { item ->
                confirmDelete(item)
            }
        )

        binding.recyclerView.adapter = adapter

        if (binding.root.findViewById<View?>(R.id.btnEmptyAdd) != null) {
            binding.btnEmptyAdd.setOnClickListener {
                findNavController().navigate(R.id.addItemFragment)
            }
        }

        renderEmptyState()
        startListening()
    }

    private fun startListening() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        userItemsRef = db.child("users").child(uid).child("userItems")

        userItemsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val itemIds = snapshot.children.mapNotNull { it.key }.distinct()

                if (itemIds.isEmpty()) {
                    items.clear()
                    adapter.notifyDataSetChanged()
                    renderEmptyState()
                    return
                }

                val tasks = itemIds.map { itemId -> itemsRef.child(itemId).get() }

                Tasks.whenAllComplete(tasks)
                    .addOnSuccessListener { results ->
                        val list = results.mapNotNull { t ->
                            val snap = t.result as? DataSnapshot
                            snap?.getValue(Item::class.java)?.copy(id = snap.key ?: "")
                        }.sortedByDescending { it.createdAt }

                        items.clear()
                        items.addAll(list)
                        adapter.notifyDataSetChanged()
                        renderEmptyState()
                    }
                    .addOnFailureListener {
                        items.clear()
                        adapter.notifyDataSetChanged()
                        renderEmptyState()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                items.clear()
                adapter.notifyDataSetChanged()
                renderEmptyState()
            }
        }

        userItemsRef!!.addValueEventListener(userItemsListener!!)
    }

    private fun renderEmptyState() {
        val isEmpty = items.isEmpty()
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE

        if (binding.root.findViewById<View?>(R.id.emptyState) != null) {
            binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }

    private fun confirmDelete(item: Item) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete item?")
            .setMessage("This will permanently delete the item and its photos.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                ItemRepository.deleteItem(
                    item = item,
                    onSuccess = {},
                    onFailure = {}
                )
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userItemsListener?.let { l -> userItemsRef?.removeEventListener(l) }
        userItemsListener = null
        userItemsRef = null
        _binding = null
    }
}

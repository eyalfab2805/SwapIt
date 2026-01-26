package com.example.swapit.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.swapit.data.ItemRepository
import com.example.swapit.databinding.FragmentProfileBinding
import com.example.swapit.ui.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseDatabase.getInstance().reference

    private var profileListener: ValueEventListener? = null
    private var profileRef: DatabaseReference? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val user = auth.currentUser
        if (user == null || user.isAnonymous || user.email.isNullOrBlank()) {
            goToAuth()
            return
        }

        startProfileListener()

        binding.btnSave.setOnClickListener {
            val uid = auth.currentUser?.uid ?: return@setOnClickListener
            val nickname = binding.etNickname.text?.toString()?.trim().orEmpty()

            if (nickname.isBlank()) {
                toast("Nickname cannot be empty")
                return@setOnClickListener
            }

            db.child("users")
                .child(uid)
                .child("profile")
                .child("nickname")
                .setValue(nickname)
                .addOnCompleteListener {
                    toast(if (it.isSuccessful) "Saved" else "Save failed")
                }
        }

        binding.btnLogout.setOnClickListener {
            ItemRepository.stop()
            auth.signOut()
            goToAuth()
        }
    }

    private fun startProfileListener() {
        val uid = auth.currentUser?.uid ?: return

        profileRef = db.child("users").child(uid).child("profile")

        profileListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nickname = snapshot.child("nickname").getValue(String::class.java).orEmpty()
                val email = snapshot.child("email").getValue(String::class.java).orEmpty()
                val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L

                binding.tvEmail.text = "Email: ${email.ifBlank { "-" }}"
                binding.tvCreatedAt.text = "Created: ${formatTs(createdAt)}"

                if (nickname.isNotBlank()) {
                    binding.tvNickname.text = nickname
                    binding.tvNickname.visibility = View.VISIBLE
                    binding.etNickname.visibility = View.GONE
                    binding.btnSave.visibility = View.GONE
                } else {
                    binding.etNickname.visibility = View.VISIBLE
                    binding.btnSave.visibility = View.VISIBLE
                    binding.tvNickname.visibility = View.GONE
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }

        profileRef!!.addValueEventListener(profileListener!!)
    }

    private fun goToAuth() {
        val intent = Intent(requireContext(), AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        profileListener?.let { l -> profileRef?.removeEventListener(l) }
        profileListener = null
        profileRef = null
        _binding = null
        super.onDestroyView()
    }

    private fun formatTs(ts: Long): String {
        if (ts <= 0L) return "-"
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(ts))
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}

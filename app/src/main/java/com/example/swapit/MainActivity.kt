package com.example.swapit

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.swapit.data.ChatRepository
import com.example.swapit.data.ItemRepository
import com.example.swapit.databinding.ActivityMainBinding
import com.example.swapit.ui.auth.AuthActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var unreadListener: ValueEventListener? = null
    private var hasUi = false

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        val loggedIn = user != null && !user.isAnonymous

        if (!loggedIn) {
            stopUnreadListener()
            ItemRepository.stop()
            goToAuth()
            return@AuthStateListener
        }

        ItemRepository.start()
        if (hasUi) startUnreadListener()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = auth.currentUser
        val loggedIn = user != null && !user.isAnonymous
        if (!loggedIn) {
            goToAuth()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        hasUi = true

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        binding.bottomNav.setupWithNavController(navController)

        ItemRepository.start()
        startUnreadListener()
    }

    private fun goToAuth() {
        val i = Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(i)
        finish()
    }

    private fun startUnreadListener() {
        if (unreadListener != null) return

        unreadListener = ChatRepository.listenUnreadCount { count ->
            val badge = binding.bottomNav.getOrCreateBadge(R.id.conversationsFragment)
            badge.isVisible = count > 0
            badge.number = count
        }
    }

    private fun stopUnreadListener() {
        unreadListener?.let { ChatRepository.stopListeningUnreadCount(it) }
        unreadListener = null
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authListener)
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authListener)
    }
}

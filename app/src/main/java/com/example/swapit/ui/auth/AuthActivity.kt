package com.example.swapit.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.swapit.MainActivity
import com.example.swapit.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class AuthActivity : AppCompatActivity() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }

    private lateinit var loginContainer: View
    private lateinit var registerContainer: View

    private lateinit var loginEmail: TextInputEditText
    private lateinit var loginPassword: TextInputEditText
    private lateinit var btnLogin: View
    private lateinit var tvRegister: View

    private lateinit var regNickname: TextInputEditText
    private lateinit var regEmail: TextInputEditText
    private lateinit var regPassword: TextInputEditText
    private lateinit var regConfirm: TextInputEditText
    private lateinit var btnRegister: View
    private lateinit var tvLogin: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val user = auth.currentUser
        if (user != null && !user.isAnonymous && !user.email.isNullOrBlank()) {
            goToMain()
            return
        }

        if (user != null && user.isAnonymous) {
            auth.signOut()
        }

        setContentView(R.layout.activity_auth)

        loginContainer = findViewById(R.id.loginContainer)
        registerContainer = findViewById(R.id.registerContainer)

        loginEmail = loginContainer.findViewById(R.id.etEmail)
        loginPassword = loginContainer.findViewById(R.id.etPassword)
        btnLogin = loginContainer.findViewById(R.id.btnLogin)
        tvRegister = loginContainer.findViewById(R.id.tvRegister)

        regNickname = registerContainer.findViewById(R.id.etNickname)
        regEmail = registerContainer.findViewById(R.id.etEmail)
        regPassword = registerContainer.findViewById(R.id.etPassword)
        regConfirm = registerContainer.findViewById(R.id.etConfirmPassword)
        btnRegister = registerContainer.findViewById(R.id.btnRegister)
        tvLogin = registerContainer.findViewById(R.id.tvLogin)

        showLogin()

        tvRegister.setOnClickListener { showRegister() }
        tvLogin.setOnClickListener { showLogin() }

        btnLogin.setOnClickListener {
            val email = loginEmail.text?.toString()?.trim().orEmpty()
            val password = loginPassword.text?.toString().orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                toast("Please fill all fields")
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { goToMain() }
                .addOnFailureListener { e ->
                    toast(e.message ?: "Login failed")
                }
        }

        btnRegister.setOnClickListener {
            val nickname = regNickname.text?.toString()?.trim().orEmpty()
            val email = regEmail.text?.toString()?.trim().orEmpty()
            val password = regPassword.text?.toString().orEmpty()
            val confirm = regConfirm.text?.toString().orEmpty()

            if (nickname.isEmpty() || email.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                toast("All fields are required")
                return@setOnClickListener
            }

            if (password != confirm) {
                toast("Passwords do not match")
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid
                    val authedEmail = auth.currentUser?.email

                    if (uid.isNullOrBlank() || authedEmail.isNullOrBlank()) {
                        toast("Register failed")
                        return@addOnSuccessListener
                    }

                    val profile = hashMapOf<String, Any>(
                        "nickname" to nickname,
                        "email" to authedEmail,
                        "createdAt" to ServerValue.TIMESTAMP
                    )

                    db.child("users")
                        .child(uid)
                        .child("profile")
                        .setValue(profile)
                        .addOnSuccessListener { goToMain() }
                        .addOnFailureListener { e ->
                            toast(e.message ?: "Register failed")
                        }
                }
                .addOnFailureListener { e ->
                    if (e is FirebaseAuthUserCollisionException) {
                        toast("This email is already registered. Please login.")
                        showLogin()
                    } else {
                        toast(e.message ?: "Register failed")
                    }
                }
        }
    }

    private fun showLogin() {
        loginContainer.visibility = View.VISIBLE
        registerContainer.visibility = View.GONE
    }

    private fun showRegister() {
        loginContainer.visibility = View.GONE
        registerContainer.visibility = View.VISIBLE
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

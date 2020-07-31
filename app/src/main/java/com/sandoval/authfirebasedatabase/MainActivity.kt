package com.sandoval.authfirebasedatabase

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.huawei.agconnect.auth.*
import com.huawei.agconnect.auth.VerifyCodeSettings.ACTION_REGISTER_LOGIN
import com.huawei.hmf.tasks.OnFailureListener
import com.huawei.hmf.tasks.OnSuccessListener
import com.huawei.hmf.tasks.TaskExecutors
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    private val huaweiAuth = AGConnectAuth.getInstance()
    private var mDataBase: DatabaseReference? = null
    private var email: String? = null
    private var name: String? = null
    private var password: String? = null
    private var confirmPassword: String? = null
    private var verCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        buttonSendCode.setOnClickListener {
            sendCodeVerification()
        }

        buttonSignUp.setOnClickListener {
            signUpWithEmail()
        }

    }

    private fun sendCodeVerification() {
        email = emailRegister.text.toString()
        if (email.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "Email is Mandatory",
                Toast.LENGTH_LONG
            ).show()
        } else {
            val settings = VerifyCodeSettings.newBuilder()
                .action(ACTION_REGISTER_LOGIN) //ACTION_REGISTER_LOGIN/ACTION_RESET_PASSWORD
                .sendInterval(30) // Minimum sending interval, ranging from 30s to 120s.
                .locale(Locale.getDefault()) // Language in which a verification code is sent, which is optional. The default value is Locale.getDefault.
                .build()

            val task = EmailAuthProvider.requestVerifyCode(email, settings)
            task.addOnSuccessListener(
                TaskExecutors.uiThread(),
                OnSuccessListener {

                    Toast.makeText(
                        this,
                        "Check your email to get the verification code",
                        Toast.LENGTH_LONG
                    ).show()

                }).addOnFailureListener(
                TaskExecutors.uiThread(),
                OnFailureListener {
                    Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                })
        }
    }

    private fun firebaseDatabase() {
        val currentUserId = huaweiAuth.currentUser
        val userId = currentUserId!!.uid

        mDataBase = FirebaseDatabase.getInstance().reference
            .child("Users").child(userId)
        val userObject = HashMap<String, String>()

        userObject["display_name"] = name!!.trim()
        userObject["status"] = "Hello There"
        userObject["email"] = email!!.trim()
        userObject["image"] = "default"
        userObject["thumb_image"] = "default"

        huaweiAuth.currentUser?.let { user ->
            val userUpdate = ProfileRequest.Builder()
            userUpdate.setDisplayName(name)
            userObject["display_name"] = name!!
            user.updateProfile(userUpdate.build()).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(
                        this,
                        "Update profile succesfull!!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Oops something went wrong!",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
        }

        mDataBase!!.setValue(userObject).addOnCompleteListener { task: Task<Void> ->
            if (task.isSuccessful) {
                Log.d("Successful:", "true")
            } else {
                Toast.makeText(
                    this,
                    "Oops something went wrong!",
                    Toast.LENGTH_LONG
                )
                    .show()
                Log.e("Error: ", task.exception!!.message!!)
            }
        }
    }

    private fun signUpWithEmail() {

        email = emailRegister.text.toString()
        password = passwordRegister.text.toString()
        confirmPassword = passwordConfirmRegister.text.toString()
        verCode = verifyCodeRegister.text.toString()
        name = nameRegister.text.toString()
        if (email.isNullOrEmpty() || password.isNullOrEmpty() || confirmPassword.isNullOrEmpty() || name.isNullOrEmpty() || verCode.isNullOrEmpty()) {
            Toast.makeText(
                this,
                "All fields are mandatory!",
                Toast.LENGTH_LONG
            ).show()
        } else {
            val emailUser =
                EmailUser.Builder().setEmail(email).setVerifyCode(verCode).setPassword(password)
                    // Optional. If this parameter is set, the current user has created a password and can use the password to sign in.
                    // If this parameter is not set, the user can only sign in using a verification code.
                    .build()
            AGConnectAuth.getInstance().createUser(emailUser)
                .addOnCompleteListener { auth ->
                    if (auth.isSuccessful) {
                        firebaseDatabase()
                    } else {
                        Toast.makeText(
                            this,
                            auth.exception.message,
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
        }

    }
}
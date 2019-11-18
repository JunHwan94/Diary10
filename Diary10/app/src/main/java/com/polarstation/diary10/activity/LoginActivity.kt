package com.polarstation.diary10.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.facebook.*
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.polarstation.diary10.R
import com.polarstation.diary10.databinding.ActivityLoginBinding
import com.polarstation.diary10.model.UserModel
import com.polarstation.diary10.util.NetworkStatus
import io.reactivex.Observable
import java.security.MessageDigest
import kotlin.experimental.and

class LoginActivity : BaseActivity(), View.OnClickListener {
    private lateinit var binding : ActivityLoginBinding
    private lateinit var authInstance : FirebaseAuth
    private lateinit var authStateListener : FirebaseAuth.AuthStateListener
    private lateinit var dbInstance : FirebaseDatabase
    private lateinit var uid : String

    private var netStat : Int? = null
    private lateinit var callbackManager : CallbackManager

    private val SIGN_IN_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        netStat = NetworkStatus.getConnectivityStatus(this)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            FirebaseApp.initializeApp(this)
            dbInstance = FirebaseDatabase.getInstance()
            authInstance = FirebaseAuth.getInstance()
            authStateListener = FirebaseAuth.AuthStateListener { auth ->
                val user = auth.currentUser
                if(user != null){
                    Log.d("AuthStateListener","User not null")
                    val intent = Intent(applicationContext, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                }else
                    auth.signOut()
            }

            binding.loginActivityGoogleLoginButton.setOnClickListener(this)
            setFacebookLogIn()
            ActivityCompat.requestPermissions(this, arrayOf( Manifest.permission.READ_EXTERNAL_STORAGE ), 1)
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    override fun onStart(){
        super.onStart()
        authInstance.addAuthStateListener(authStateListener)
    }

    override fun onStop(){
        super.onStop()
        authInstance.removeAuthStateListener(authStateListener)
    }

    private fun googleSignIn() {
        netStat = NetworkStatus.getConnectivityStatus(this)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            val gso =
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(getString(R.string.default_web_client_id))
                            .requestEmail()
                            .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            val intent = googleSignInClient.signInIntent
            startActivityForResult(intent, SIGN_IN_REQUEST_CODE)
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        Log.d("FunFAuthWithGmail", "Run Authorizing")
        netStat = NetworkStatus.getConnectivityStatus(this)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            setViewWhenLoading()
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val userName: String = account.displayName.toString()
            val profileImageUrl = account.photoUrl.toString()
            val email : String = account.email.toString()
            processCredential(credential, userName, profileImageUrl, email)
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    private fun processCredential(credential: AuthCredential, userName: String, profileImageUrl: String, email: String) {
        Log.d("Fun procCred", "Process Credential")
        netStat = NetworkStatus.getConnectivityStatus(this)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            authInstance.signInWithCredential(credential)
                    .addOnCompleteListener(this) { task ->
                        if(task.isSuccessful){
                            checkUid(userName, profileImageUrl, email)
                        }else{
                            setViewWhenDone()
                            Toast.makeText(baseContext, getString(R.string.auth_failed), Toast.LENGTH_SHORT).show()
                        }
                    }
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    private fun checkUid(userName: String?, profileImageUrl: String, email: String) {
        val hash = createHashValue(email)
        netStat = NetworkStatus.getConnectivityStatus(this)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            uid = authInstance.currentUser!!.uid
            dbInstance.reference.child(getString(R.string.fdb_users)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if(dataSnapshot.childrenCount.equals(0)){
                                addUserToDB(userName, profileImageUrl, hash)
                                val userProfileChangeRequest = UserProfileChangeRequest.Builder().setDisplayName(userName).build()
                                authInstance.currentUser!!.updateProfile(userProfileChangeRequest)
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                            Toast.makeText(baseContext, getString(R.string.account_save_failed), Toast.LENGTH_LONG).show()
                        }
                    })
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    private fun addUserToDB(userName: String?, profileImageUrl: String, hash: String) {
        val userModel =
                UserModel.Builder()
                        .setUserName(userName)
                        .setProfileImageUrl(profileImageUrl)
                        .setUid(uid)
                        .setHash(hash)
                        .build()

        netStat = NetworkStatus.getConnectivityStatus(this)
        if (netStat == NetworkStatus.TYPE_CONNECTED) {
            dbInstance.reference.child(getString(R.string.fdb_users)).child(uid).setValue(userModel)
                    .addOnSuccessListener { // aVoid -> 람다식에서 변수가 1개일 때 그것을 쓰지 않으면 지워버림
                        authInstance.addAuthStateListener(authStateListener)
                        Toast.makeText(applicationContext, getString(R.string.auth_success), Toast.LENGTH_SHORT).show()
                    }
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    private fun setFacebookLogIn() {
        netStat = NetworkStatus.getConnectivityStatus(this)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            callbackManager = CallbackManager.Factory.create()
            binding.loginActivityFacebookLoginButton.setReadPermissions(getString(R.string.email))
            binding.loginActivityFacebookLoginButton.setLoginText(getString(R.string.log_in))
            binding.loginActivityFacebookLoginButton.registerCallback(callbackManager, object : FacebookCallback<LoginResult>{ // 인터페이스 구현 시 앞에 object :
                override fun onSuccess(result: LoginResult) {
                    checkHashOfEmail(result)
                }

                override fun onCancel() {}

                override fun onError(error: FacebookException?) {
                    Toast.makeText(baseContext, getString(R.string.auth_failed), Toast.LENGTH_LONG).show()
                    Log.w("LoginActivity", error.toString())
                }
            })
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    private fun handleFacebookAccessToken(accessToken: AccessToken) {
        netStat = NetworkStatus.getConnectivityStatus(this)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            val credential = FacebookAuthProvider.getCredential(accessToken.token)
            val name = Profile.getCurrentProfile().name
            val profileImageUrl = Profile.getCurrentProfile().getProfilePictureUri(300, 300).toString()

            setViewWhenLoading()
            processCredential(credential, name, profileImageUrl, "")
        }
    }

    private fun checkHashOfEmail(loginResult: LoginResult) {
        val request = GraphRequest.newMeRequest(loginResult.accessToken) { _, response -> // object가 _로 표현
            val email = response.jsonObject.getString(getString(R.string.email))
            val hash = createHashValue(email)
            dbInstance.reference.child(getString(R.string.fdb_users)).orderByChild(getString(R.string.fdb_hash)).equalTo(hash)
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if(0 < dataSnapshot.childrenCount) {
                                Toast.makeText(baseContext, getString(R.string.already_have_account) + dataSnapshot.childrenCount, Toast.LENGTH_SHORT).show()

                                setViewWhenDone()
                                LoginManager.getInstance().logOut()
                            }else handleFacebookAccessToken(loginResult.accessToken)
                        }
                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }

        val parameters = Bundle()
        parameters.putString(getString(R.string.fields), getString(R.string.email))
        request.parameters = parameters
        request.executeAsync()
    }

    private fun createHashValue(email: String): String {
//        TODO("검사하기")
        var hash = ""
        val sh = MessageDigest.getInstance(getString(R.string.sha_256))
        val byteData = sh.digest()
        val sb = StringBuffer()

        Observable.fromIterable(byteData.toList()).subscribe { byte ->
            sb.append(((byte and 0xff.toByte()) + 0x100.toByte()).toString().substring(1))
        }
        hash = sb.toString()
        return when(email == ""){
            true -> email
            false -> hash
        }
    }

    private fun setViewWhenLoading() {
        binding.loginActivityProgressBar.visibility = View.VISIBLE
        binding.loginActivityFacebookLoginButton.isEnabled = false
        binding.loginActivityGoogleLoginButton.isEnabled = false
    }

    private fun setViewWhenDone(){
        binding.loginActivityProgressBar.visibility = View.INVISIBLE
        binding.loginActivityFacebookLoginButton.isEnabled = true
        binding.loginActivityGoogleLoginButton.isEnabled = true
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.loginActivity_googleLoginButton -> googleSignIn()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("LoginActivity Result", "Got result")
        callbackManager.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == SIGN_IN_REQUEST_CODE) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult < ApiException >(ApiException::class.java)
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException) {
                Log.w("LoginActivity", "${e.statusCode}")
            }
        }
    }
}
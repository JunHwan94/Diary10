package com.polarstation.diary10.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.polarstation.diary10.R
import com.polarstation.diary10.databinding.ActivityEditAccountBinding
import com.polarstation.diary10.fragment.*
import com.polarstation.diary10.util.FontUtil
import com.polarstation.diary10.util.NetworkStatus
import gun0912.tedkeyboardobserver.BaseKeyboardObserver
import gun0912.tedkeyboardobserver.TedKeyboardObserver
import io.reactivex.Observable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.HashMap

class EditAccountActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityEditAccountBinding
    private val strInstance: () -> FirebaseStorage = { FirebaseStorage.getInstance() }
    private val dbInstance: () -> FirebaseDatabase = { FirebaseDatabase.getInstance() }
    private val authInstance: () -> FirebaseAuth = { FirebaseAuth.getInstance() }
    private val uid: () -> String = { authInstance().currentUser!!.uid }
    private val netStat: () -> Int = { NetworkStatus.getConnectivityStatus(this) }
    private var uriOptional: Optional<Uri> = Optional.empty()
    private lateinit var imageUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_account)
        FontUtil.setGlobalFont(binding.root)

        val accountImageUrl = intent!!.getStringExtra(URL_KEY)
        val name = intent!!.getStringExtra(NAME_KEY)
        val comment = intent!!.getStringExtra(COMMENT_KEY)

        Glide.with(baseContext)
                .load(accountImageUrl)
                .apply(RequestOptions().circleCrop())
                .into(binding.editAccountActivityProfileImageView)
        binding.editAccountActivityNameEditText.setText(name)
        binding.editAccountActivityCommentEditText.setText(comment)

        val clickableViewList = listOf(binding.editAccountActivityProfileImageView, binding.editAccountActivityCloseButton, binding.editAccountActivitySaveButton)
        Observable.fromIterable(clickableViewList).subscribe { it.setOnClickListener(this) }.dispose()
        TedKeyboardObserver(this).listen(object : BaseKeyboardObserver.OnKeyboardListener{
            override fun onKeyboardChange(isShow: Boolean) {
                when{
                    !isShow && binding.editAccountActivityNameEditText.isFocused -> binding.editAccountActivityNameEditText.clearFocus()
                    !isShow && binding.editAccountActivityCommentEditText.isFocused -> binding.editAccountActivityCommentEditText.clearFocus()
                }
            }
        })
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.editAccountActivity_closeButton -> finish()
            R.id.editAccountActivity_saveButton -> {
                if(netStat() == NetworkStatus.TYPE_CONNECTED){
                    updateUser()
                    val resultIntent = Intent().apply { putExtra(NAME_KEY, binding.editAccountActivityNameEditText.text.toString())
                            putExtra(COMMENT_KEY, binding.editAccountActivityCommentEditText.text.toString()) }
                    if(uriOptional != Optional.empty<Uri>()) updateProfileImage(resultIntent)
                    setResultAndFinish(resultIntent)
                }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
            }
            R.id.editAccountActivity_profileImageView ->
                startActivityForResult(Intent(Intent.ACTION_PICK).apply{ type = MediaStore.Images.Media.CONTENT_TYPE }, PICK_FROM_ALBUM_CODE)
        }
    }

    private fun updateUser(){
        val name = binding.editAccountActivityNameEditText.text.toString()
        if(name == "") Toast.makeText(baseContext, getString(R.string.write_name), Toast.LENGTH_SHORT).show()
        else {
            fun updateUser(map: HashMap<String, Any>) = dbInstance().reference.child(getString(R.string.fdb_users)).child(uid()).updateChildren(map)
            updateUser(HashMap<String, Any>().apply { put(getString(R.string.fdb_user_name), name) })
            updateUser(HashMap<String, Any>().apply { put(getString(R.string.fdb_comment), binding.editAccountActivityCommentEditText.text.toString()) })
            val userProfileChangeRequest = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            authInstance().currentUser!!.updateProfile(userProfileChangeRequest)
        }
    }

    private fun updateProfileImage(resultIntent: Intent){
        setViewWhenUploading()
        strInstance().reference.child(getString(R.string.fstr_user_images)).child(uid()).putFile(uriOptional.get())
                .addOnSuccessListener {
                    strInstance().reference.child(getString(R.string.fstr_user_images)).child(uid()).downloadUrl
                            .addOnSuccessListener {
                                imageUrl = it.toString()
                                resultIntent.putExtra(URI_KEY, uriOptional.get())

                                val map = HashMap<String, Any>().apply { put(getString(R.string.fdb_profile_image_url), imageUrl) }
                                dbInstance().reference.child(getString(R.string.fdb_users)).child(uid()).updateChildren(map)
                                        .addOnSuccessListener {
                                            setResult(Activity.RESULT_OK, resultIntent)
                                            finish()
                                        }
                            }}
    }

    private fun setViewWhenUploading() {
        val viewList = listOf(binding.editAccountActivityProgressLayout, binding.editAccountActivityConstraintLayout, binding.editAccountActivityCloseButton, binding.editAccountActivitySaveButton,
                binding.editAccountActivityProfileImageView, binding.editAccountActivityCommentEditText, binding.editAccountActivityNameEditText)
        GlobalScope.launch {
            sequence{ yieldAll(viewList) }
                    .forEach { when(it.id){
                        R.id.editAccountActivity_progressLayout -> it.visibility = View.VISIBLE
                        else -> it.isEnabled = false
                    } }
        }
    }

    private fun setResultAndFinish(resultIntent: Intent){
        binding.editAccountActivityProgressBar.visibility = View.INVISIBLE
        resultIntent.putExtra(URI_KEY, "")
        setResult(Activity.RESULT_OK, resultIntent)
        finish()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_FROM_ALBUM_CODE && resultCode == Activity.RESULT_OK){
            uriOptional = Optional.of(data!!.data!!)
            Glide.with(baseContext)
                    .load(data.data!!)
                    .apply(RequestOptions().circleCrop())
                    .into(binding.editAccountActivityProfileImageView)
        }
    }
}

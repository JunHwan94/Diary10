package com.polarstation.diary10.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.polarstation.diary10.R
import com.polarstation.diary10.databinding.ActivityWriteDiaryBinding
import com.polarstation.diary10.fragment.*
import com.polarstation.diary10.fragment.WriteFragment.Companion.pushPage
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.util.NetworkStatus
import gun0912.tedkeyboardobserver.BaseKeyboardObserver
import gun0912.tedkeyboardobserver.TedKeyboardObserver
import io.reactivex.Observable
import java.util.*
import kotlin.collections.HashMap

class WriteDiaryActivityKt : BaseActivity(), View.OnClickListener {
    private lateinit var binding: ActivityWriteDiaryBinding
    private val strInstance: () -> FirebaseStorage = { FirebaseStorage.getInstance() }
    private val dbInstance: () -> FirebaseDatabase = { FirebaseDatabase.getInstance() }
    private val uid: () -> String = { FirebaseAuth.getInstance().currentUser!!.uid }
    private val netStat: () -> Int = { NetworkStatus.getConnectivityStatus(this) }
    private var isImageChanged: Boolean = false
    private var imageUri: Uri = Uri.parse("")
    private val isCover: (Intent) -> Boolean = { it.getBooleanExtra(IS_COVER_KEY, false) }
    private val isNew: (Intent) -> Boolean = { it.getBooleanExtra(IS_NEW_KEY, false) }
    private val diaryKey: (Intent) -> String = { it.getStringExtra(DIARY_KEY_KEY) }
    private val title : (Intent) -> String = { it.getStringExtra(TITLE_KEY) }
    private val pageKey: (Intent) -> String = { it.getStringExtra(PAGE_KEY_KEY) }
    private val pageCreateTime: (Intent) -> Long = { it.getLongExtra(PAGE_CREATE_TIME_KEY, 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_write_diary)

        if(intent != null)
            setUI()
        Observable.just(binding.writeActivitySaveButton, binding.writeActivityCancelButton, binding.writeActivityChildConstraintLayout)
                .subscribe{ it.setOnClickListener(this) }.dispose()

        TedKeyboardObserver(this).listen(object : BaseKeyboardObserver.OnKeyboardListener{
            override fun onKeyboardChange(isShow: Boolean) {
                if(!isShow) binding.writeActivityEditText.clearFocus()
            }
        })
    }

    private fun setUI() {
        binding.writeActivityTitleTextView.text = title(intent)
        val imageUrl = intent.getStringExtra(IMAGE_URL_KEY)
        when{
            isNew(intent) -> {
                binding.writeActivityGuideImageView.visibility = View.VISIBLE
                binding.writeActivityGuideTextView.visibility = View.VISIBLE
            }
            isCover(intent) -> {
                binding.writeActivityTitleTextView.visibility = View.INVISIBLE
                binding.writeActivityEditText2.visibility = View.INVISIBLE
                binding.writeActivitySwitch.visibility = View.VISIBLE
                binding.writeActivityEditText.apply {
                    text = Editable.Factory().newEditable(title(intent))
                    hint = getString(R.string.write_title)
                    inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    filters = arrayOf(InputFilter.LengthFilter(15))
                }
            }
            else -> {
                // 페이지 수정 공통
                val content = intent.getStringExtra(CONTENT_KEY)
                binding.writeActivityEditText.text =
                        Editable.Factory().newEditable( if(content.contains("\n")) content.split("\n")[0] else content)
                binding.writeActivityEditText2.text =
                        Editable.Factory().newEditable( if(content.contains("\n")) content.split("\n")[1] else "")
                // 사진 없는 페이지 수정인 경우
                if(imageUrl == ""){
                    binding.writeActivityGuideTextView.apply { text = getString(R.string.select_new_image); visibility = View.VISIBLE }
                    binding.writeActivityGuideImageView.visibility = View.VISIBLE
                }
            }
        }
        Glide.with(baseContext)
                .load(imageUrl)
                .apply(RequestOptions().centerCrop())
                .into(binding.writeActivityCoverImageView)
        binding.writeActivityCoverImageView.visibility = View.VISIBLE
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.writeActivity_childConstraintLayout -> startActivityForResult(Intent(Intent.ACTION_PICK).apply { type = MediaStore.Images.Media.CONTENT_TYPE }, PICK_FROM_ALBUM_CODE)
            R.id.writeActivity_cancelButton -> finish()
            R.id.writeActivity_saveButton -> {
                if(netStat() == NetworkStatus.TYPE_CONNECTED) {
                    val firstLine = binding.writeActivityEditText.text.toString()
                    val secondLine = binding.writeActivityEditText2.text.toString()
                    val text = if(secondLine == "") firstLine else "$firstLine\n$secondLine"
                    when{
                        isNew(intent) ->
                            if(binding.writeActivityEditText.text.toString() == "") Toast.makeText(this, getString(R.string.write_content_toast), Toast.LENGTH_SHORT).show()
                            else putPageImageFile(text)
                        isCover(intent) && binding.writeActivityEditText.text.toString() == "" -> Toast.makeText(this, getString(R.string.write_title), Toast.LENGTH_SHORT).show()
                        else -> update(text)
                    }
                }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun update(text: String){
        dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(intent))
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val diaryModel = dataSnapshot.getValue(DiaryModel::class.java)!!
                        val diaryCreateTime = diaryModel.createTime
                        if(isImageChanged)
                            when{
                                isCover(intent) -> putCoverImageFile(text, diaryCreateTime.toString())
                                else -> updatePageImageFile(text, diaryCreateTime.toString())
                            }
                    }

                    override fun onCancelled(p0: DatabaseError) {}
                })
    }

    private fun updatePageImageFile(content: String, diaryCreateTime: String){
        strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryCreateTime).child(pageCreateTime.toString()).putFile(imageUri)
                .addOnSuccessListener {
                    strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryCreateTime).child(pageCreateTime.toString()).downloadUrl
                            .addOnSuccessListener {
                                val map = HashMap<String, Any>().apply{ put(getString(R.string.fdb_image_url), it.toString()) }
                                dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(intent)).child(getString(R.string.fdb_pages)).child(pageKey(intent)).updateChildren(map)
                                updateDatabase(content)
                            }
                }
    }

    private fun putPageImageFile(content: String){
        Log.d("putPageImageFile", "called")
        setViewWhenUploading()
        dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(intent))
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val diaryModel = dataSnapshot.getValue(DiaryModel::class.java)!!
                        val diaryCreateTime = diaryModel.createTime.toString()
                        val timeStamp = Calendar.getInstance().timeInMillis
                        val pageCreateTime = timeStamp.toString()
                        if(isImageChanged)
                            strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryCreateTime).child(pageCreateTime).putFile(imageUri)
                                    .addOnSuccessListener {
                                        strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryCreateTime).child(pageCreateTime).downloadUrl
                                                .addOnSuccessListener {
                                                    pushPage(applicationContext, dbInstance(), content, timeStamp, it.toString(), diaryKey(intent), title(intent), activityOp = Optional.of(this@WriteDiaryActivityKt))
                                                }
                                    }
                        else pushPage(applicationContext, dbInstance(), content, timeStamp, imageUri.toString(), diaryKey(intent), title(intent), activityOp = Optional.of(this@WriteDiaryActivityKt))
                    }

                    override fun onCancelled(p0: DatabaseError) {}
                })
    }

    private fun putCoverImageFile(text: String, diaryCreateTime: String){
        strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryCreateTime).child(uid()).putFile(imageUri)
                .addOnSuccessListener {
                    strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryCreateTime).child(uid()).downloadUrl
                            .addOnSuccessListener {
                                val map = HashMap<String, Any>().apply { put(getString(R.string.fdb_cover_image_url), it.toString()) }
                                dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(intent)).updateChildren(map)
                                updateDatabase(text)
                            }
                }
    }

    private fun updateDatabase(text: String) {
        val map = HashMap<String, Any>()
        if(isCover(intent)){
            map[getString(R.string.fdb_title)] = text
            dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(intent)).updateChildren(map)
                    .addOnSuccessListener {
                        val isPrivate = binding.writeActivitySwitch.isChecked
                        val bMap = HashMap<String, Any>().apply{ put(getString(R.string.fdb_private), isPrivate)}
                        dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(intent)).updateChildren(bMap)
                                .addOnSuccessListener {
                                    if(isPrivate)
                                        dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(intent)).child(getString(R.string.fdb_like_users)).removeValue()
                                                .addOnSuccessListener {
                                                    Toast.makeText(baseContext, getString(R.string.edit_complete), Toast.LENGTH_SHORT).show()
                                                    finishWithEditResult(this)
                                                }
                                    else {
                                        Toast.makeText(baseContext, getString(R.string.edit_complete), Toast.LENGTH_SHORT).show()
                                        finishWithEditResult(this)
                                    }
                                }
                    }
        }else{
            map[getString(R.string.fdb_content)] = text
            dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(intent)).child(getString(R.string.fdb_pages)).child(pageKey(intent)).updateChildren(map)
                    .addOnSuccessListener {
                        Toast.makeText(baseContext, getString(R.string.edit_complete), Toast.LENGTH_SHORT).show()
                        finishWithEditResult(this)
                    }
        }
    }

    private fun setViewWhenUploading() {
        binding.writeDiaryActivityProgressLayout.visibility = View.VISIBLE
        binding.writeActivityChildConstraintLayout.isEnabled = false
        binding.writeActivityCoverImageView.isEnabled = false
        binding.writeActivityEditText.isEnabled = false
        binding.writeActivityEditText2.isEnabled = false
        binding.writeActivitySwitch.isEnabled = false
        binding.writeActivitySaveButton.isEnabled = false
        binding.writeActivityCancelButton.isEnabled = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_FROM_ALBUM_CODE && resultCode == Activity.RESULT_OK){
            imageUri = data!!.data!!
            Glide.with(baseContext)
                    .load(imageUri)
                    .apply(RequestOptions().centerCrop())
                    .into(binding.writeActivityCoverImageView)
            binding.writeActivityGuideTextView.visibility = View.INVISIBLE
            binding.writeActivityGuideImageView.visibility = View.INVISIBLE
            isImageChanged = true
        }
    }

    companion object{
        @JvmStatic
        fun finishWithEditResult(activity: WriteDiaryActivityKt) {
            activity.setResult(EDIT_DIARY_CODE, activity.intent)
            activity.setResult(RESULT_OK, activity.intent)
            activity.finish()
        }
    }
}
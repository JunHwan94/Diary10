package com.polarstation.diary10.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.polarstation.diary10.R
import com.polarstation.diary10.activity.*
import com.polarstation.diary10.activity.WriteDiaryActivityKt.Companion.finishWithEditResult
import com.polarstation.diary10.databinding.FragmentWriteBinding
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.model.NotificationModel
import com.polarstation.diary10.model.PageModel
import com.polarstation.diary10.model.UserModel
import com.polarstation.diary10.util.NetworkStatus
import gun0912.tedkeyboardobserver.BaseKeyboardObserver
import gun0912.tedkeyboardobserver.TedKeyboardObserver
import io.reactivex.Observable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.*
import java.io.IOException
import java.util.*

const val CONTENT_FIRST_LINE_KEY = "firstLine"
const val CONTEXT_SECOND_LINE_KEY = "secondLine"
const val PREFERENCE = "pref"
const val PICK_FROM_ALBUM_CODE = 100

class WriteFragment : Fragment(), View.OnClickListener {
    private lateinit var binding : FragmentWriteBinding
    private val dbInstance: () -> FirebaseDatabase = { FirebaseDatabase.getInstance() }
    private val strInstance : () -> FirebaseStorage = { FirebaseStorage.getInstance() }
    private val uid: () -> String = { FirebaseAuth.getInstance().currentUser!!.uid }
    private val netStat: () -> Int = { NetworkStatus.getConnectivityStatus(context!!) }
    private var imageUrl: String = ""
    private lateinit var imageUri : Uri
    private lateinit var callbackOptional : Optional<MainFragmentCallBack>
    private var isImageChanged : Boolean = false

    private var writeType : Int? = null
    private lateinit var bundle : Bundle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_write, container, false)
        BaseActivity.setGlobalFont(binding.root)

        isImageChanged = false
        if(netStat() == NetworkStatus.TYPE_CONNECTED){
            bundle = arguments!!
            writeType = NEW_DIARY_TYPE

            if(bundle != Bundle.EMPTY)
                writeType = bundle.getInt(TYPE_KEY)

            setUI(writeType!!)
            if(writeType == NEW_PAGE_TYPE) {
                AlertDialog.Builder(context).setMessage(getString(R.string.create_diary_or_page))
                        .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                            writeType = NEW_DIARY_TYPE
                            setUI(writeType!!)
                        }.setNegativeButton(getString(R.string.cancel)) { _, _ ->
                        }.show()
            }

            Observable.just(binding.writeFragmentChildConstraintLayout, binding.writeFragmentCancelButton, binding.writeFragmentSaveButton)
                    .subscribe{it.setOnClickListener(this)}.dispose()

            TedKeyboardObserver(callbackOptional.get().activity).listen(object : BaseKeyboardObserver.OnKeyboardListener{
                override fun onKeyboardChange(isShow: Boolean) {
                    if(!isShow) binding.writeFragmentRootConstraintLayout.clearFocus()
                }
            })
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()

        return binding.root
    }

    private fun setUI(type: Int){
        when(type){
            NEW_DIARY_TYPE -> {
                binding.writeFragmentSpinner.visibility = View.INVISIBLE
                binding.writeFragmentEditText2.visibility = View.INVISIBLE
                binding.writeFragmentSwitch.visibility = View.VISIBLE
                binding.writeFragmentGuideTextView.text = getString(R.string.select_cover)
                binding.writeFragmentEditText.apply {
                    hint = getString(R.string.write_title)
                    inputType = InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    filters = arrayOf(InputFilter.LengthFilter(15))
                }
            }
            NEW_PAGE_TYPE -> {
                val diaryTitleList = bundle.getStringArrayList(LIST_KEY)
                setSpinner(diaryTitleList!!)
            }
        }
    }

    private fun setSpinner(diaryTitleList: ArrayList<String>) {
        val spinnerAdapter = ArrayAdapter<String>(context!!, android.R.layout.simple_spinner_dropdown_item, diaryTitleList)
        binding.writeFragmentSpinner.adapter = spinnerAdapter
    }

    override fun onClick(v: View){
        when(v.id){
            R.id.writeFragment_childConstraintLayout -> {
                val intent = Intent(Intent.ACTION_PICK).apply { type = MediaStore.Images.Media.CONTENT_TYPE }
                startActivityForResult(intent, PICK_FROM_ALBUM_CODE)
            }
            R.id.writeFragment_cancelButton -> {
                binding.writeFragmentCoverImageView.visibility = View.INVISIBLE
                binding.writeFragmentSwitch.isChecked = false
                binding.writeFragmentEditText.text.clear()
                callbackOptional.get().replaceFragment(LIST_TYPE)
            }
            R.id.writeFragment_saveButton -> {
                if(netStat() == NetworkStatus.TYPE_CONNECTED){
                    when(writeType){
                        NEW_DIARY_TYPE -> {
                            when{
                                binding.writeFragmentCoverImageView.visibility == View.INVISIBLE -> Toast.makeText(context, getString(R.string.select_cover), Toast.LENGTH_SHORT).show()
                                binding.writeFragmentEditText.text.toString() == "" -> Toast.makeText(context, getString(R.string.write_title), Toast.LENGTH_SHORT).show()
                                else -> saveNewDiary()
                            }
                        }
                        NEW_PAGE_TYPE -> {
                            saveNewPage()
                        }
                    }
                }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveNewPage() {
//        Log.d("FindSelectedDiary", "run")
        val firstLine = binding.writeFragmentEditText.text.toString()
        val content = when (val secondLine = binding.writeFragmentEditText2.text.toString()) {
            "" -> firstLine
            else -> "$firstLine\n$secondLine"
        }
        val titleOfDiary = binding.writeFragmentSpinner.selectedItem.toString()

        if(content == "") Toast.makeText(context, getString(R.string.write_content_toast), Toast.LENGTH_SHORT).show()
        else {
            setViewWhenUploading()
            dbInstance().reference.child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid())
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val job = runBlocking {
                                GlobalScope.launch {
                                    sequence { yieldAll(dataSnapshot.children) }
                                        .filter { it.getValue(DiaryModel::class.java)!!.title == titleOfDiary }
                                        .forEach {
                                            val diaryModel = it.getValue(DiaryModel::class.java)!!
                                            val diaryKey = diaryModel.key
                                            if (!isImageChanged) pushPage(context!!, dbInstance(), content, Calendar.getInstance().timeInMillis, imageUrl, diaryKey, diaryModel.title, callbackOptional)
                                            else putPageImageFile(content, diaryModel)
                                        }
                                }
                            }
                            job.start()
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }
    }

    private fun putPageImageFile(content: String, diaryModel: DiaryModel){
//        Log.d("PutPageImageFile", "run")
        val pageCreateTime = Calendar.getInstance().timeInMillis
        strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryModel.createTime.toString()).child(pageCreateTime.toString()).putFile(imageUri)
                .addOnSuccessListener {
                    strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryModel.createTime.toString()).child(pageCreateTime.toString()).downloadUrl
                            .addOnSuccessListener {
                                pushPage(context!!, dbInstance(), content, pageCreateTime, it.toString(), diaryModel.key, diaryModel.title, callbackOptional)
                            }
                }
    }

//    private fun sendFCM(diaryKey: String){
////        Log.d("SendFCM", "run")
//        dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey)
//                .addListenerForSingleValueEvent(object : ValueEventListener{
//                    override fun onDataChange(dataSnapshot: DataSnapshot) {
//                        val diaryModel = dataSnapshot.getValue(DiaryModel::class.java)!!
//                        val likeUsers : Map<String, Boolean> = diaryModel.likeUsers
//
//                        GlobalScope.launch{
//                            sequence{ yieldAll(likeUsers.keys) }
//                                    .filter{ likeUsers[it] ?: false }
//                                    .forEach {
//                                        dbInstance().reference.child(getString(R.string.fdb_users)).child(it)
//                                                .addListenerForSingleValueEvent(object : ValueEventListener{
//                                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
//                                                        val destUserModel = dataSnapshot.getValue(UserModel::class.java)!!
//                                                        sendRequest(context!!, destUserModel, binding.writeFragmentSpinner.selectedItem.toString(), callbackOptional)
//                                                    }
//
//                                                    override fun onCancelled(p0: DatabaseError) {}
//                                                })
//                                    }
//                        }
//                    }
//
//                    override fun onCancelled(p0: DatabaseError) {}
//                })
//    }

    private fun saveNewDiary() {
        setViewWhenUploading()
        putDiaryImageFile()
    }

    private fun putDiaryImageFile(){
        val title = binding.writeFragmentEditText.text.toString()
        val isPrivate = binding.writeFragmentSwitch.isChecked
        val createTime = Calendar.getInstance().timeInMillis

        strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(createTime.toString()).child(uid()).putFile(imageUri)
                .addOnSuccessListener{
                    strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(createTime.toString()).child(uid()).downloadUrl.addOnSuccessListener {
                        imageUrl = it.toString()
                        val diaryModel = DiaryModel(title, uid(), imageUrl, isPrivate, createTime)
                        pushDiary(diaryModel)
                    }
                }
    }

    private fun pushDiary(diaryModel: DiaryModel){
        dbInstance().reference.child(getString(R.string.fdb_diaries)).push().setValue(diaryModel).addOnSuccessListener {
            dbInstance().reference.child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_title)).equalTo(diaryModel.title)
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            GlobalScope.launch{
                                sequence{ yieldAll(dataSnapshot.children)}
                                        .filter { it.getValue(DiaryModel::class.java)!!.uid == uid() }
                                        .forEach {
                                            val key : String = it.key!!
                                            val keyMap = HashMap<String, Any>()
                                            keyMap[getString(R.string.fdb_key)] = key
                                            dbInstance().reference.child(getString(R.string.fdb_diaries)).child(key).updateChildren(keyMap).addOnSuccessListener {
                                                GlobalScope.launch{
                                                    delay(1000)
                                                    callbackOptional.get().replaceFragment(ACCOUNT_TYPE)
                                                }
                                            }
                                        }
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }
    }

    private fun setViewWhenUploading() {
        binding.writeFragmentProgressLayout.visibility = View.VISIBLE
        binding.writeFragmentChildConstraintLayout.isEnabled = false
        binding.writeFragmentCoverImageView.isEnabled = false
        binding.writeFragmentEditText.isEnabled = false
        binding.writeFragmentEditText2.isEnabled = false
        binding.writeFragmentSwitch.isEnabled = false
        binding.writeFragmentCancelButton.isEnabled = false
        binding.writeFragmentSaveButton.isEnabled = false
        callbackOptional.get().setNavigationViewDisabled()
    }

    override fun onStop() {
        super.onStop()
        val pref = context!!.getSharedPreferences(PREFERENCE, Activity.MODE_PRIVATE)
        val editor = pref.edit()
        editor.putString(CONTENT_FIRST_LINE_KEY, binding.writeFragmentEditText.text.toString())
        editor.putString(CONTEXT_SECOND_LINE_KEY, binding.writeFragmentEditText2.text.toString())
        editor.apply()

        binding.writeFragmentEditText.text.clear()
        binding.writeFragmentEditText2.text.clear()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainFragmentCallBack)
            callbackOptional = Optional.of(context)
    }

    override fun onDetach() {
        super.onDetach()
        callbackOptional = Optional.empty()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == PICK_FROM_ALBUM_CODE && resultCode == Activity.RESULT_OK){
            imageUri = data!!.data!!

            binding.writeFragmentGuideImageView.visibility = View.INVISIBLE
            binding.writeFragmentGuideTextView.visibility = View.INVISIBLE
            binding.writeFragmentCoverImageView.visibility = View.VISIBLE

            Glide.with(context)
                    .load(imageUri)
                    .apply(RequestOptions().centerCrop())
                    .into(binding.writeFragmentCoverImageView)

            isImageChanged = true
        }
        val pref = context!!.getSharedPreferences(PREFERENCE, Activity.MODE_PRIVATE)
        if(pref != null){
            binding.writeFragmentEditText.setText(pref.getString(CONTENT_FIRST_LINE_KEY, ""))
            binding.writeFragmentEditText2.setText(pref.getString(CONTEXT_SECOND_LINE_KEY, ""))
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(typeKey: String, writeType: Int, listKey: String = "", sList : ArrayList<String> = arrayListOf()) =
                WriteFragment().apply {
                    arguments = Bundle().apply {
//                        Log.d("WriteFragment", "$writeType")
                        putInt(typeKey, writeType)
                        putStringArrayList(listKey, sList)
                    }
                }

        @JvmStatic
        fun sendRequest(context: Context, destUserModel: UserModel, titleOfDiary: String, callbackOptional: Optional<MainFragmentCallBack>, activityOp: Optional<WriteDiaryActivityKt>){
            val gson = Gson()
            val userName = FirebaseAuth.getInstance().currentUser!!.displayName
            val title = context.getString(R.string.fcm_title)
            val text = userName + "님의 $titleOfDiary "

            val notificationModel = NotificationModel(destUserModel.pushToken)
            notificationModel.data.setTitle(title)
            notificationModel.data.setText(text)

            val requestBody = RequestBody.create(MediaType.parse(context.getString(R.string.media_type)), gson.toJson(notificationModel))
            val request = Request.Builder()
                    .header(context.getString(R.string.request_header_content_type), context.getString(R.string.request_header_content_type_value))
                    .addHeader(context.getString(R.string.request_header_authorization),context.getString(R.string.request_header_authorization_value))
                    .url(context.getString(R.string.fcm_send_url))
                    .post(requestBody)
                    .build()

            val okHttpClient = OkHttpClient()
            okHttpClient.newCall(request).enqueue(object : Callback{
                override fun onResponse(call: Call, response: Response) {
                    Log.d("OkHttp", response.toString())
                    if(callbackOptional != Optional.empty<MainFragmentCallBack>())
                        callbackOptional.get().replaceFragment(ACCOUNT_TYPE) // runOnUiThread
                    else finishWithEditResult(activityOp.get())

                }

                override fun onFailure(call: Call, e: IOException) { e.printStackTrace() }
            })
        }

        @JvmStatic
        fun pushPage(context: Context, dbInstance: FirebaseDatabase, content: String, pageCreateTime: Long, imageUrl: String, diaryKey: String, titleOfDiary: String, callbackOptional: Optional<MainFragmentCallBack> = Optional.empty(), activityOp: Optional<WriteDiaryActivityKt> = Optional.empty()){
//        Log.d("PushPage", "run")
            val pageModel = PageModel(content, imageUrl, pageCreateTime)
            dbInstance.reference.child(context.getString(R.string.fdb_diaries)).child(diaryKey).child(context.getString(R.string.fdb_pages)).push().setValue(pageModel)
                    .addOnSuccessListener {
                        dbInstance.reference.child(context.getString(R.string.fdb_diaries)).child(diaryKey).child(context.getString(R.string.fdb_pages)).orderByChild(context.getString(R.string.fdb_key)).equalTo("")
                                .addListenerForSingleValueEvent(object : ValueEventListener{
                                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                                        val pageKey = dataSnapshot.children.first().key!!
                                        val map = HashMap<String, Any>().apply{ put(context.getString(R.string.fdb_key), pageKey)}
                                        dbInstance.reference.child(context.getString(R.string.fdb_diaries)).child(diaryKey).child(context.getString(R.string.fdb_pages)).child(pageKey).updateChildren(map)
                                                .addOnSuccessListener {
                                                    sendFCM(context, dbInstance, diaryKey, titleOfDiary, callbackOptional, activityOp)
                                                    Toast.makeText(context, context.getString(R.string.uploaded), Toast.LENGTH_SHORT).show()
                                                }
                                    }

                                    override fun onCancelled(p0: DatabaseError) {}
                                })
                    }
        }

        @JvmStatic
        fun sendFCM(context: Context, dbInstance: FirebaseDatabase, diaryKey: String, titleOfDiary: String, callbackOptional: Optional<MainFragmentCallBack>, activityOp: Optional<WriteDiaryActivityKt> = Optional.empty()){
//        Log.d("SendFCM", "run")
            dbInstance.reference.child(context.getString(R.string.fdb_diaries)).child(diaryKey)
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val diaryModel = dataSnapshot.getValue(DiaryModel::class.java)!!
                            val likeUsers : Map<String, Boolean> = diaryModel.likeUsers
                            when {
                                likeUsers.values.contains(true) -> GlobalScope.launch {
                                    sequence { yieldAll(likeUsers.keys) }
                                            .filter { likeUsers[it] ?: false }
                                            .forEach {
                                                dbInstance.reference.child(context.getString(R.string.fdb_users)).child(it)
                                                        .addListenerForSingleValueEvent(object : ValueEventListener {
                                                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                                                val destUserModel = dataSnapshot.getValue(UserModel::class.java)!!
                                                                sendRequest(context, destUserModel, titleOfDiary, callbackOptional, activityOp)
                                                            }

                                                            override fun onCancelled(p0: DatabaseError) {}
                                                        })
                                            }
                                }
                                callbackOptional.isPresent -> callbackOptional.get().replaceFragment(ACCOUNT_TYPE)
                                else -> finishWithEditResult(activityOp.get())
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }
    }
}
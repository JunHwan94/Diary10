package com.polarstation.diary10.fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.polarstation.diary10.R
import com.polarstation.diary10.activity.*
import com.polarstation.diary10.databinding.FragmentWriteBinding
import com.polarstation.diary10.fragment.AccountFragment.PICK_FROM_ALBUM_CODE
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.util.NetworkStatus
import gun0912.tedkeyboardobserver.BaseKeyboardObserver
import gun0912.tedkeyboardobserver.TedKeyboardObserver
import io.reactivex.Observable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

class WriteFragmentKt : Fragment(), View.OnClickListener {
    private lateinit var binding : FragmentWriteBinding
    private lateinit var strInstance : FirebaseStorage
    private lateinit var dbInstance : FirebaseDatabase
    private lateinit var uid : String
    private lateinit var imageUrl: String
    private lateinit var imageUri : Uri
    private lateinit var callbackOptional : Optional<MainFragmentCallBack>
    private var isImageChanged : Boolean = false
    private var netStat : Int? = null
    private var writeType : Int? = null
    private lateinit var bundle : Bundle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_write, container, false)
        Observable.just(binding.root).subscribe{ BaseActivity.setGlobalFont(it)}

        isImageChanged = false
        netStat = NetworkStatus.getConnectivityStatus(context)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            strInstance = FirebaseStorage.getInstance()
            dbInstance = FirebaseDatabase.getInstance()
            uid = FirebaseAuth.getInstance().currentUser!!.uid

            writeType = NEW_DIARY_TYPE
            setUI(writeType!!)
            if(arguments != null){
                bundle = arguments!!
                AlertDialog.Builder(context).setMessage(getString(R.string.create_diary_or_page))
                        .setPositiveButton(getString(R.string.confirm)) { _, _ ->
                            writeType = NEW_DIARY_TYPE
                            setUI(writeType!!)
                        }.setNegativeButton(getString(R.string.cancel)) { _, _ ->
                        }.show()


                Observable.just(binding.writeFragmentChildConstraintLayout, binding.writeFragmentCancelButton)
                        .subscribe{it.setOnClickListener(this)}

                TedKeyboardObserver(callbackOptional.get().activity).listen(object : BaseKeyboardObserver.OnKeyboardListener{
                    override fun onKeyboardChange(isShow: Boolean) {
                        if(!isShow) binding.writeFragmentRootConstraintLayout.clearFocus()
                    }
                })
            }
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
                setSpinner(diaryTitleList)
            }
        }
    }

    private fun setSpinner(diaryTitleList: ArrayList<String>?) {
        val spinnerAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, diaryTitleList)
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
                netStat = NetworkStatus.getConnectivityStatus(context)
                if(netStat == NetworkStatus.TYPE_CONNECTED){
                    when(writeType){
                        NEW_DIARY_TYPE -> {
                            when{
                                binding.writeFragmentCoverImageView.visibility == View.INVISIBLE -> Toast.makeText(context, getString(R.string.select_cover), Toast.LENGTH_SHORT).show()
                                binding.writeFragmentEditText.text == null -> Toast.makeText(context, getString(R.string.write_title), Toast.LENGTH_SHORT).show()
                                else -> {
                                    saveNewDiary()
                                }
                            }
                        }
                        NEW_PAGE_TYPE -> {
                            TODO("implement: new page push")
                        }
                    }
                }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveNewDiary() {
        setViewWhenUploading()
        putFileToFirebaseStorage()
    }

    private fun putFileToFirebaseStorage(){
        val title = binding.writeFragmentEditText.text.toString()
        val isPrivate = binding.writeFragmentSwitch.isChecked
        val createTime = Calendar.getInstance().timeInMillis

        strInstance.reference.child(getString(R.string.fstr_diary_images)).child(uid).child(createTime.toString()).putFile(imageUri)
                .addOnCompleteListener{
                    strInstance.reference.child(getString(R.string.fstr_diary_images)).child(uid).child(createTime.toString()).downloadUrl.addOnSuccessListener {
                        imageUrl = it.toString()
                        val diaryModel = DiaryModel.Builder()
                                .setTitle(title)
                                .setUid(uid)
                                .setCoverImageUrl(imageUrl)
                                .setIsPrivate(isPrivate)
                                .setCreateTime(createTime)
                                .build()
                        pushDiary(diaryModel)
                    }
                }
    }

    private fun pushDiary(diaryModel: DiaryModel){
        dbInstance.reference.child(getString(R.string.fdb_diaries)).push().setValue(diaryModel).addOnSuccessListener {
            dbInstance.reference.child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_title)).equalTo(diaryModel.title)
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            Observable.fromIterable(dataSnapshot.children)
                                    .filter{it.getValue(DiaryModel::class.java)!!.uid == uid }
                                    .subscribe {
                                        val key : String = it.key!!
                                        val keyMap = HashMap<String, Any>()
                                        keyMap.put(getString(R.string.fdb_key), key)
                                        dbInstance.reference.child(getString(R.string.fdb_diaries)).child(key).updateChildren(keyMap).addOnSuccessListener {
                                            GlobalScope.launch{
                                                delay(1000)
                                                callbackOptional.get().replaceFragment(ACCOUNT_TYPE)
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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainFragmentCallBack)
            callbackOptional = Optional.of(context)

    }

    override fun onDetach() {
        super.onDetach()
        if (callbackOptional.get() != null)
            callbackOptional = Optional.empty()

    }

    companion object {
        @JvmStatic
        fun newInstance(key: String, writeType: Int) =
                WriteFragmentKt().apply {
                    arguments = Bundle().apply {
                        putInt(key, writeType)
                    }
                }

        @JvmStatic
        fun newInstance(key: String, sList: ArrayList<String>) =
                WriteFragmentKt().apply {
                    arguments = Bundle().apply {
                        putStringArrayList(key, sList)
                    }
                }
    }
}

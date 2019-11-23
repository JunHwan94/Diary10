package com.polarstation.diary10.activity

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.iid.FirebaseInstanceId
import com.polarstation.diary10.R
import com.polarstation.diary10.databinding.ActivityMainBinding
import com.polarstation.diary10.fragment.AccountFragment
import com.polarstation.diary10.fragment.ListFragment
import com.polarstation.diary10.fragment.MainFragmentCallBack
import com.polarstation.diary10.fragment.WriteFragment
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.util.NetworkStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity(), MainFragmentCallBack {
    private lateinit var binding : ActivityMainBinding
    private lateinit var dbInstance : FirebaseDatabase
    private lateinit var uid : String
    private lateinit var bundle : Bundle
    private var netStat : Int? = null
    private var writeType : Int? = null

    private lateinit var listFragment : ListFragment
    private lateinit var createDiaryFragment : WriteFragment
    private lateinit var writeFragment : WriteFragment
    private lateinit var accountFragment : AccountFragment
    private lateinit var createOrWriteFragment : WriteFragment

    val NEW_DIARY_TYPE = 0
    val NEW_PAGE_TYPE = 1
    val TYPE_KEY = "typeKey"
    val LIST_KEY = "listKey"
    val PUSH_TOKEN = "pushToken"
    val LIST_TYPE = 10
    val CREATE_TYPE = 11
    val WRITE_TYPE = 12
    val ACCOUNT_TYPE = 13

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        uid = FirebaseAuth.getInstance().currentUser!!.uid
        bundle = Bundle()
        dbInstance = FirebaseDatabase.getInstance()

        setViewWhenLoading()

        listFragment = ListFragment()
        createDiaryFragment = WriteFragment()
        writeFragment = WriteFragment()
        accountFragment = AccountFragment()
        createOrWriteFragment = WriteFragment()
        findMyDiary()

        supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, listFragment).commit()
        setNavigationViewListener()
        sendPushToken()
    }

    private fun sendPushToken(){
        val token : String = FirebaseInstanceId.getInstance().token.toString()
        val map = HashMap<String, Any>()
        map.put(PUSH_TOKEN, token)

        dbInstance.reference.child(getString(R.string.fdb_users)).child(uid).updateChildren(map)
    }

    private fun setNavigationViewListener(){
        binding.mainActivityBottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when(menuItem.itemId){
                R.id.action_list -> {
                    menuItem.isChecked = true
                    supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, listFragment).commit()
                    true
                }
                R.id.action_write -> {
                    menuItem.isChecked = true
                    supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, createOrWriteFragment).commit()
                    true
                }
                R.id.action_account -> {
                    menuItem.isChecked = true
                    supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, accountFragment).commit()
                    true
                }
                else -> false
            }
        }
    }

    override fun findMyDiary() {
        netStat = NetworkStatus.getConnectivityStatus(this)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            dbInstance.reference.child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid)
                    .addValueEventListener(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val diaryTitleList = ArrayList<String>()
                            val job = runBlocking {
                                GlobalScope.launch {
                                    dataSnapshot.children.forEach { item ->
                                        val diaryModel = item.getValue(DiaryModel::class.java)!!
                                        val title = diaryModel.title
                                        diaryTitleList.add(title)
                                    }

                                    diaryTitleList.sort()
                                    bundle.putStringArrayList(LIST_KEY, diaryTitleList)
                                    writeFragment.arguments = bundle
                                    createOrWriteFragment = writeFragment
                                }
                            }
                            // 내 일기장이 있을 때
                            if(0 < dataSnapshot.childrenCount){
                                writeType = NEW_PAGE_TYPE
                                bundle.putInt(TYPE_KEY, writeType!!)
                                job.start()
                            } else{ // 없을 때
                                writeType = NEW_DIARY_TYPE
                                bundle.putInt(TYPE_KEY, writeType!!)
                                createDiaryFragment.arguments = bundle
                                createOrWriteFragment = createDiaryFragment
                            }
                            setViewWhenDone()
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    override fun replaceFragment(type: Int) {
        setNavigationViewListener()
        when(type){
            LIST_TYPE -> {
                binding.mainActivityBottomNavigationView.setSelectedItemId(R.id.action_list)
                supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, listFragment).commit()
            }
            CREATE_TYPE -> {
                supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, createDiaryFragment).commit()
            }
            WRITE_TYPE -> {
                supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, writeFragment).commit()
                setViewWhenDone()
            }
            ACCOUNT_TYPE -> {
                Toast.makeText(baseContext, getString(R.string.uploaded), Toast.LENGTH_LONG).show()
                supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, accountFragment).commit()
                setViewWhenDone()
            }
        }
    }

    override fun quitApp() {
        finishAffinity()
        System.runFinalization()
        exitProcess(0)
    }

    override fun getActivity(): Activity {
        return this
    }

    override fun setNavigationViewDisabled() {
        binding.mainActivityBottomNavigationView.setOnNavigationItemSelectedListener(null)
    }

    private fun setViewWhenLoading(){
        binding.mainActivityProgressBar.visibility = View.VISIBLE
        binding.mainActivityBottomNavigationView.isEnabled = false
    }

    private fun setViewWhenDone(){
        binding.mainActivityProgressBar.visibility = View.INVISIBLE
        binding.mainActivityBottomNavigationView.isEnabled = true
    }
}
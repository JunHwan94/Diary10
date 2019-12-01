package com.polarstation.diary10.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.polarstation.diary10.fragment.WriteFragmentKt
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.util.NetworkStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

const val NEW_DIARY_TYPE = 0
const val NEW_PAGE_TYPE = 1
const val TYPE_KEY = "typeKey"
const val LIST_KEY = "listKey"
const val PUSH_TOKEN = "pushToken"
const val LIST_TYPE = 10
const val CREATE_TYPE = 11
const val WRITE_TYPE = 12
const val ACCOUNT_TYPE = 13

class MainActivity : AppCompatActivity(), MainFragmentCallBack {
    private lateinit var binding : ActivityMainBinding
    private lateinit var dbInstance : FirebaseDatabase
    private lateinit var uid : String
    private lateinit var bundle : Bundle
    private var netStat : Int? = null

    private lateinit var listFragment : ListFragment
    private lateinit var createDiaryFragment : WriteFragmentKt
    private lateinit var writeFragment : WriteFragmentKt
    private lateinit var accountFragment : AccountFragment
    private lateinit var createOrWriteFragment : WriteFragmentKt


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        uid = FirebaseAuth.getInstance().currentUser!!.uid
        bundle = Bundle()
        dbInstance = FirebaseDatabase.getInstance()

        setViewWhenLoading()

        listFragment = ListFragment()
        accountFragment = AccountFragment()
        findMyDiary()

        supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, listFragment).commit()

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
                            // 내 일기장이 있을 때
                            if(0 < dataSnapshot.childrenCount){
                                GlobalScope.launch {
                                    sequence{ yieldAll(dataSnapshot.children) }
                                            .forEach {
                                                val diaryModel = it.getValue(DiaryModel::class.java)!!
                                                val title = diaryModel.title
                                                diaryTitleList.add(title)
                                            }
                                    diaryTitleList.sort()
                                    createOrWriteFragment = WriteFragmentKt.newInstance(TYPE_KEY, NEW_PAGE_TYPE, LIST_KEY, diaryTitleList)
                                }
                            } else createOrWriteFragment = WriteFragmentKt.newInstance(TYPE_KEY, NEW_DIARY_TYPE) // 없을 때
                            setViewWhenDone()
                            setNavigationViewListener()
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    override fun replaceFragment(type: Int) {
        setNavigationViewListener()
        when(type){
            LIST_TYPE -> {
                binding.mainActivityBottomNavigationView.selectedItemId = R.id.action_list
                supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, listFragment).commit()
            }
            CREATE_TYPE -> {
                binding.mainActivityBottomNavigationView.selectedItemId = R.id.action_write
                supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, createDiaryFragment).commit()
            }
            WRITE_TYPE -> {
                binding.mainActivityBottomNavigationView.selectedItemId = R.id.action_write
                supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, writeFragment).commit()
                setViewWhenDone()
            }
            ACCOUNT_TYPE -> {
                runOnUiThread {
                    Toast.makeText(baseContext, getString(R.string.uploaded), Toast.LENGTH_LONG).show()
                    binding.mainActivityBottomNavigationView.selectedItemId = R.id.action_account
                    supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, accountFragment).commit()
                    setViewWhenDone()
                }
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
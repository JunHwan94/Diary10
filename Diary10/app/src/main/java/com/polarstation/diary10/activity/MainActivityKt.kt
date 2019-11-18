package com.polarstation.diary10.activity

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId
import com.polarstation.diary10.R
import com.polarstation.diary10.databinding.ActivityMainBinding
import com.polarstation.diary10.fragment.AccountFragment
import com.polarstation.diary10.fragment.ListFragment
import com.polarstation.diary10.fragment.MainFragmentCallBack
import com.polarstation.diary10.fragment.WriteFragment

class MainActivityKt : AppCompatActivity(), MainFragmentCallBack {
    private lateinit var binding : ActivityMainBinding
    private lateinit var dbInstance : FirebaseDatabase
    private lateinit var uid : String
    private lateinit var bundle : Bundle
    private var netStat : Int? = null

    private lateinit var listFragment : ListFragment
    private lateinit var createDiaryFragment : WriteFragment
    private lateinit var writeFragment : WriteFragment
    private lateinit var accountFragment : AccountFragment
    private lateinit var createOrWriteFragment : WriteFragment

    val NEW_DIARY_TYPE = 0
    val NEW_PAGE_TYPE = 1
    val TYPE_KEY = "typeKey"
    val LIST_KEY = "listKey"
    val USER_MODEL_KEY = "userModelKey"
    val PUSH_TOKEN = "pushToken"

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

        supportFragmentManager.beginTransaction().replace(R.id.mainActivity_frameLayout, listFragment)
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
        TODO("not implemented")
    }

    override fun replaceFragment(type: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun quitApp() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getActivity(): Activity {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setNavigationViewDisabled() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
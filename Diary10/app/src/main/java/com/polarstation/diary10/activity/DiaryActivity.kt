package com.polarstation.diary10.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.polarstation.diary10.R
import com.polarstation.diary10.databinding.ActivityDiaryBinding
import com.polarstation.diary10.fragment.*
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.model.PageModel
import com.polarstation.diary10.util.NetworkStatus
import java.util.*
import kotlin.collections.ArrayList

const val IS_COVER_KEY = "isCoverKey"
const val PAGE_MODEL_KEY = "pageModelKey"
const val EDIT_DIARY_CODE = 200

class DiaryActivity : AppCompatActivity(), DiaryFragmentCallback {
    private lateinit var binding: ActivityDiaryBinding
    private val isCover = true
    private val title: () -> String = { intent.getStringExtra(TITLE_KEY) }
    private val writerUid: () -> String = { intent.getStringExtra(WRITER_UID_KEY) }
    private val diaryKey: () -> String = { intent.getStringExtra(DIARY_KEY_KEY) }
    private val dbInstance: () -> FirebaseDatabase = { FirebaseDatabase.getInstance() }
//    private lateinit var pagerAdapter: ListPagerAdapter
    private val netStat: () -> Int = { NetworkStatus.getConnectivityStatus(this) }
    private var isDataChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_diary)

        if(netStat() == NetworkStatus.TYPE_CONNECTED){
            val pagerAdapter = ListPagerAdapter(supportFragmentManager)
            binding.diaryActivityViewPager.apply { offscreenPageLimit = 2; adapter = pagerAdapter }
            loadDiaryCover(pagerAdapter)
            loadDiary(diaryKey(), pagerAdapter)
        }else {
            Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun loadDiary(key: String?, pagerAdapter: ListPagerAdapter) {
        if(netStat() == NetworkStatus.TYPE_CONNECTED){
            setViewWhenLoading()
            dbInstance().reference.child(getString(R.string.fdb_diaries)).child(key!!).child(getString(R.string.fdb_pages)).orderByChild(getString(R.string.fdb_createTime))
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            sequence { yieldAll(dataSnapshot.children) }
                                    .map { it.getValue(PageModel::class.java)!! }
                                    .forEach{
                                        pagerAdapter.addItem(PageFragment.newInstance(title = title(), writerUid = writerUid(),
                                                diaryKey = diaryKey(), pageModelOp = Optional.of(it)))
//                                        runOnUiThread { pagerAdapter.notifyDataSetChanged() }
                                    }
                            setViewWhenDone()
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    private fun setViewWhenLoading() {
        binding.diaryActivityProgressBar.visibility = View.VISIBLE
        binding.diaryActivityViewPager.visibility = View.INVISIBLE
    }

    private fun setViewWhenDone() {
        binding.diaryActivityProgressBar.visibility = View.INVISIBLE
        binding.diaryActivityViewPager.visibility = View.VISIBLE
    }

    override fun finishDiaryActivity() = onBackPressed()

    override fun getActivity(): Activity = this

    override fun dataChanges() { isDataChanged = true }

    override fun onBackPressed() {
        if(isDataChanged){
            setResult(SHOW_DIARY_CODE, Intent())
            setResult(Activity.RESULT_OK, Intent())
        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == EDIT_DIARY_CODE && resultCode == Activity.RESULT_OK){
            Log.d("ActivityResult", "OK")
//            dataChanges()
            isDataChanged = true
//            pagerAdapter.clear()
            val pagerAdapter = ListPagerAdapter(supportFragmentManager)
            binding.diaryActivityViewPager.adapter = pagerAdapter
            loadDiaryCover(pagerAdapter)
            loadDiary(diaryKey(), pagerAdapter)
        }
    }

    private fun loadDiaryCover(pagerAdapter: ListPagerAdapter){
        if(netStat() == NetworkStatus.TYPE_CONNECTED){
            setViewWhenLoading()
            dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey())
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val diaryModel = dataSnapshot.getValue(DiaryModel::class.java)!!
                            pagerAdapter.addItem(PageFragment.newInstance(isCover = isCover, title = diaryModel.title,
                                    writerUid = diaryModel.uid, coverImageUrl = diaryModel.coverImageUrl, diaryKey = diaryModel.key))
                            setViewWhenDone()
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })

        }else Toast.makeText(baseContext, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    class ListPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
        private val fragmentList = ArrayList<Fragment>()
        fun addItem(fragment: Fragment){
            fragmentList.add(fragment)
            notifyDataSetChanged() // 여기서 notify 안하면 에러발생
        }
        fun clear(){
            fragmentList.clear()
            notifyDataSetChanged()
        }
        override fun getItem(position: Int): Fragment = fragmentList[position]
        override fun getCount(): Int = fragmentList.size
    }
}

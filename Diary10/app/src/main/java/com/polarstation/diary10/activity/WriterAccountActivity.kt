package com.polarstation.diary10.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.polarstation.diary10.R
import com.polarstation.diary10.data.DiaryRecyclerViewAdapter
import com.polarstation.diary10.databinding.ActivityWriterAccountBinding
import com.polarstation.diary10.fragment.*
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.model.UserModel
import com.polarstation.diary10.util.FontUtil
import com.polarstation.diary10.util.NetworkStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class WriterAccountActivity : AppCompatActivity(), View.OnClickListener {
    private lateinit var binding: ActivityWriterAccountBinding
    private val dbInstance: () -> FirebaseDatabase = { FirebaseDatabase.getInstance() }
    private val netStat: () -> Int = { NetworkStatus.getConnectivityStatus(this) }
    private lateinit var imageUrl: String
    private val writerUid: (Intent) -> String = { it.getStringExtra(WRITER_UID_KEY) }
    private lateinit var adapter: DiaryRecyclerViewAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_writer_account)
        FontUtil.setGlobalFont(binding.root)
        if(netStat() == NetworkStatus.TYPE_CONNECTED)
            if(intent != null){
                binding.writerAccountActivityProgressBar.visibility = View.VISIBLE
                setUserInfo()
                loadDiaries()
            }
        binding.writerAccountActivityProfileImageView.setOnClickListener(this)

        val layoutManager = GridLayoutManager(baseContext, 3)
        binding.writerAccountActivityRecyclerView.layoutManager = layoutManager
        adapter = DiaryRecyclerViewAdapter()
        adapter.setOnItemClickListener(object : DiaryRecyclerViewAdapter.OnItemClickListener{
            override fun onItemClick(holder: DiaryRecyclerViewAdapter.DiaryViewHolder, view: View, position: Int) {
                val diaryModel = adapter.getItem(position)
                startActivity(Intent(this@WriterAccountActivity, DiaryActivity::class.java).apply {
                    putExtra(TITLE_KEY, diaryModel.title)
                    putExtra(WRITER_UID_KEY, diaryModel.uid)
                    putExtra(IMAGE_URL_KEY, diaryModel.coverImageUrl)
                    putExtra(DIARY_KEY_KEY, diaryModel.key)
                })
            }
        })
        binding.writerAccountActivityRecyclerView.adapter = adapter
    }

    private fun loadDiaries() {
        dbInstance().reference.child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_private)).equalTo(false)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        Log.d("writerUid", writerUid(intent))
                        GlobalScope.launch{
                            sequence{ yieldAll(dataSnapshot.children) }
                                    .map{ it.getValue(DiaryModel::class.java)!! }
                                    .filter{ it.uid == writerUid(intent) }
                                    .forEach { adapter.addItem(it); runOnUiThread{ adapter.notifyDataSetChanged() } }

                            Log.d("adapterSize", "${adapter.itemCount}")
                            binding.writerAccountActivityProgressBar.visibility = View.INVISIBLE
                        }
                    }

                    override fun onCancelled(p0: DatabaseError) {}
                })
    }

    private fun setUserInfo() {
        dbInstance().reference.child(getString(R.string.fdb_users)).child(writerUid(intent))
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val userModel = dataSnapshot.getValue(UserModel::class.java)!!
                        imageUrl = userModel.profileImageUrl

                        binding.writerAccountActivityNameTextView.text = userModel.userName
                        binding.writerAccountActivityCommentTextView.text = userModel.comment
                        Glide.with(baseContext)
                                .load(imageUrl)
                                .apply(RequestOptions().circleCrop())
                                .into(binding.writerAccountActivityProfileImageView)
                    }

                    override fun onCancelled(p0: DatabaseError) {
                    }
                })
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.writerAccountActivity_profileImageView ->
                startActivity(Intent(this, PhotoViewActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(URL_KEY, imageUrl)
                })
        }
    }
}

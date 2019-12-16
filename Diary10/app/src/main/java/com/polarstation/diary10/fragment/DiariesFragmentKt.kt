package com.polarstation.diary10.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.polarstation.diary10.R
import com.polarstation.diary10.activity.DiaryActivity
import com.polarstation.diary10.data.DiaryRecyclerViewAdapter
import com.polarstation.diary10.databinding.FragmentDiariesBinding
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.util.NetworkStatus
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

const val SHOW_DIARY_CODE = 100
class DiariesFragmentKt : Fragment() {
    private val netStat: () -> Int = { NetworkStatus.getConnectivityStatus(context!!) }
    private val uid: () -> String = { FirebaseAuth.getInstance().currentUser!!.uid }
    private val dbInstance: () -> FirebaseDatabase = { FirebaseDatabase.getInstance() }
    private val type: () -> String = { arguments!!.getString(FRAGMENT_TYPE_KEY)!! }
    private lateinit var callbackOptional: Optional<MainFragmentCallback>
    private lateinit var adapter: DiaryRecyclerViewAdapter
    private lateinit var binding: FragmentDiariesBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_diaries, container,false)

        loadDiariesOfType()
        val layoutManager = GridLayoutManager(context, 3)
        binding.diariesFragmentRecyclerView.layoutManager = layoutManager
        adapter = DiaryRecyclerViewAdapter()
        adapter.setOnItemClickListener { _, _, position->
            val diaryModel = adapter.getItem(position)
            startActivityForResult(Intent(context, DiaryActivity::class.java).apply{
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(TITLE_KEY, diaryModel.title)
                putExtra(WRITER_UID_KEY, diaryModel.uid)
                putExtra(IMAGE_URL_KEY, diaryModel.coverImageUrl)
                putExtra(DIARY_KEY_KEY, diaryModel.key)
            }, SHOW_DIARY_CODE)
        }
        binding.diariesFragmentRecyclerView.adapter = adapter

        return binding.root
    }

    private fun loadDiariesOfType() {
        when(type()){
            MY_DIARY -> loadMyDiaries()
            LIKED_DIARY -> loadLikedDiaries()
        }
    }

    private fun loadLikedDiaries() {
        if(netStat() == NetworkStatus.TYPE_CONNECTED){
            binding.diariesFragmentProgressBar.visibility = View.VISIBLE
            dbInstance().reference.child(getString(R.string.fdb_diaries))
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            adapter.clear()
                            GlobalScope.launch{
                                sequence{ yieldAll(dataSnapshot.children) }
                                        .filter {
                                            val map = it.getValue(DiaryModel::class.java)!!.likeUsers
                                            map.keys.contains(uid()) && (map[uid()] ?: false)
                                        }.map { it.getValue(DiaryModel::class.java)!! }
                                        .forEach {
                                            adapter.addItem(it)
                                            callbackOptional.get().notifyAdapter(adapter)
                                        }

                                binding.diariesFragmentProgressBar.visibility = View.INVISIBLE
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    private fun loadMyDiaries() {
        if(netStat() == NetworkStatus.TYPE_CONNECTED){
            binding.diariesFragmentProgressBar.visibility = View.VISIBLE
            dbInstance().reference.child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid())
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            adapter.clear()
                            GlobalScope.launch{
                                sequence{ yieldAll(dataSnapshot.children) }
                                        .forEach{
                                            adapter.addItem(it.getValue(DiaryModel::class.java)!!)
                                            callbackOptional.get().notifyAdapter(adapter)
                                        }

                                binding.diariesFragmentProgressBar.visibility = View.INVISIBLE
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == SHOW_DIARY_CODE && resultCode == Activity.RESULT_OK)
            loadDiariesOfType()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is MainFragmentCallback)
            callbackOptional = Optional.of(context)
    }

    override fun onDetach() {
        super.onDetach()
        callbackOptional = Optional.empty()
    }
}

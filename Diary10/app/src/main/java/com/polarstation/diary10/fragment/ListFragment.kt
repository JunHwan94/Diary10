package com.polarstation.diary10.fragment

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
import com.polarstation.diary10.activity.BaseActivity
import com.polarstation.diary10.activity.DiaryActivity
import com.polarstation.diary10.data.DiaryRecyclerViewAdapter
import com.polarstation.diary10.databinding.FragmentListBinding
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.util.NetworkStatus
import gun0912.tedkeyboardobserver.BaseKeyboardObserver
import gun0912.tedkeyboardobserver.TedKeyboardObserver
import io.reactivex.Observable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

const val TITLE_KEY = "titleKey"
const val WRITER_UID_KEY = "writerKey"
const val IMAGE_URL_KEY = "imageUrlKey"
const val DIARY_KEY_KEY ="diaryKeyKey"

class ListFragment : Fragment(), View.OnClickListener {
    private lateinit var binding : FragmentListBinding
    private val netStat: () -> Int = { NetworkStatus.getConnectivityStatus(context!!) }
    private val dbInstance: () -> FirebaseDatabase = { FirebaseDatabase.getInstance() }
    private val uid: () -> String = { FirebaseAuth.getInstance().currentUser!!.uid }
    private lateinit var adapter : DiaryRecyclerViewAdapter
    private lateinit var callbackOptional: Optional<MainFragmentCallBack>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_list, container, false)
        Observable.just(binding.root).subscribe{ BaseActivity.setGlobalFont(it) }

        if(netStat() == NetworkStatus.TYPE_CONNECTED){
            adapter = DiaryRecyclerViewAdapter()
            setAdapterListener(adapter)
            binding.listFragmentRecyclerView.layoutManager = GridLayoutManager(context, 3)
            binding.listFragmentRecyclerView.adapter = adapter

            loadDiaries()
            Observable.just(binding.listFragmentRefreshButton, binding.listFragmentSearchButton)
                    .subscribe{it.setOnClickListener(this)}

            TedKeyboardObserver(activity!!).listen(object : BaseKeyboardObserver.OnKeyboardListener{
                override fun onKeyboardChange(isShow: Boolean) {
                    if(!isShow) binding.listFragmentSearchEditText.clearFocus()
                }
            })
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()

        return binding.root
    }

    private fun setAdapterListener(adapter: DiaryRecyclerViewAdapter){
        adapter.setOnItemClickListener{ _, _, position ->
            val diaryModel = adapter.getItem(position)
            val intent = Intent(context, DiaryActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DIARY_KEY_KEY, diaryModel.key)
                putExtra(TITLE_KEY, diaryModel.title)
                putExtra(WRITER_UID_KEY, diaryModel.uid)
                putExtra(IMAGE_URL_KEY, diaryModel.coverImageUrl)
            }
            startActivity(intent)
        }
    }

    private fun loadDiaries() {
        if(netStat() == NetworkStatus.TYPE_CONNECTED){
            binding.listFragmentProgressBar.visibility = View.VISIBLE
            dbInstance().reference.child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_private)).equalTo(false)
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            adapter.clear()
                            GlobalScope.launch {
                                sequence { yieldAll(dataSnapshot.children) }
                                        .filter { it.getValue(DiaryModel::class.java)!!.uid != uid() }
                                        .map { it.getValue(DiaryModel::class.java)!! }
                                        .forEach {
                                            adapter.addItem(it)
                                            callbackOptional.get().notifyAdapter(adapter)
                                        }
                            }

                            binding.listFragmentProgressBar.visibility = View.INVISIBLE
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    private fun searchDiaries(searchWord: String){
        if(netStat() == NetworkStatus.TYPE_CONNECTED){
            binding.listFragmentProgressBar.visibility = View.VISIBLE
            dbInstance().reference.child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_private)).equalTo(false)
                    .addListenerForSingleValueEvent(object : ValueEventListener{
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            adapter.clear()
                            if(adapter.itemCount == 0) Toast.makeText(context, getString(R.string.no_result), Toast.LENGTH_SHORT).show()
                            else GlobalScope.launch{
                                    sequence{ yieldAll(dataSnapshot.children) }
                                            .filter{ it.getValue(DiaryModel::class.java)!!.title.contains(searchWord) }
                                            .map{ it.getValue(DiaryModel::class.java) }
                                            .forEach {
                                                adapter.addItem(it)
                                                callbackOptional.get().notifyAdapter(adapter)
                                            }
                            }
                            binding.listFragmentProgressBar.visibility = View.INVISIBLE
                        }

                        override fun onCancelled(p0: DatabaseError) {}
                    })
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is MainFragmentCallBack)
            callbackOptional = Optional.of(context)
    }

    override fun onDetach() {
        super.onDetach()
        callbackOptional = Optional.empty()
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.listFragment_searchButton -> {
                val searchWord = binding.listFragmentSearchEditText.text.toString()
                searchDiaries(searchWord)
                binding.listFragmentSearchEditText.clearFocus()
            }
            R.id.listFragment_refreshButton -> {
                loadDiaries()
                binding.listFragmentSearchEditText.apply {
                    text.clear()
                    clearFocus()
                }
            }
        }
    }

}
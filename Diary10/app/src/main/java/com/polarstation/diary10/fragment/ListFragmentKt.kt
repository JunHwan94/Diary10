package com.polarstation.diary10.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.polarstation.diary10.R
import com.polarstation.diary10.activity.BaseActivity
import com.polarstation.diary10.activity.DiaryActivity
import com.polarstation.diary10.data.DiaryRecyclerViewAdapter

import com.polarstation.diary10.databinding.FragmentListBinding
import com.polarstation.diary10.fragment.ListFragment.DIARY_KEY_KEY
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.util.NetworkStatus
import gun0912.tedkeyboardobserver.BaseKeyboardObserver
import gun0912.tedkeyboardobserver.TedKeyboardObserver

class ListFragmentKt : Fragment(), View.OnClickListener {
    private lateinit var binding : FragmentListBinding
    private var netStat : Int? = null
    private lateinit var dbInstance : FirebaseDatabase
    private lateinit var uid : String
    private lateinit var adapter : DiaryRecyclerViewAdapter
    private lateinit var diaryModelList : List<DiaryModel>

    val TITLE_KEY = "titleKey"
    val WRITER_UID_KEY = "writerKey"
    val IMAGE_URL_KEY = "imageUrlKey"
    val DIARY_KEY_KEY ="diaryKeyKey"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_list, container, false)
        BaseActivity.setGlobalFont(binding.root)

        netStat = NetworkStatus.getConnectivityStatus(context)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            dbInstance = FirebaseDatabase.getInstance()
            uid = FirebaseAuth.getInstance().currentUser!!.uid

            adapter = DiaryRecyclerViewAdapter()
            setAdapterListener(adapter)

            diaryModelList = ArrayList()

            binding.listFragmentRecyclerView.layoutManager = GridLayoutManager(context, 3)
            binding.listFragmentRecyclerView.adapter = adapter
            loadDiaries()

            binding.listFragmentRefreshButton.setOnClickListener(this)
            binding.listFragmentSearchButton.setOnClickListener(this)

            activity?.let { TedKeyboardObserver(it).listen(object : BaseKeyboardObserver.OnKeyboardListener{
                override fun onKeyboardChange(isShow: Boolean) {
                    if(!isShow) binding.listFragmentSearchEditText.clearFocus()
                }
            })}
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()

        return binding.root
    }

    override fun onClick(v: View) {
        when(v.id){

        }
    }

    private fun loadDiaries() {

    }

    private fun setAdapterListener(adapter: DiaryRecyclerViewAdapter){
        adapter.setOnItemClickListener{ _, _, position ->
            val diaryModel = adapter.getItem(position)
            val intent = Intent(context, DiaryActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP

            intent.putExtra(DIARY_KEY_KEY, diaryModel.key)
            intent.putExtra(TITLE_KEY, diaryModel.title)
            intent.putExtra(WRITER_UID_KEY, diaryModel.uid)
            intent.putExtra(IMAGE_URL_KEY, diaryModel.coverImageUrl)
            startActivity(intent)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onDetach() {
        super.onDetach()
    }
}

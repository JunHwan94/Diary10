package com.polarstation.diary10.fragment


import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.text.Editable
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.ads.AdRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.polarstation.diary10.R
import com.polarstation.diary10.activity.*
import com.polarstation.diary10.databinding.FragmentPageBinding
import com.polarstation.diary10.model.DiaryModel
import com.polarstation.diary10.model.PageModel
import com.polarstation.diary10.model.UserModel
import com.polarstation.diary10.util.NetworkStatus
import io.reactivex.Observable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

const val CONTENT_KEY = "contentKey"
const val PAGE_KEY_KEY = "pageKeyKey"
const val IS_NEW_KEY = "isNewKey"
const val PAGE_CREATE_TIME_KEY = "pageCreateTimeKey"

class PageFragment : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentPageBinding
    private var isMenuOpened = false
    private val netStat: () -> Int = { NetworkStatus.getConnectivityStatus(context!!) }
    private val dbInstance: () -> FirebaseDatabase = { FirebaseDatabase.getInstance() }
    private val strInstance: () -> FirebaseStorage = { FirebaseStorage.getInstance() }
    private val uid: () -> String = { FirebaseAuth.getInstance().currentUser!!.uid }
    private val adRequest: () -> AdRequest = { AdRequest.Builder().build() }
    private val isCover: (Bundle) -> Boolean = { it.getBoolean(IS_COVER_KEY) }
    private val pageModel: (Bundle) -> PageModel = { it.getParcelable(PAGE_MODEL_KEY)!! }
    private val diaryKey: (Bundle) -> String = { it.getString(DIARY_KEY_KEY)!! }
    private val writerUid: (Bundle) -> String = { it.getString(WRITER_UID_KEY)!! }
    private val imageUrl: (Bundle) -> String = { it.getString(IMAGE_URL_KEY)!! }
    private val title: (Bundle) -> String = { it.getString(TITLE_KEY)!! }
    private val scaleBigger: () -> Animation = { AnimationUtils.loadAnimation(context, R.anim.spread_left_up) }
    private val scaleSmaller: () -> Animation = { AnimationUtils.loadAnimation(context, R.anim.contract_right_down) }
    private lateinit var callbackOptional: Optional<DiaryFragmentCallback>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_page, container, false)
        BaseActivity.setGlobalFont(binding.root)

        isMenuOpened = false
        if(netStat() == NetworkStatus.TYPE_CONNECTED){
            setUI()
            setMenu()
            loadLikeOrNot()

            val clickableViewList = listOf(binding.pageFragmentMenuButton, binding.pageFragmentDeleteDiaryButton, binding.pageFragmentEditDiaryButton,
                    binding.pageFragmentWritePageButton, binding.pageFragmentWriterTextView, binding.pageFragmentLikeButton,
                    binding.pageFragmentImageView, binding.pageFragmentLabel)
            Observable.fromIterable(clickableViewList).subscribe{ it.setOnClickListener(this) }.dispose()

        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
        return binding.root
    }

    private fun loadLikeOrNot() {
        Log.d("loadLikeOrNot", "called")
        dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(arguments!!))
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val diaryModel = dataSnapshot.getValue(DiaryModel::class.java)!!
                        if(diaryModel.likeUsers.keys.contains(uid()) && (diaryModel.likeUsers[uid()] ?: error("")))
                            binding.pageFragmentLikeButton.isSelected = true
                    }

                    override fun onCancelled(p0: DatabaseError) {}
                })
    }

    private fun setMenu() {
        Log.d("setMenu", "called")
        if(uid() == writerUid(arguments!!)) {
            binding.pageFragmentMenuButton.visibility = View.VISIBLE
            binding.pageFragmentLikeButton.visibility = View.INVISIBLE
            val listener = object : Animation.AnimationListener{
                override fun onAnimationRepeat(p0: Animation?) {}

                override fun onAnimationEnd(p0: Animation?) {
                    if(isMenuOpened)
                        binding.pageFragmentSlideMenu.visibility = View.INVISIBLE
                }

                override fun onAnimationStart(p0: Animation?) {}
            }
            scaleBigger().setAnimationListener(listener)
            scaleSmaller().setAnimationListener(listener)
        }
    }

    private fun setUI() {
        Log.d("setUI", "called")
        setViewWhenLoading()
        lateinit var imageUrl: String
        if(isCover(arguments!!)){
            binding.pageFragmentDateTextView.visibility = View.INVISIBLE
            binding.pageFragmentWritePageButton.visibility = View.VISIBLE
            binding.pageFragmentAdView.loadAd(adRequest())
            imageUrl = imageUrl(arguments!!)

            setWriterAndImage(writerUid(arguments!!))
            setCoverImageViewSize()
        }else {
            val pageModel = pageModel(arguments!!)
            val sdf = SimpleDateFormat(getString(R.string.date_format))
            val date = Date(pageModel.createTime)
            imageUrl = pageModel.imageUrl

            binding.pageFragmentContentTextView.apply{ textSize = 22.0f; text = Editable.Factory().newEditable(pageModel.content) }
            binding.pageFragmentDateTextView.text = sdf.format(date)
            binding.pageFragmentDeleteDiaryButton.text = getString(R.string.delete_page)
            binding.pageFragmentEditDiaryButton.text = getString(R.string.edit_page)
            binding.pageFragmentWritePageButton.visibility = View.GONE
            binding.pageFragmentLikeButton.visibility = View.INVISIBLE
        }

        Glide.with(context)
                .load(imageUrl)
                .apply(RequestOptions().centerCrop().sizeMultiplier(0.4f))
                .listener(object : RequestListener<Drawable>{
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        setViewWhenDone()
                        return false
                    }
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                        e!!.printStackTrace()
                        if(imageUrl != "") Toast.makeText(context, getString(R.string.image_load_failed), Toast.LENGTH_SHORT).show()
                        setViewWhenDone()
                        return false
                    }
                })
                .into(binding.pageFragmentImageView)
    }

    private fun setCoverImageViewSize() {
        Log.d("setCoverImageView", "called")
        val metrics = DisplayMetrics()
        val windowManager = context!!.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(metrics)
        binding.pageFragmentImageView.layoutParams.width = metrics.widthPixels
        binding.pageFragmentImageView.layoutParams.height = metrics.heightPixels
        binding.pageFragmentLabel.setBackgroundColor(resources.getColor(R.color.trans_white_deep, Resources.getSystem().newTheme()))
    }

    private fun setWriterAndImage(writerUid: String) {
        dbInstance().reference.child(getString(R.string.fdb_users)).child(writerUid)
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val userModel = dataSnapshot.getValue(UserModel::class.java)!!
                        binding.pageFragmentWriterTextView.text = userModel.userName
                        binding.pageFragmentContentTextView.text = title(arguments!!)
                        Glide.with(context)
                                .load(userModel.profileImageUrl)
                                .apply(RequestOptions().circleCrop().override(200, 200))
                                .listener(object : RequestListener<Drawable>{
                                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                        binding.pageFragmentWriterImageView.visibility = View.VISIBLE
                                        binding.pageFragmentWriterTextView.visibility = View.VISIBLE
                                        return false
                                    }

                                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                                        Toast.makeText(context, getString(R.string.image_load_failed), Toast.LENGTH_SHORT).show()
                                        return false
                                    }
                                })
                                .into(binding.pageFragmentWriterImageView)
                    }

                    override fun onCancelled(p0: DatabaseError) {}
                })
    }

    private fun setViewWhenLoading() {
        Log.d("setViewWhenLoading", "called")
        binding.pageFragmentProgressLayout.visibility = View.VISIBLE
        binding.pageFragmentMenuButton.isEnabled = false
        binding.pageFragmentSlideMenu.cardElevation = 0f
    }

    private fun setViewWhenDone() {
        Log.d("setViewWhenDone", "called")
        binding.pageFragmentProgressLayout.visibility = View.INVISIBLE
        binding.pageFragmentMenuButton.isEnabled = true
    }


    override fun onClick(v: View) {
        when(v.id){
            R.id.pageFragment_menuButton -> {
                Log.d("isMenuOpened", "$isMenuOpened")
                if (isMenuOpened){
                    Log.d("Animation", "smaller")
                    binding.pageFragmentSlideMenu.startAnimation(scaleSmaller())
                    binding.pageFragmentSlideMenu.visibility = View.INVISIBLE
                }else {
                    Log.d("Animation", "bigger")
                    binding.pageFragmentSlideMenu.visibility = View.VISIBLE
                    binding.pageFragmentSlideMenu.startAnimation(scaleBigger())
                }
                isMenuOpened = !isMenuOpened
            }
            R.id.pageFragment_deleteDiaryButton ->
                if(netStat() == NetworkStatus.TYPE_CONNECTED){
                    callbackOptional.get().dataChanges()
                    setViewWhenLoading()
                    deleteData()
                }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
            R.id.pageFragment_editDiaryButton, R.id.pageFragment_writePageButton ->
                startWriteDiaryActivity(v.id)
            R.id.pageFragment_writerTextView ->
                if(uid() != writerUid(arguments!!)){
                    startActivity(Intent(context, WriterAccountActivity::class.java).apply{
                        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(WRITER_UID_KEY, writerUid(arguments!!))
                    })
                }
            R.id.pageFragment_likeButton -> {
                callbackOptional.get().dataChanges()
                v.isSelected = !v.isSelected
                processLike(v.isSelected)
            }
            R.id.pageFragment_imageView, R.id.pageFragment_label -> if(isMenuOpened) {
                binding.pageFragmentSlideMenu.startAnimation(scaleSmaller())
                binding.pageFragmentSlideMenu.visibility = View.INVISIBLE
                isMenuOpened = false
            }
        }
    }

    private fun startWriteDiaryActivity(id: Int){
        callbackOptional.get().getActivity().startActivityForResult(Intent(context, WriteDiaryActivity::class.java).apply{
            when(id) {
                R.id.pageFragment_editDiaryButton -> {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(IS_COVER_KEY, isCover(arguments!!))
                    putExtra(DIARY_KEY_KEY, diaryKey(arguments!!))
                    putExtra(TITLE_KEY, title(arguments!!))
                    if (!isCover(arguments!!)) {
                        val pageModel = pageModel(arguments!!)
                        putExtra(IMAGE_URL_KEY, pageModel.imageUrl)
                        putExtra(CONTENT_KEY, pageModel.content)
                        putExtra(PAGE_KEY_KEY, pageModel.key)
                        putExtra(PAGE_CREATE_TIME_KEY, pageModel.createTime)
                    }else putExtra(IMAGE_URL_KEY, imageUrl(arguments!!))
                }
                R.id.pageFragment_writePageButton -> {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(TITLE_KEY, title(arguments!!))
                    putExtra(IS_NEW_KEY, true)
                    putExtra(DIARY_KEY_KEY, diaryKey(arguments!!))
                }
            }
        }, EDIT_DIARY_CODE)
    }

    private fun processLike(like: Boolean) {
        val map = HashMap<String, Any>().apply{ put(uid(), like) }
        if(netStat() == NetworkStatus.TYPE_CONNECTED)
            dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(arguments!!)).child(getString(R.string.fdb_like_users)).updateChildren(map)
        else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()
    }

    private fun deleteData() {
        Log.d("deleteData", "called")
        dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(arguments!!))
                .addListenerForSingleValueEvent(object : ValueEventListener{
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val diaryModel = dataSnapshot.getValue(DiaryModel::class.java)!!
                        if(isCover(arguments!!))
                            deletePictures(diaryModel)
                        else deletePicture(diaryModel)
                    }

                    override fun onCancelled(p0: DatabaseError) {}
                })
    }

    private fun deletePicture(diaryModel: DiaryModel){
        Log.d("deletePicture", "called")
        GlobalScope.launch {
            sequence { yieldAll(diaryModel.pages.values) }
                    .filter { it.key == pageModel(arguments!!).key }
                    .forEach {
                        if(pageModel(arguments!!).imageUrl != "")
                            strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryModel.createTime.toString()).child(it.createTime.toString()).delete()
                                    .addOnSuccessListener { deletePageInDatabase() }
                        else deletePageInDatabase()
                    }
        }
    }

    private fun deletePageInDatabase() {
        Log.d("deletePageInDatabase", "called")
        dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(arguments!!)).child(getString(R.string.fdb_pages)).child(pageModel(arguments!!).key).removeValue()
                .addOnSuccessListener {
                    binding.pageFragmentProgressBar.visibility = View.INVISIBLE
                    Toast.makeText(context, getString(R.string.delete_success), Toast.LENGTH_SHORT).show()
                    callbackOptional.get().finishDiaryActivity()
                }
    }

    private fun deletePictures(diaryModel: DiaryModel) {
        Log.d("deletePictures", "called")
        strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryModel.createTime.toString()).child(uid()).delete()
                .addOnSuccessListener {
                    dbInstance().reference.child(getString(R.string.fdb_diaries)).child(diaryKey(arguments!!)).removeValue()
                            .addOnSuccessListener {
                                binding.pageFragmentProgressLayout.visibility = View.INVISIBLE
                                Toast.makeText(context, getString(R.string.delete_success), Toast.LENGTH_SHORT).show()
                                GlobalScope.launch{
                                    sequence{ yieldAll(diaryModel.pages.values) }
                                            .map{ it.createTime }
                                            .forEach { strInstance().reference.child(getString(R.string.fstr_diary_images)).child(uid()).child(diaryModel.createTime.toString()).child(it.toString()).delete() }
                                    callbackOptional.get().finishDiaryActivity()
                                }
                            }
                }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is DiaryFragmentCallback)
            callbackOptional = Optional.of(context)
    }

    override fun onDetach() {
        super.onDetach()
        callbackOptional = Optional.empty()
    }

    override fun onPause() {
        isMenuOpened = false
        binding.pageFragmentSlideMenu.visibility = View.INVISIBLE
        super.onPause()
    }

    companion object{
        @JvmStatic
        fun newInstance(isCover: Boolean = false, title: String = "", writerUid: String = "", coverImageUrl: String = "", diaryKey: String = "", pageModelOp: Optional<PageModel> = Optional.empty()) =
                PageFragment().apply {
                    arguments = Bundle().apply {
                        putBoolean(IS_COVER_KEY, isCover)
                        putString(TITLE_KEY, title)
                        putString(WRITER_UID_KEY, writerUid)
                        putString(IMAGE_URL_KEY, coverImageUrl)
                        putString(DIARY_KEY_KEY, diaryKey)
                        if(pageModelOp != Optional.empty<PageModel>()) putParcelable(PAGE_MODEL_KEY, pageModelOp.get())
                    }
                }
    }
}

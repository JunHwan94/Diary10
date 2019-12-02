package com.polarstation.diary10.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.facebook.login.LoginManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.polarstation.diary10.R
import com.polarstation.diary10.activity.BaseActivity
import com.polarstation.diary10.activity.EditAccountActivity
import com.polarstation.diary10.activity.PUSH_TOKEN
import com.polarstation.diary10.activity.PhotoViewActivity
import com.polarstation.diary10.databinding.FragmentAccountBinding
import com.polarstation.diary10.model.UserModel
import com.polarstation.diary10.util.NetworkStatus
import io.reactivex.Observable
import java.util.*

const val USER_MODEL_KEY = "userModelKey"
const val FRAGMENT_TYPE_KEY = "fragmentTypeKey"
const val MY_DIARY = "myDiary"
const val LIKED_DIARY = "likedDiary"
const val URL_KEY = "urlKey"
const val NAME_KEY = "nameKey"
const val URI_KEY = "uriKey"
const val COMMENT_KEY = "commentKey"
const val EDIT_COMPLETE_CODE = 101

class AccountFragment : Fragment(), View.OnClickListener {
    private lateinit var binding : FragmentAccountBinding
    private lateinit var authInstance : FirebaseAuth
    private lateinit var dbInstance : FirebaseDatabase
    private lateinit var callbackOptional : Optional<MainFragmentCallBack>
    private lateinit var uid : String
    var isMenuOpened = true
    private var netStat : Int? = null
    private lateinit var imageUrl : String
    private lateinit var scaleBigger : Animation
    private lateinit var scaleSmaller: Animation

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_account, container, false)
        BaseActivity.setGlobalFont(binding.root)

        isMenuOpened = false
        netStat = NetworkStatus.getConnectivityStatus(context)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            authInstance = FirebaseAuth.getInstance()
            dbInstance = FirebaseDatabase.getInstance()

            uid = authInstance.currentUser!!.uid
            dbInstance.reference.child(getString(R.string.fdb_users)).child(uid).addListenerForSingleValueEvent(object : ValueEventListener{
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userModel = dataSnapshot.getValue(UserModel::class.java)
                    val bundle = Bundle()
                    bundle.putParcelable(USER_MODEL_KEY, userModel)
                    setUserInfo(bundle)
                }

                override fun onCancelled(p0: DatabaseError) {}
            })

            val viewList = listOf(binding.accountFragmentProfileImageView, binding.accountFragmentEditButton, binding.accountFragmentMenuButton, binding.accountFragmentSignOutButton,
                    binding.accountFragmentLicenseGuideButton, binding.accountFragmentRootLayout)
            Observable.fromIterable(viewList).subscribe{ it.setOnClickListener(this) }

            setDiariesFragment()
            setButtonAnimation()
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show()

        return binding.root
    }

    private fun setButtonAnimation() {
        scaleBigger = AnimationUtils.loadAnimation(context, R.anim.spread_left_down)
        scaleSmaller = AnimationUtils.loadAnimation(context, R.anim.contract_right_up)
        val listener = object : Animation.AnimationListener{
            override fun onAnimationRepeat(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                if(isMenuOpened){
                    binding.accountFragmentSlideMenu.visibility = View.INVISIBLE
                    isMenuOpened = false
                }else isMenuOpened = true
            }

            override fun onAnimationStart(p0: Animation?) {}
        }

        scaleBigger.setAnimationListener(listener)
        scaleSmaller.setAnimationListener(listener)
    }

    private fun setDiariesFragment() {
        var bundle = Bundle()
        val myDiariesFragment = DiariesFragment()
        bundle.putString(FRAGMENT_TYPE_KEY, MY_DIARY)
        myDiariesFragment.arguments = bundle
        bundle = Bundle()

        val likedDiariesFragment = DiariesFragment()
        bundle.putString(FRAGMENT_TYPE_KEY, LIKED_DIARY)
        likedDiariesFragment.arguments = bundle

        childFragmentManager.beginTransaction().replace(R.id.accountFragment_frameLayout, myDiariesFragment).commit()
        binding.accountFragmentTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener{
            override fun onTabSelected(tab: TabLayout.Tab) {
                when(tab.position){
                    0 -> childFragmentManager.beginTransaction().replace(R.id.accountFragment_frameLayout, myDiariesFragment).commit()
                    1 -> childFragmentManager.beginTransaction().replace(R.id.accountFragment_frameLayout, likedDiariesFragment).commit()
                }
            }

            override fun onTabReselected(p0: TabLayout.Tab?) {}

            override fun onTabUnselected(p0: TabLayout.Tab?) {}
        })
    }

    private fun setUserInfo(bundle: Bundle) {
        val userModel : UserModel = bundle.getParcelable(USER_MODEL_KEY)!!
        imageUrl = userModel.profileImageUrl

        netStat = NetworkStatus.getConnectivityStatus(context)
        if(netStat == NetworkStatus.TYPE_CONNECTED){
            Glide.with(context)
                    .load(imageUrl)
                    .apply(RequestOptions().circleCrop().override(200,300))
                    .into(binding.accountFragmentProfileImageView)
        }else Toast.makeText(context, getString(R.string.image_load_failed), Toast.LENGTH_SHORT).show()

        binding.accountFragmentNameTextView.text = userModel.userName
        binding.accountFragmentCommentTextView.text = userModel.comment
    }

    override fun onClick(v: View) {
        when(v.id){
            R.id.accountFragment_profileImageView -> {
                binding.accountFragmentSlideMenu.visibility = View.INVISIBLE
                startActivity(Intent(context, PhotoViewActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(URL_KEY, imageUrl)})
            }
            R.id.accountFragment_editButton -> {
                binding.accountFragmentSlideMenu.visibility = View.INVISIBLE
                startActivityForResult(Intent(context, EditAccountActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(URL_KEY, imageUrl)
                    putExtra(NAME_KEY, binding.accountFragmentNameTextView.text.toString())
                    putExtra(COMMENT_KEY, binding.accountFragmentCommentTextView.text.toString())
                }, EDIT_COMPLETE_CODE)
            }
            R.id.accountFragment_menuButton -> {
                if(isMenuOpened) binding.accountFragmentSlideMenu.startAnimation(scaleSmaller)
                else{
                    binding.accountFragmentSlideMenu.visibility = View.VISIBLE
                    binding.accountFragmentSlideMenu.startAnimation(scaleBigger)
                }
            }
            R.id.accountFragment_signOutButton -> {
                authInstance.signOut()
                val map = HashMap<String, Any>().apply{ put(PUSH_TOKEN, "") }
                dbInstance.reference.child(getString(R.string.fdb_users)).child(uid).updateChildren(map)

                LoginManager.getInstance().logOut()
                AlertDialog.Builder(context).setTitle(getString(R.string.sign_out)).setMessage(getString(R.string.dialog_quit))
                        .setPositiveButton(getString(R.string.confirm)) { _, _ -> callbackOptional.get().quitApp() }.show()
            }
            R.id.accountFragment_licenseGuideButton -> {
                binding.accountFragmentSlideMenu.visibility = View.INVISIBLE
                startActivity(Intent(context, PhotoViewActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(URL_KEY, "")
                })
            }
            R.id.accountFragment_rootLayout -> {
                if(isMenuOpened) binding.accountFragmentSlideMenu.startAnimation(scaleSmaller)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == EDIT_COMPLETE_CODE && resultCode == Activity.RESULT_OK){
            val imageUri = data!!.getStringExtra(URI_KEY)
            val name = data.getStringExtra(NAME_KEY)
            val comment = data.getStringExtra(COMMENT_KEY)

            if(imageUrl != ""){
                Glide.with(context)
                        .load(imageUri)
                        .apply(RequestOptions().circleCrop())
                        .listener(object : RequestListener<Drawable>{
                            override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                                val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)
                                binding.accountFragmentProfileImageView.startAnimation(fadeIn)
                                return false
                            }

                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean { return false }
                        })
                        .into(binding.accountFragmentProfileImageView)
                imageUrl = imageUri
            }
            binding.accountFragmentNameTextView.text = name
            binding.accountFragmentCommentTextView.text = comment
        }
    }

    override fun onResume() {
        super.onResume()
        isMenuOpened = false
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context is MainFragmentCallBack)
            callbackOptional = Optional.of(context)
    }

    override fun onDetach() {
        super.onDetach()
        if(callbackOptional != Optional.empty<MainFragmentCallBack>())
            callbackOptional = Optional.empty()
    }
}
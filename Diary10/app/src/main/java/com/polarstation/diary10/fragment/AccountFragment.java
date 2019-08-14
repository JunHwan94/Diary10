package com.polarstation.diary10.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.facebook.login.LoginManager;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.BaseActivity;
import com.polarstation.diary10.EditAccountActivity;
import com.polarstation.diary10.PhotoViewActivity;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.FragmentAccountBinding;
import com.polarstation.diary10.model.UserModel;


import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import static com.polarstation.diary10.EditAccountActivity.COMMENT_KEY;
import static com.polarstation.diary10.EditAccountActivity.NAME_KEY;
import static com.polarstation.diary10.EditAccountActivity.URI_KEY;
import static com.polarstation.diary10.MainActivity.USER_MODEL_KEY;

public class AccountFragment extends Fragment implements View.OnClickListener{
    private FirebaseDatabase dbInstance;
    private FirebaseAuth authInstance;
    private static FragmentAccountBinding binding;
    private String imageUrl = "";
    private boolean isChanged = false;
    private MainFragmentCallBack callback;
    private Animation translateLeft;
    private Animation translateRight;
    static boolean isMenuOpen = false;

    public static final int PICK_FROM_ALBUM_CODE = 100;
    public static final int EDIT_COMPLETE_CODE = 101;
    public static final String URL_KEY = "urlKey";
    public static final String FRAGMENT_TYPE_KEY = "fragmentTypeKey";
    public static final String MY_DIARY = "myDiary";
    public static final String LIKED_DIARY = "likedDiary";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_account, container, false);
        BaseActivity.setGlobalFont(binding.getRoot());
        dbInstance = FirebaseDatabase.getInstance();
        authInstance = FirebaseAuth.getInstance();

        if(!isChanged) {
            Bundle bundle = getArguments();
            setUserInfo(bundle);
        }else{
            String uid = authInstance.getCurrentUser().getUid();
            dbInstance.getReference().child(getString(R.string.fdb_users)).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    UserModel userModel = dataSnapshot.getValue(UserModel.class);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(USER_MODEL_KEY, userModel);
                    setUserInfo(bundle);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

        binding.accountFragmentProfileImageView.setOnClickListener(this);
        binding.accountFragmentEditButton.setOnClickListener(this);
        binding.accountFragmentMenuButton.setOnClickListener(this);
        binding.accountFragmentSignOutButton.setOnClickListener(this);
        binding.accountFragmentLicenseGuideButton.setOnClickListener(this);

        setFragment();
        setButtonAnimation();

        return binding.getRoot();
    }

    private void setFragment(){
        Bundle bundle = new Bundle();
        DiariesFragment myDiariesFragment = new DiariesFragment();
        bundle.putString(FRAGMENT_TYPE_KEY, MY_DIARY);
        myDiariesFragment.setArguments(bundle);

        bundle = new Bundle();
        DiariesFragment likedDiariesFragment = new DiariesFragment();
        bundle.putString(FRAGMENT_TYPE_KEY, LIKED_DIARY);
        likedDiariesFragment.setArguments(bundle);

        getFragmentManager().beginTransaction().replace(R.id.accountFragment_frameLayout, myDiariesFragment).commit();
        binding.accountFragmentTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch(tab.getPosition()){
                    case 0:
                        getFragmentManager().beginTransaction().replace(R.id.accountFragment_frameLayout, myDiariesFragment).commit();
                        break;
                    case 1:
                        getFragmentManager().beginTransaction().replace(R.id.accountFragment_frameLayout, likedDiariesFragment).commit();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    private void setButtonAnimation(){
        translateLeft = AnimationUtils.loadAnimation(getContext(), R.anim.translate_left);
        translateRight = AnimationUtils.loadAnimation(getContext(), R.anim.translate_right);
        Animation.AnimationListener listener = new SlidingAnimationListener();
        translateLeft.setAnimationListener(listener);
        translateRight.setAnimationListener(listener);
    }

    private void setUserInfo(Bundle bundle){
        UserModel userModel = bundle.getParcelable(USER_MODEL_KEY);

        imageUrl = userModel.getProfileImageUrl();
        Glide.with(getContext())
                .load(imageUrl)
                .apply(new RequestOptions().circleCrop())
                .into(binding.accountFragmentProfileImageView);

        String userName = userModel.getUserName();
        binding.accountFragmentNameTextView.setText(userName);
        String comment = userModel.getComment();
        binding.accountFragmentCommentTextView.setText(comment);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof MainFragmentCallBack)
            callback = (MainFragmentCallBack) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(callback != null)
            callback = null;
    }

    @Override
    public void onPause() {
        isMenuOpen = false;
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == EDIT_COMPLETE_CODE && resultCode == Activity.RESULT_OK){
            String name = data.getStringExtra(NAME_KEY);
            String comment = data.getStringExtra(COMMENT_KEY);
            String imageUri = data.getStringExtra(URI_KEY);

            if(!imageUri.equals("")) {
//                Toast.makeText(getContext(), String.valueOf(imageUri), Toast.LENGTH_SHORT).show();
                Glide.with(getContext())
                        .load(imageUri)
                        .apply(new RequestOptions().circleCrop())
                        .into(binding.accountFragmentProfileImageView);
                imageUrl = imageUri;
            }
            binding.accountFragmentNameTextView.setText(name);
            binding.accountFragmentCommentTextView.setText(comment);
            isChanged = true;
        }
    }

    @Override
    public void onClick(View view) {
        Intent intent;
        switch(view.getId()){
            case R.id.accountFragment_profileImageView:
                intent = new Intent(getContext(), PhotoViewActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(URL_KEY, imageUrl);
                startActivity(intent);
                break;
            case R.id.accountFragment_editButton:
                String userName = String.valueOf(binding.accountFragmentNameTextView.getText());
                String comment = String.valueOf(binding.accountFragmentCommentTextView.getText());
                intent = new Intent(getContext(), EditAccountActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(URL_KEY, imageUrl);
                intent.putExtra(NAME_KEY, userName);
                intent.putExtra(COMMENT_KEY, comment);
                startActivityForResult(intent, EDIT_COMPLETE_CODE);
                break;
            case R.id.accountFragment_menuButton:
                if(isMenuOpen)
                    binding.accountFragmentSlideMenu.startAnimation(translateRight);
                else {
                    binding.accountFragmentSlideMenu.setVisibility(View.VISIBLE);
                    binding.accountFragmentSlideMenu.startAnimation(translateLeft);
                }
                break;
            case R.id.accountFragment_signOutButton:
                authInstance.signOut();
                LoginManager.getInstance().logOut();
                new AlertDialog.Builder(getContext()).setTitle(getString(R.string.sign_out)).setMessage(getString(R.string.dialog_quit))
                        .setPositiveButton(getString(R.string.confirm), (dialogInterface, i) -> callback.quitApp() ).show();
                break;
            case R.id.accountFragment_licenseGuideButton:
                new AlertDialog.Builder(getContext()).setTitle(getString(R.string.font_license_title))
                        .setMessage("\n1. " + getString(R.string.font_license_1) +
                                "\n\n2. " + getString(R.string.font_license_2) +
                                "\n\n3. " + getString(R.string.font_license_3) +
                                "\n\n4. " + getString(R.string.font_license_4) +
                                "\n\n5. " + getString(R.string.font_license_5)).setPositiveButton(getString(R.string.confirm), null).show();
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        callback.cancelDialog();
    }

    public static class SlidingAnimationListener implements Animation.AnimationListener{
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if(isMenuOpen){
                binding.accountFragmentSlideMenu.setVisibility(View.INVISIBLE);
                isMenuOpen = false;
            }else
                isMenuOpen = true;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }

    public static class ListPagerAdapter extends FragmentStatePagerAdapter {
        List<Fragment> fragmentList = new ArrayList<>();

        public ListPagerAdapter(FragmentManager fm){
            super(fm);
        }

        public void addItem(Fragment fragment){
            fragmentList.add(fragment);
        }

        void clear(){
            fragmentList.clear();
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }
    }
}
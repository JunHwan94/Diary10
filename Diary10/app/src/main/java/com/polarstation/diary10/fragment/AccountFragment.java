package com.polarstation.diary10.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.polarstation.diary10.BaseActivity;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.FragmentAccountBinding;
import com.polarstation.diary10.model.UserModel;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import static com.polarstation.diary10.Util.DialogUtils.showProgressDialog;

public class AccountFragment extends Fragment {
    public static final int PICK_FROM_ALBUM_CODE = 100;
    private FirebaseDatabase dbInstance = FirebaseDatabase.getInstance();
    private FirebaseStorage strInstance = FirebaseStorage.getInstance();
    private FragmentAccountBinding binding;
    private String uid;
    private ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_account, container, false);
        BaseActivity.setGlobalFont(binding.getRoot());

        setUserInfo();

        binding.accountFragmentProfileImageView.setOnClickListener(v->{
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
            startActivityForResult(intent, PICK_FROM_ALBUM_CODE);
        });

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        binding.accountFragmentRecyclerView.setLayoutManager(layoutManager);

        return binding.getRoot();
    }

    private void setUserInfo(){
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        dbInstance.getReference().child(getString(R.string.fdb_users)).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserModel userModel = dataSnapshot.getValue(UserModel.class);

                String imageUrl = userModel.getProfileImageUrl();
                Glide.with(getContext())
                        .load(imageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(binding.accountFragmentProfileImageView);

                String userName = userModel.getUserName();
                binding.accountFragmentNameTextView.setText(userName);
                String comment = userModel.getComment();
                binding.accountFragmentCommentTextView.setText(comment);

                progressDialog.cancel();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        progressDialog = new ProgressDialog(context);
        showProgressDialog(progressDialog, getString(R.string.please_wait));
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_FROM_ALBUM_CODE && resultCode == Activity.RESULT_OK){
            Uri imageUri = data.getData();
            Glide.with(getContext())
                    .load(imageUri)
                    .apply(new RequestOptions().circleCrop())
                    .into(binding.accountFragmentProfileImageView);

            strInstance.getReference().child(getString(R.string.fstr_user_images)).child(uid).putFile(imageUri)
            .addOnCompleteListener(task -> {
                strInstance.getReference().child(getString(R.string.fstr_user_images)).child(uid).getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = String.valueOf(uri);

                    Map<String, Object> stringObjectMap = new HashMap<>();
                    stringObjectMap.put(getString(R.string.fdb_profile_image_url), imageUrl);
                    dbInstance.getReference().child(getString(R.string.fdb_users)).child(uid).updateChildren(stringObjectMap);
                });

            });
        }
    }
}

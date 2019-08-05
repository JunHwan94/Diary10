package com.polarstation.diary10.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.BaseActivity;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.FragmentAccountBinding;
import com.polarstation.diary10.model.UserModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

public class AccountFragment extends Fragment {
    private FragmentAccountBinding binding;
    ProgressDialog progressDialog;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_account, container, false);
        BaseActivity.setGlobalFont(binding.getRoot());

        showProgressDialog();

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference().child(getString(R.string.fdb_users)).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
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

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        binding.accountFragmentRecyclerView.setLayoutManager(layoutManager);

        return binding.getRoot();
    }

    public void showProgressDialog(){
        progressDialog = new ProgressDialog(getContext());

        SpannableString message = new SpannableString(getString(R.string.please_wait));
        message.setSpan(new RelativeSizeSpan(1.0f), 0, message.length(), 0);
        progressDialog.setMessage(message);
        progressDialog.show();

        WindowManager.LayoutParams params = progressDialog.getWindow().getAttributes();
        params.width = 1200;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        progressDialog.getWindow().setAttributes(params);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}

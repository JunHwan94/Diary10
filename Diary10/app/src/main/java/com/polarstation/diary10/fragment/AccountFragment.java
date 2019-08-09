package com.polarstation.diary10.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.BaseActivity;
import com.polarstation.diary10.DiaryRecyclerViewAdapter;
import com.polarstation.diary10.EditAccountActivity;
import com.polarstation.diary10.PhotoViewActivity;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.FragmentAccountBinding;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.UserModel;


import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import static com.polarstation.diary10.EditAccountActivity.COMMENT_KEY;
import static com.polarstation.diary10.EditAccountActivity.NAME_KEY;
import static com.polarstation.diary10.EditAccountActivity.URI_KEY;
import static com.polarstation.diary10.MainActivity.USER_MODEL_KEY;
import static com.polarstation.diary10.util.DialogUtils.showProgressDialog;

public class AccountFragment extends Fragment implements View.OnClickListener{
    private FirebaseDatabase dbInstance;
    private FragmentAccountBinding binding;
    private String imageUrl = "";
    private ProgressDialog progressDialog;
    private DiaryRecyclerViewAdapter adapter;
    private List<DiaryModel> diaryModelList;

    public static final int PICK_FROM_ALBUM_CODE = 100;
    public static final int EDIT_COMPLETE_CODE = 101;
    public static final String URL_KEY = "urlKey";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_account, container, false);
        BaseActivity.setGlobalFont(binding.getRoot());
        dbInstance = FirebaseDatabase.getInstance();

        Bundle bundle = getArguments();
        setUserInfo(bundle);

        binding.accountFragmentProfileImageView.setOnClickListener(this);
        binding.accountFragmentEditButton.setOnClickListener(this);

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        binding.accountFragmentRecyclerView.setLayoutManager(layoutManager);
        adapter = new DiaryRecyclerViewAdapter();
        binding.accountFragmentRecyclerView.setAdapter(adapter);
        loadMyDiaries();

        return binding.getRoot();
    }

    private void loadMyDiaries(){
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        diaryModelList = new ArrayList<>();
        dbInstance.getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        diaryModelList.clear();
                        for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
                            diaryModelList.add(diaryModel);
                        }
                        adapter.addAll(diaryModelList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
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

        progressDialog.cancel();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        progressDialog = new ProgressDialog(context);
        showProgressDialog(progressDialog, getString(R.string.loading_data));
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == EDIT_COMPLETE_CODE && resultCode == Activity.RESULT_OK){
            String name = data.getStringExtra(NAME_KEY);
            String comment = data.getStringExtra(COMMENT_KEY);
            String imageUri = data.getStringExtra(URI_KEY);

            if(!imageUri.equals("")) {
//                Toast.makeText(getContext(), String.valueOf(imageUri), Toast.LENGTH_SHORT).show();
                Glide.with(this.getContext())
                        .load(imageUri)
                        .apply(new RequestOptions().circleCrop())
                        .into(binding.accountFragmentProfileImageView);
                imageUrl = imageUri;
            }
            binding.accountFragmentNameTextView.setText(name);
            binding.accountFragmentCommentTextView.setText(comment);
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
        }
    }
}

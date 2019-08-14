package com.polarstation.diary10;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.databinding.ActivityWriterAccountBinding;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.UserModel;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.GridLayoutManager;

import static com.polarstation.diary10.fragment.AccountFragment.URL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.DIARY_KEY_KEY;
import static com.polarstation.diary10.fragment.ListFragment.IMAGE_URL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.TITLE_KEY;
import static com.polarstation.diary10.fragment.ListFragment.WRITER_UID_KEY;

public class WriterAccountActivity extends BaseActivity implements View.OnClickListener{
    private ActivityWriterAccountBinding binding;
    private FirebaseDatabase dbInstance;
    private DiaryRecyclerViewAdapter adapter;
    private String imageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_writer_account);
        dbInstance = FirebaseDatabase.getInstance();

        Intent intent = getIntent();
        if(intent != null){
            processIntent(intent);
        }

        binding.writerAccountActivityProfileImageView.setOnClickListener(this);

        GridLayoutManager layoutManager = new GridLayoutManager(getBaseContext(), 3);
        binding.writerAccountActivityRecyclerView.setLayoutManager(layoutManager);
        adapter = new DiaryRecyclerViewAdapter();
        adapter.setOnItemClickListener((holder, view, position) -> {
            DiaryModel diaryModel = adapter.getItem(position);
            Intent diaryActivityIntent = new Intent(getBaseContext(), DiaryActivity.class);
            diaryActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            diaryActivityIntent.putExtra(TITLE_KEY, diaryModel.getTitle());
            diaryActivityIntent.putExtra(WRITER_UID_KEY, diaryModel.getUid());
            diaryActivityIntent.putExtra(IMAGE_URL_KEY, diaryModel.getCoverImageUrl());
            diaryActivityIntent.putExtra(DIARY_KEY_KEY, diaryModel.getKey());
            startActivity(diaryActivityIntent);
        });
        binding.writerAccountActivityRecyclerView.setAdapter(adapter);
    }

    private void processIntent(Intent intent) {
        String writerUid = intent.getStringExtra(WRITER_UID_KEY);

        setUserInfo(writerUid);
        loadDiaries(writerUid);
    }

    private void loadDiaries(String writerUid) {
        List<DiaryModel>  diaryModelList = new ArrayList<>();
        dbInstance.getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_private)).equalTo(false)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        diaryModelList.clear();
                        for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
                            if(diaryModel.getUid().equals(writerUid))
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

    private void setUserInfo(String writerUid){
        dbInstance.getReference().child(getString(R.string.fdb_users)).child(writerUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        UserModel userModel = dataSnapshot.getValue(UserModel.class);
                        String userName = userModel.getUserName();
                        String comment = userModel.getComment();
                        imageUrl = userModel.getProfileImageUrl();

                        binding.writerAccountActivityNameTextView.setText(userName);
                        binding.writerAccountActivityCommentTextView.setText(comment);
                        Glide.with(getBaseContext())
                                .load(imageUrl)
                                .apply(new RequestOptions().circleCrop())
                                .into(binding.writerAccountActivityProfileImageView);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.writerAccountActivity_profileImageView:
                Intent intent = new Intent(getBaseContext(), PhotoViewActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(URL_KEY, imageUrl);
                startActivity(intent);
                break;
        }
    }
}
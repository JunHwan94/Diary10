package com.polarstation.diary10;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import gun0912.tedkeyboardobserver.TedKeyboardObserver;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.polarstation.diary10.databinding.ActivityWriteDiaryBinding;
import com.polarstation.diary10.fragment.PageFragmentCallback;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.PageModel;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.polarstation.diary10.DiaryActivity.IS_COVER_KEY;
import static com.polarstation.diary10.fragment.ListFragment.DIARY_KEY_KEY;
import static com.polarstation.diary10.fragment.ListFragment.IMAGE_URL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.TITLE_KEY;
import static com.polarstation.diary10.fragment.PageFragment.CONTENT_KEY;
import static com.polarstation.diary10.fragment.PageFragment.EDIT_DIARY_CODE;
import static com.polarstation.diary10.fragment.PageFragment.IS_NEW_KEY;
import static com.polarstation.diary10.fragment.PageFragment.PAGE_CREATE_TIME_KEY;
import static com.polarstation.diary10.fragment.PageFragment.PAGE_KEY_KEY;
import static com.polarstation.diary10.util.DialogUtils.showProgressDialog;

public class WriteDiaryActivity extends BaseActivity implements View.OnClickListener{
    private ActivityWriteDiaryBinding binding;
    private boolean isCover;
    private String diaryKey;
    private String pageKey;
    private Uri imageUri;
    private FirebaseStorage strInstance;
    private FirebaseDatabase dbInstance;
    private boolean isImageChanged;
    private String uid;
    private String title;
    private String content;
    private long diaryCreateTime;
    private long pageCreateTime;
    private boolean isNew;

    private static final int PICK_FROM_ALBUM_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_write_diary);
        strInstance = FirebaseStorage.getInstance();
        dbInstance = FirebaseDatabase.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        Intent intent = getIntent();
        if(intent != null)
            setUI(intent);

        binding.writeActivitySaveButton.setOnClickListener(this);
        binding.writeActivityCancelButton.setOnClickListener(this);
        binding.writeActivityChildConstraintLayout.setOnClickListener(this);

        // 키보드 올라오거나 내려갈때 이벤트
        new TedKeyboardObserver(this).listen(isShow-> {
            if(!isShow){
                binding.writeActivityEditText.clearFocus();
            }
        });
    }

    private void setUI(Intent intent){
        isCover = intent.getBooleanExtra(IS_COVER_KEY, false);
        diaryKey = intent.getStringExtra(DIARY_KEY_KEY);
        String imageUrl = intent.getStringExtra(IMAGE_URL_KEY);
        title = intent.getStringExtra(TITLE_KEY);
        content = intent.getStringExtra(CONTENT_KEY);
        pageKey = intent.getStringExtra(PAGE_KEY_KEY);
        isNew = intent.getBooleanExtra(IS_NEW_KEY, false);
        pageCreateTime = intent.getLongExtra(PAGE_CREATE_TIME_KEY, 0);
        if(isNew){
            binding.writeActivityGuideImageView.setVisibility(View.VISIBLE);
            binding.writeActivityGuideTextView.setVisibility(View.VISIBLE);
        }else if(isCover){
            binding.writeActivityTitleTextView.setVisibility(View.INVISIBLE);
            binding.writeActivitySwitch.setVisibility(View.VISIBLE);
            binding.writeActivityEditText.setText(title);
            binding.writeActivityEditText.setHint(R.string.write_title);
            binding.writeActivityEditText.setMaxEms(15);
        }else{
            String content = intent.getStringExtra(CONTENT_KEY);
            binding.writeActivityEditText.setText(content);
            binding.writeActivityTitleTextView.setText(title);
            try{
                if(imageUrl.equals(null));
            }catch(Exception e){
                binding.writeActivityGuideTextView.setText(getString(R.string.select_new_image));
                binding.writeActivityGuideTextView.setVisibility(View.VISIBLE);
                binding.writeActivityGuideImageView.setVisibility(View.VISIBLE);
            }
        }
        Glide.with(getBaseContext())
                .load(imageUrl)
                .apply(new RequestOptions().centerCrop())
                .into(binding.writeActivityCoverImageView);
        binding.writeActivityCoverImageView.setVisibility(View.VISIBLE);

    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.writeActivity_childConstraintLayout:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, PICK_FROM_ALBUM_CODE);
                break;
            case R.id.writeActivity_cancelButton:
                finish();
                break;
            case R.id.writeActivity_saveButton:
                String text = String.valueOf(binding.writeActivityEditText.getText());
                if(isNew){
                    if(String.valueOf(binding.writeActivityEditText.getText()).equals("")){
                        Toast.makeText(this, getString(R.string.write_content_toast), Toast.LENGTH_SHORT).show();
                    }else uploadImage(text);
                }else if(isCover){
                    if(String.valueOf(binding.writeActivityEditText.getText()).equals("")){
                        Toast.makeText(this, getString(R.string.write_title), Toast.LENGTH_SHORT).show();
                    }else if(isImageChanged)
                        update(text);
                    else updateDatabase(text);
                }else
                    update(text);

                break;
        }
    }

    private void update(String text){
        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                        diaryCreateTime = diaryModel.getCreateTime();
                        if(isImageChanged) {
                            uploadImage(text);
                        }
                        updateDatabase(text);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void uploadImage(String text){
        if(isNew) {
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                            String diaryCreateTime = String.valueOf(diaryModel.getCreateTime());
                            long timeStamp = Calendar.getInstance().getTimeInMillis();
                            String pageCreateTime = String.valueOf(timeStamp);
                            if(isImageChanged)
                                strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(diaryCreateTime).child(pageCreateTime).putFile(imageUri)
                                    .addOnCompleteListener(task -> {
                                        strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(diaryCreateTime).child(pageCreateTime).getDownloadUrl()
                                                .addOnSuccessListener(uri -> {
                                                    String imageUrl = String.valueOf(uri);
                                                    pushPage(text, timeStamp, imageUrl);
                                                });
                                    });
                            else pushPage(text, timeStamp, null);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }else if(isCover){
            strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(text).putFile(imageUri)
                    .addOnCompleteListener(task -> {
                        strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(text).getDownloadUrl()
                                .addOnSuccessListener( uri -> {
                                    String imageUrl = String.valueOf(uri);
                                    Map<String, Object> map = new HashMap<>();
                                    map.put(getString(R.string.fdb_cover_image_url), imageUrl);
                                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).updateChildren(map);
                                });
                        updateDatabase(text);
                    });
        }else{
            strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(String.valueOf(pageCreateTime)).putFile(imageUri)
                    .addOnCompleteListener(task -> {
                        strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(String.valueOf(pageCreateTime)).getDownloadUrl()
                                .addOnSuccessListener( uri -> {
                                    String imageUrl = String.valueOf(uri);
                                    Map<String, Object> map = new HashMap<>();
                                    map.put(getString(R.string.fdb_image_url), imageUrl);
                                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).child(pageKey).updateChildren(map);
                                });
                        updateDatabase(text);
                    });
        }
    }

    private void pushPage(String content, long timeStamp, String imageUrl){
        PageModel pageModel =
                new PageModel.Builder()
                        .setContent(content)
                        .setCreateTime(timeStamp)
                        .setImageUrl(imageUrl)
                        .build();

        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).push().setValue(pageModel)
                .addOnSuccessListener( aVoid -> {
                    // 저장 후 PageModel key값 바로 업데이트하기
                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).orderByChild(getString(R.string.fdb_createTime)).equalTo(timeStamp)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    String pageKey = null;
                                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                                        PageModel pageModel = snapshot.getValue(PageModel.class);
                                        if(pageModel.getContent().equals(content))
                                            pageKey = snapshot.getKey();
                                    }

                                    Log.d("pageKey", pageKey);
                                    Map<String, Object> map = new HashMap<>();
                                    map.put(getString(R.string.fdb_key), pageKey);
                                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).child(pageKey).updateChildren(map)
                                            .addOnSuccessListener(aVoid1 -> {
                                                finishWriteDiaryActivity();
                                            });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });

                });
    }

    private void updateDatabase(String text){
        Map<String, Object> map = new HashMap<>();
        if(isCover) {
            String title = text;
            map.put(getString(R.string.fdb_title), title);
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).updateChildren(map)
                    .addOnSuccessListener( aVoid -> {
                        Map<String, Boolean> booleanMap = new HashMap<>();
                        boolean isPrivate = binding.writeActivitySwitch.isChecked();
                        booleanMap.put(getString(R.string.fdb_private), isPrivate);
                        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).updateChildren(map)
                                .addOnSuccessListener( aVoid1 -> {
                                    finishWriteDiaryActivity();
                                });
                    });
        }else{
            String content = text;
            map.put(getString(R.string.fdb_content), content);
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).child(pageKey).updateChildren(map)
                    .addOnSuccessListener( aVoid -> {
                        finishWriteDiaryActivity();
                    });
        }
    }

    private void finishWriteDiaryActivity(){
        Toast.makeText(getBaseContext(), getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent();
        setResult(EDIT_DIARY_CODE, intent);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(requestCode == PICK_FROM_ALBUM_CODE && resultCode == Activity.RESULT_OK){
            imageUri = data.getData();

            Glide.with(getBaseContext())
                    .load(imageUri)
                    .apply(new RequestOptions().centerCrop())
                    .into(binding.writeActivityCoverImageView);
            binding.writeActivityGuideTextView.setVisibility(View.INVISIBLE);
            binding.writeActivityGuideImageView.setVisibility(View.INVISIBLE);

            isImageChanged = true;
        }
    }
}
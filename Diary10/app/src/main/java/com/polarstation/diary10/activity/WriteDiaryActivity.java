package com.polarstation.diary10.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import gun0912.tedkeyboardobserver.TedKeyboardObserver;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.Gson;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.ActivityWriteDiaryBinding;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.NotificationModel;
import com.polarstation.diary10.model.PageModel;
import com.polarstation.diary10.model.UserModel;
import com.polarstation.diary10.util.NetworkStatus;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.polarstation.diary10.activity.DiaryActivity.IS_COVER_KEY;
import static com.polarstation.diary10.fragment.ListFragment.DIARY_KEY_KEY;
import static com.polarstation.diary10.fragment.ListFragment.IMAGE_URL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.TITLE_KEY;
import static com.polarstation.diary10.fragment.PageFragment.CONTENT_KEY;
import static com.polarstation.diary10.fragment.PageFragment.EDIT_DIARY_CODE;
import static com.polarstation.diary10.fragment.PageFragment.IS_NEW_KEY;
import static com.polarstation.diary10.fragment.PageFragment.PAGE_CREATE_TIME_KEY;
import static com.polarstation.diary10.fragment.PageFragment.PAGE_KEY_KEY;
import static com.polarstation.diary10.fragment.WriteFragment.sendRequest;
import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;

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
    private long diaryCreateTime;
    private long pageCreateTime;
    private boolean isNew;
    private int netStat;

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
        pageKey = intent.getStringExtra(PAGE_KEY_KEY);
        isNew = intent.getBooleanExtra(IS_NEW_KEY, false);
        pageCreateTime = intent.getLongExtra(PAGE_CREATE_TIME_KEY, 0);

        binding.writeActivityTitleTextView.setText(title);
        if(isNew){
            binding.writeActivityGuideImageView.setVisibility(View.VISIBLE);
            binding.writeActivityGuideTextView.setVisibility(View.VISIBLE);
            binding.writeActivityTitleTextView.setVisibility(View.VISIBLE);
            setLimitEditText(binding.writeActivityEditText);
        }else if(isCover){
            binding.writeActivityTitleTextView.setVisibility(View.INVISIBLE);
            binding.writeActivitySwitch.setVisibility(View.VISIBLE);
            binding.writeActivityEditText.setText(title);
            binding.writeActivityEditText.setHint(R.string.write_title);
            binding.writeActivityEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            binding.writeActivityEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
            ViewGroup.LayoutParams params = binding.writeActivityEditText.getLayoutParams();
            params.height = params.height / 2;
        }else{
            // 페이지 수정 공통
            String content = intent.getStringExtra(CONTENT_KEY);
            binding.writeActivityEditText.setText(content);
            setLimitEditText(binding.writeActivityEditText);
            binding.writeActivityTitleTextView.setVisibility(View.VISIBLE);
            // 사진 없는 페이지 수정인 경우
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

    public static void setLimitEditText(EditText editText){
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String text = String.valueOf(editText.getText());
                String addedText = "";
                int enterCnt = 0;
                for(char c : text.toCharArray()){
                    if(c == '\n') enterCnt++;
                }

                if(1 < enterCnt){
                    if(1 < text.split("\n").length) {
                        try {
                            text = text.split("\n")[0] + "\n" + text.split("\n")[1] + text.split("\n")[2];
                        }catch(Exception e){
                            text = text.substring(0, text.length() - 1);
                        }
                    }else
                        text = text.substring(0, text.length() - 1);
                    editText.setText(text);
                    editText.setSelection(text.length());
                }// 2줄, 첫줄 15글자 초과
                else if(1 < text.split("\n").length && 20 < text.split("\n")[0].length()){
                    addedText = String.valueOf(text.split("\n")[0].charAt(text.split("\n")[0].length() - 1));
                    text = text.split("\n")[0].substring(0, 20) + "\n" + addedText + text.split("\n")[1];
                    editText.setText(text);
                    if(21 < text.length())
                        editText.setSelection(text.indexOf("\n") + 2);
                }// 2줄, 둘째줄 15글자 초과
                else if(1 < text.split("\n").length && 20 < text.split("\n")[1].length()){
                    text = text.split("\n")[0] + "\n" + text.split("\n")[1].substring(0, 15);
                    editText.setText(text);
                    editText.setSelection(text.length());
                }//
                else if(!text.contains("\n") && 20 < text.length()){
                    addedText = String.valueOf(text.charAt(text.length() - 1));
                    text = text.substring(0, 20) + "\n" + addedText;
                    editText.setText(text);
                    editText.setSelection(text.indexOf("\n") + 2);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
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
                    }else
                        update(text);
                }else
                    update(text);

                break;
        }
    }

    private void setViewWhenUploading(){
        binding.writeDiaryActivityProgressLayout.setVisibility(View.VISIBLE);
        binding.writeActivityChildConstraintLayout.setEnabled(false);
        binding.writeActivityCoverImageView.setEnabled(false);
        binding.writeActivityEditText.setEnabled(false);
        binding.writeActivitySwitch.setEnabled(false);
        binding.writeActivitySaveButton.setEnabled(false);
        binding.writeActivityCancelButton.setEnabled(false);
    }

    private void update(String text){
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                            diaryCreateTime = diaryModel.getCreateTime();
                            if (isImageChanged)
                                uploadImage(text);
                            else
                                updateDatabase(text);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void uploadImage(String text){
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(isNew && netStat == TYPE_CONNECTED) {
            setViewWhenUploading();
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
        }else if(isCover && netStat == TYPE_CONNECTED){
            setViewWhenUploading();
            strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(uid).putFile(imageUri)
                    .addOnCompleteListener(task -> {
                        strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(uid).getDownloadUrl()
                                .addOnSuccessListener( uri -> {
                                    String imageUrl = String.valueOf(uri);
                                    Map<String, Object> map = new HashMap<>();
                                    map.put(getString(R.string.fdb_cover_image_url), imageUrl);
                                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).updateChildren(map);
                                    updateDatabase(text);
                                });

                    });
        }else if(netStat == TYPE_CONNECTED){
            setViewWhenUploading();
            strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(String.valueOf(pageCreateTime)).putFile(imageUri)
                    .addOnCompleteListener(task -> {
                        strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(String.valueOf(pageCreateTime)).getDownloadUrl()
                                .addOnSuccessListener( uri -> {
                                    String imageUrl = String.valueOf(uri);
                                    Map<String, Object> map = new HashMap<>();
                                    map.put(getString(R.string.fdb_image_url), imageUrl);
                                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).child(pageKey).updateChildren(map);
                                    updateDatabase(text);
                                });
                    });
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void pushPage(String content, long timeStamp, String imageUrl){
        PageModel pageModel =
                new PageModel.Builder()
                        .setContent(content)
                        .setCreateTime(timeStamp)
                        .setImageUrl(imageUrl)
                        .build();

        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).push().setValue(pageModel)
                    .addOnSuccessListener(aVoid -> {
                        // 저장 후 PageModel key값 바로 업데이트하기
                        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).orderByChild(getString(R.string.fdb_createTime)).equalTo(timeStamp)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        String pageKey = null;
                                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            PageModel pageModel = snapshot.getValue(PageModel.class);
                                            if (pageModel.getContent().equals(content))
                                                pageKey = snapshot.getKey();
                                        }
//                                        Log.d("pageKey", pageKey);
                                        Map<String, Object> map = new HashMap<>();
                                        map.put(getString(R.string.fdb_key), pageKey);
                                        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).child(pageKey).updateChildren(map)
                                                .addOnSuccessListener(aVoid1 -> {
                                                    Toast.makeText(getBaseContext(), getString(R.string.uploaded), Toast.LENGTH_SHORT).show();

                                                    sendFCM(diaryKey);
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });
                    });
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    public void sendFCM(String diaryKey){
        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                        Map<String, Boolean> likeUsers = diaryModel.getLikeUsers();

                        for(String user : likeUsers.keySet()){
                            if(likeUsers.get(user)) {
                                dbInstance.getReference().child(getString(R.string.fdb_users)).child(user)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                UserModel destinationUserModel = dataSnapshot.getValue(UserModel.class);
                                                String titleOfDiary = String.valueOf(binding.writeActivityTitleTextView.getText());
                                                sendRequest(getBaseContext(), destinationUserModel, titleOfDiary);

                                                finishWithEditResult();
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                            }
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void updateDatabase(String text){
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            Map<String, Object> map = new HashMap<>();
            if (isCover) {
                String title = text;
                map.put(getString(R.string.fdb_title), title);
                dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).updateChildren(map)
                        .addOnSuccessListener(aVoid -> {
                            Map<String, Object> booleanMap = new HashMap<>();
                            boolean isPrivate = binding.writeActivitySwitch.isChecked();
                            booleanMap.put(getString(R.string.fdb_private), isPrivate);
                            dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).updateChildren(booleanMap)
                                    .addOnSuccessListener(aVoid1 -> {
                                        Toast.makeText(getBaseContext(), getString(R.string.edit_complete), Toast.LENGTH_SHORT).show();
                                        finishWithEditResult();
                                    });
                        });
            } else {
                String content = text;
                map.put(getString(R.string.fdb_content), content);
                dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).child(pageKey).updateChildren(map)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getBaseContext(), getString(R.string.edit_complete), Toast.LENGTH_SHORT).show();
                            finishWithEditResult();
                        });
            }
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void finishWithEditResult(){
//        Toast.makeText(getBaseContext(), getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
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
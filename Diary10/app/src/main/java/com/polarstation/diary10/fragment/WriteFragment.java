package com.polarstation.diary10.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.InputFilter;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.Gson;
import com.polarstation.diary10.R;
import com.polarstation.diary10.activity.BaseActivity;
import com.polarstation.diary10.databinding.FragmentWriteBinding;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.NotificationModel;
import com.polarstation.diary10.model.PageModel;
import com.polarstation.diary10.model.UserModel;
import com.polarstation.diary10.util.NetworkStatus;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import gun0912.tedkeyboardobserver.TedKeyboardObserver;
import io.reactivex.Observable;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.polarstation.diary10.activity.MainActivityKt.ACCOUNT_TYPE;
import static com.polarstation.diary10.activity.MainActivityKt.LIST_KEY;
import static com.polarstation.diary10.activity.MainActivityKt.LIST_TYPE;
import static com.polarstation.diary10.activity.MainActivityKt.NEW_DIARY_TYPE;
import static com.polarstation.diary10.activity.MainActivityKt.NEW_PAGE_TYPE;
import static com.polarstation.diary10.activity.MainActivityKt.TYPE_KEY;
import static com.polarstation.diary10.fragment.AccountFragment.PICK_FROM_ALBUM_CODE;
import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;

public class WriteFragment extends Fragment implements View.OnClickListener{
    private static final String CONTENT_FIRST_LINE_KEY = "firstLine";
    private static final String CONTENT_SECOND_LINE_KEY = "secondLine";
    private static final String PREFERENCE = "pref";
    private FragmentWriteBinding binding;
    private FirebaseStorage strInstance;
    private FirebaseDatabase dbInstance;
    private String imageUrl;
    private Uri imageUri;
    private String uid;
    private Bundle bundle;
    private MainFragmentCallBack callback;
    private int type;
    private int netStat;
    private boolean isImageChanged;
    private Context context;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_write, container,false);
        BaseActivity.setGlobalFont(binding.getRoot());

        isImageChanged = false;
        netStat = NetworkStatus.getConnectivityStatus(context);
        if(netStat == TYPE_CONNECTED) {
            strInstance = FirebaseStorage.getInstance();
            dbInstance = FirebaseDatabase.getInstance();
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            bundle = getArguments();
            type = NEW_DIARY_TYPE;
            if(bundle != null)
                type = bundle.getInt(TYPE_KEY, 1);

            setUI(type);
            if(type == NEW_PAGE_TYPE){
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(getString(R.string.create_diary_or_page)).setPositiveButton(getString(R.string.confirm), ((dialogInterface, i) ->{
                    type = NEW_DIARY_TYPE;
                    setUI(type);
                })).setNegativeButton(getString(R.string.cancel), ((dialogInterface, i) -> {}
                )).show();
            }

            binding.writeFragmentChildConstraintLayout.setOnClickListener(this);
            setSaveButtonListener();
            binding.writeFragmentCancelButton.setOnClickListener(this);

            // 키보드 올라오거나 내려갈때 이벤트
            new TedKeyboardObserver(callback.getActivity()).listen(isShow-> {
                if(!isShow){
                    binding.writeFragmentRootConstraintLayout.clearFocus();
                }
            });
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();

        return binding.getRoot();
    }

    private void setUI(int type){
        switch(type){
            case NEW_DIARY_TYPE:
                binding.writeFragmentSpinner.setVisibility(View.INVISIBLE);
                binding.writeFragmentEditText2.setVisibility(View.INVISIBLE);
//                binding.writeFragmentHashTagEditText.setVisibility(View.VISIBLE);
                binding.writeFragmentSwitch.setVisibility(View.VISIBLE);
                binding.writeFragmentGuideTextView.setText(R.string.select_cover);
                binding.writeFragmentEditText.setHint(R.string.write_title);
                binding.writeFragmentEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                binding.writeFragmentEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
                break;
            case NEW_PAGE_TYPE:
                List<String> diaryTitleList = bundle.getStringArrayList(LIST_KEY);
                setSpinner(diaryTitleList);
                break;
        }
    }

    private void setSpinner(List<String> diaryTitleList){
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, diaryTitleList);
        binding.writeFragmentSpinner.setAdapter(spinnerAdapter);
    }

    private void setViewWhenUploading(){
        binding.writeFragmentProgressLayout.setVisibility(View.VISIBLE);
        binding.writeFragmentChildConstraintLayout.setEnabled(false);
        binding.writeFragmentCoverImageView.setEnabled(false);
        binding.writeFragmentEditText.setEnabled(false);
        binding.writeFragmentEditText2.setEnabled(false);
        binding.writeFragmentSwitch.setEnabled(false);
        binding.writeFragmentCancelButton.setEnabled(false);
        binding.writeFragmentSaveButton.setEnabled(false);
        callback.setNavigationViewDisabled();
    }

    private void setSaveButtonListener(){
        netStat = NetworkStatus.getConnectivityStatus(context);
        if(netStat == TYPE_CONNECTED) {
            binding.writeFragmentSaveButton.setOnClickListener(v -> {
                switch (type) {
                    case NEW_DIARY_TYPE:
                        if (binding.writeFragmentCoverImageView.getVisibility() == View.INVISIBLE) {
                            Toast.makeText(context, getString(R.string.select_cover), Toast.LENGTH_SHORT).show();
                        } else if (String.valueOf(binding.writeFragmentEditText.getText()).equals("")) {
                            Toast.makeText(context, getString(R.string.write_title), Toast.LENGTH_SHORT).show();
                        } else {
                            String title = String.valueOf(binding.writeFragmentEditText.getText());
                            boolean isPrivate = binding.writeFragmentSwitch.isChecked();
                            long createTime = Calendar.getInstance().getTimeInMillis();

                            setViewWhenUploading();
                            strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(createTime)).child(uid).putFile(imageUri)
                                    .addOnCompleteListener(task -> {
                                        strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(createTime)).child(uid).getDownloadUrl().addOnSuccessListener(uri -> {
                                            imageUrl = String.valueOf(uri);
                                            DiaryModel diaryModel =
                                                    new DiaryModel.Builder()
                                                            .setTitle(title)
                                                            .setUid(uid)
                                                            .setCoverImageUrl(imageUrl)
                                                            .setIsPrivate(isPrivate)
                                                            .setCreateTime(createTime)
                                                            .build();

                                            dbInstance.getReference().child(getString(R.string.fdb_diaries)).push().setValue(diaryModel).addOnSuccessListener(aVoid -> {
                                                // DiaryModel 저장 후 바로 key값 업데이트
                                                dbInstance.getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_title)).equalTo(title)
                                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                                // RxJava
                                                                Observable.fromIterable(dataSnapshot.getChildren()).filter(snapshot ->
                                                                        snapshot.getValue(DiaryModel.class).getUid().equals(uid)
                                                                ).subscribe(snapshot -> {
                                                                    String key = snapshot.getKey();
                                                                    Map<String, Object> keyMap = new HashMap<>();
                                                                    keyMap.put(getString(R.string.fdb_key), key);
                                                                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(key).updateChildren(keyMap).addOnSuccessListener(aVoid -> {

                                                                        new Handler().postDelayed(() -> callback.replaceFragment(ACCOUNT_TYPE), 1000);
                                                                    });
                                                                });
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError databaseError) {

                                                            }
                                                        });
                                            });
                                        });
                                    });
                        }
                        break;
                    case NEW_PAGE_TYPE:
                        String firstLine = String.valueOf(binding.writeFragmentEditText.getText());
                        String secondLine = String.valueOf(binding.writeFragmentEditText2.getText());
                        String content = secondLine.equals("") ? firstLine : firstLine + "\n" + secondLine;
                        String titleOfDiary = binding.writeFragmentSpinner.getSelectedItem().toString();
                        if (content.equals("")) {
                            Toast.makeText(context, getString(R.string.write_content_toast), Toast.LENGTH_SHORT).show();
                        } else {
                            setViewWhenUploading();

                            // uid와 제목이 같은 일기장에서 날짜 불러와서 그 날짜에 해당하는 storage의 폴더 안에 이미지 저장하고 데이터 삽입
                            dbInstance.getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            // RxJava
                                            Observable.fromIterable(dataSnapshot.getChildren()).filter(snapshot ->
                                                snapshot.getValue(DiaryModel.class).getTitle().equals(titleOfDiary)
                                            ).subscribe(snapshot -> {
                                                DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
                                                String diaryKey = diaryModel.getKey();
                                                String diaryCreateTime = String.valueOf(diaryModel.getCreateTime());
                                                long timeStamp = Calendar.getInstance().getTimeInMillis();
                                                String pageCreateTime = String.valueOf(timeStamp);

                                                if (!isImageChanged) {
                                                    pushPage(content, timeStamp, diaryKey, "");
                                                }else {
                                                    final String key = diaryKey;

                                                    strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(diaryCreateTime).child(pageCreateTime).putFile(imageUri)
                                                            .addOnCompleteListener(task ->
                                                                strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(diaryCreateTime).child(pageCreateTime).getDownloadUrl()
                                                                        .addOnSuccessListener(uri -> {
                                                                            String imageUrl = String.valueOf(uri);
                                                                            pushPage(content, timeStamp, key, imageUrl);
                                                                        })
                                                            );
                                                }
                                            });
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }
                        break;
                }
            });
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void pushPage(String content, long timeStamp, String diaryKey, String imageUrl){
        PageModel pageModel =
                new PageModel.Builder()
                        .setContent(content)
                        .setCreateTime(timeStamp)
                        .setImageUrl(imageUrl)
                        .build();

        netStat = NetworkStatus.getConnectivityStatus(context);
        if(netStat == TYPE_CONNECTED) {
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).push().setValue(pageModel)
                    .addOnSuccessListener(aVoid -> {
//                        Log.d("timestamp", timeStamp + "");
                        // 저장 후 PageModel key값 바로 업데이트하기
                        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).orderByChild(getString(R.string.fdb_createTime)).equalTo(timeStamp)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                        Log.d("WriteFragment", dataSnapshot.getChildrenCount() + "");
                                        String pageKey = null;
                                        for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                            pageKey = snapshot.getKey();
                                        }
//                                            Log.d("pageKey", pageKey);
                                        Map<String, Object> map = new HashMap<>();
                                        map.put(getString(R.string.fdb_key), pageKey);
                                        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).child(pageKey).updateChildren(map)
                                                .addOnSuccessListener(aVoid1 -> {
                                                    sendFCM(diaryKey);
                                                    Toast.makeText(context.getApplicationContext(), getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {
                                        Log.d("DB Error", databaseError.toString());
                                    }
                                });

                    }).addOnCanceledListener(new OnCanceledListener() {
                        @Override
                        public void onCanceled() {
                            Toast.makeText(context, "실패", Toast.LENGTH_SHORT).show();
                        }
                    }).addOnFailureListener(aVoid->{
                        Toast.makeText(context, "실패", Toast.LENGTH_SHORT).show();
                    });
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    public void sendFCM(String diaryKey){
        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                        Map<String, Boolean> likeUsers = diaryModel.getLikeUsers();

                        // RxJava
                        Observable.fromIterable(likeUsers.keySet()).filter(user ->
                            likeUsers.get(user)
                        ).subscribe(user ->
                            dbInstance.getReference().child(getString(R.string.fdb_users)).child(user)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            UserModel destinationUserModel = dataSnapshot.getValue(UserModel.class);
                                            String titleOfDiary = binding.writeFragmentSpinner.getSelectedItem().toString();
                                            sendRequest(context, destinationUserModel, titleOfDiary);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    })
                        );
                        callback.replaceFragment(ACCOUNT_TYPE);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    public static void sendRequest(Context context, UserModel destinationUserModel, String titleOfDiary){
        Gson gson = new Gson();
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();

        String title = context.getString(R.string.fcm_title);
        String text = userName + context.getString(R.string.fcm_text_1) + " " + titleOfDiary + " " ;

        NotificationModel notificationModel = new NotificationModel(destinationUserModel.getPushToken());
//        Log.d("FCM, userName", destinationUserModel.getUserName() + " / " + destinationUserModel.getPushToken());
        notificationModel.getData().setTitle(title);
        notificationModel.getData().setText(text);


        RequestBody requestBody = RequestBody.create(MediaType.parse(context.getString(R.string.media_type)), gson.toJson(notificationModel));
        okhttp3.Request request = new Request.Builder()
                .header(context.getString(R.string.request_header_content_type), context.getString(R.string.request_header_content_type_value))
                .addHeader(context.getString(R.string.request_header_authorization), context.getString(R.string.request_header_authorization_value))
                .url(context.getString(R.string.fcm_send_url))
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d("OkHttp", response.toString());

            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == PICK_FROM_ALBUM_CODE && resultCode == Activity.RESULT_OK){
            imageUri = data.getData();

            binding.writeFragmentGuideImageView.setVisibility(View.INVISIBLE);
            binding.writeFragmentGuideTextView.setVisibility(View.INVISIBLE);
            binding.writeFragmentCoverImageView.setVisibility(View.VISIBLE);

            Glide.with(context)
                    .load(imageUri)
                    .apply(new RequestOptions().centerCrop())
                    .into(binding.writeFragmentCoverImageView);

            isImageChanged = true;
        }

        SharedPreferences pref = context.getSharedPreferences(PREFERENCE, Activity.MODE_PRIVATE);
        if(pref != null) {
            binding.writeFragmentEditText.setText(pref.getString(CONTENT_FIRST_LINE_KEY, ""));
            binding.writeFragmentEditText2.setText(pref.getString(CONTENT_SECOND_LINE_KEY, ""));
        }
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.writeFragment_childConstraintLayout:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
                startActivityForResult(intent, PICK_FROM_ALBUM_CODE);
                break;
            case R.id.writeFragment_cancelButton:
                binding.writeFragmentCoverImageView.setVisibility(View.INVISIBLE);
                binding.writeFragmentSwitch.setChecked(false);
                binding.writeFragmentEditText.setText("");
                callback.replaceFragment(LIST_TYPE);
                break;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        SharedPreferences pref = context.getSharedPreferences(PREFERENCE, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(CONTENT_FIRST_LINE_KEY, String.valueOf(binding.writeFragmentEditText.getText()));
        editor.putString(CONTENT_SECOND_LINE_KEY, String.valueOf(binding.writeFragmentEditText2.getText()));
        editor.apply();

        binding.writeFragmentEditText.setText("");
        binding.writeFragmentEditText2.setText("");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof MainFragmentCallBack)
            callback = (MainFragmentCallBack) context;
        this.context = context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback.findMyDiary();
        if(callback != null)
            callback = null;
    }
}
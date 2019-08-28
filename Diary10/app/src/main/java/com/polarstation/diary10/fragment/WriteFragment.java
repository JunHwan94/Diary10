package com.polarstation.diary10.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.gson.Gson;
import com.polarstation.diary10.activity.BaseActivity;
import com.polarstation.diary10.R;
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
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.polarstation.diary10.activity.MainActivity.LIST_KEY;
import static com.polarstation.diary10.activity.MainActivity.NEW_DIARY_TYPE;
import static com.polarstation.diary10.activity.MainActivity.NEW_PAGE_TYPE;
import static com.polarstation.diary10.activity.MainActivity.TYPE_KEY;
import static com.polarstation.diary10.activity.WriteDiaryActivity.setLimitEditText;
import static com.polarstation.diary10.fragment.AccountFragment.PICK_FROM_ALBUM_CODE;
import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;

public class WriteFragment extends Fragment implements View.OnClickListener{
    private static FragmentWriteBinding binding;
    private FirebaseStorage strInstance;
    private FirebaseDatabase dbInstance;
    private String imageUrl;
    private Uri imageUri;
    private String uid;
    private Bundle bundle;
    private MainFragmentCallBack callback;
    private int type;
    private int netStat;
    private boolean isImageChanged = false;

    public static final int LIST_TYPE = 0;
    public static final int CREATE_TYPE = 1;
    public static final int WRITE_TYPE= 2;
    public static final int ACCOUNT_TYPE = 3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_write, container,false);
        BaseActivity.setGlobalFont(binding.getRoot());

        netStat = NetworkStatus.getConnectivityStatus(getContext());
        binding.writeFragmentEditText.setText("");

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
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(getString(R.string.create_diary_or_page)).setPositiveButton(getString(R.string.confirm), ((dialogInterface, i) ->{
                    type = NEW_DIARY_TYPE;
                    setUI(type);
                })).setNegativeButton(getString(R.string.cancel), ((dialogInterface, i) ->
                    setUI(type)
                )).show();

                setUI(type);
            }

            binding.writeFragmentChildConstraintLayout.setOnClickListener(this);
            setSaveButtonListener();
            binding.writeFragmentCancelButton.setOnClickListener(this);

            // 키보드 올라오거나 내려갈때 이벤트
            new TedKeyboardObserver(getActivity()).listen(isShow-> {
                if(!isShow){
                    binding.writeFragmentEditText.clearFocus();
                }
            });
        }else Toast.makeText(getContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();

        return binding.getRoot();
    }

    private void setUI(int type){
        switch(type){
            case NEW_DIARY_TYPE:
                binding.writeFragmentSpinner.setVisibility(View.INVISIBLE);
                binding.writeFragmentSwitch.setVisibility(View.VISIBLE);
                binding.writeFragmentGuideTextView.setText(R.string.select_cover);
                binding.writeFragmentEditText.setHint(R.string.write_title);
                binding.writeFragmentEditText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                binding.writeFragmentEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
                ViewGroup.LayoutParams params = binding.writeFragmentEditText.getLayoutParams();
                params.height = params.height / 2;
                break;
            case NEW_PAGE_TYPE:
                List<String> diaryTitleList = bundle.getStringArrayList(LIST_KEY);
                setSpinner(diaryTitleList);
                setLimitEditText(binding.writeFragmentEditText);
                break;
        }
    }

    private void setSpinner(List<String> diaryTitleList){
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, diaryTitleList);
        binding.writeFragmentSpinner.setAdapter(spinnerAdapter);
    }

    private void setViewWhenUploading(){
        binding.writeFragmentProgressLayout.setVisibility(View.VISIBLE);
        binding.writeFragmentChildConstraintLayout.setEnabled(false);
        binding.writeFragmentCoverImageView.setEnabled(false);
        binding.writeFragmentEditText.setEnabled(false);
        binding.writeFragmentSwitch.setEnabled(false);
        binding.writeFragmentCancelButton.setEnabled(false);
        binding.writeFragmentSaveButton.setEnabled(false);
        callback.setNavigationViewDisabled();
    }

    private void setSaveButtonListener(){
        netStat = NetworkStatus.getConnectivityStatus(getContext());
        if(netStat == TYPE_CONNECTED) {
            binding.writeFragmentSaveButton.setOnClickListener(v -> {
                String text = String.valueOf(binding.writeFragmentEditText.getText());
                switch (type) {
                    case NEW_DIARY_TYPE:
                        if (binding.writeFragmentCoverImageView.getVisibility() == View.INVISIBLE) {
                            Toast.makeText(getContext(), getString(R.string.select_cover), Toast.LENGTH_SHORT).show();
                        } else if (String.valueOf(binding.writeFragmentEditText.getText()).equals("")) {
                            Toast.makeText(getContext(), getString(R.string.write_title), Toast.LENGTH_SHORT).show();
                        } else {
                            String title = text;
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
                                                                String key = null;
                                                                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                                                                    DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
                                                                    if (diaryModel.getUid().equals(uid))
                                                                        key = snapshot.getKey();
                                                                }
                                                                Map<String, Object> keyMap = new HashMap<>();
                                                                keyMap.put(getString(R.string.fdb_key), key);
                                                                dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(key).updateChildren(keyMap).addOnSuccessListener(aVoid -> {

                                                                    new Handler().postDelayed(() -> callback.replaceFragment(ACCOUNT_TYPE), 1000);
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
                        String content = text;
                        String titleOfDiary = binding.writeFragmentSpinner.getSelectedItem().toString();
                        if (content.equals("")) {
                            Toast.makeText(getContext(), getString(R.string.write_content_toast), Toast.LENGTH_SHORT).show();
                        } else {
                            setViewWhenUploading();

                            // uid와 제목이 같은 일기장에서 날짜 불러와서 그 날짜에 해당하는 storage의 폴더 안에 이미지 저장하고 데이터 삽입
                            dbInstance.getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            DiaryModel diaryModel = null;
                                            String diaryKey = "";
                                            for (DataSnapshot item : dataSnapshot.getChildren()) {
                                                diaryModel = item.getValue(DiaryModel.class);
                                                if (diaryModel.getTitle().equals(titleOfDiary)) {
                                                    diaryKey = item.getKey();
                                                    break;
                                                }
                                            }
                                            String diaryCreateTime = String.valueOf(diaryModel.getCreateTime());
                                            long timeStamp = Calendar.getInstance().getTimeInMillis();
                                            String pageCreateTime = String.valueOf(timeStamp);

                                            if (!isImageChanged) {
                                                pushPage(content, timeStamp, diaryKey, "");
                                            } else {
                                                final String key = diaryKey;
                                                strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(diaryCreateTime).child(pageCreateTime).putFile(imageUri)
                                                        .addOnCompleteListener(task -> {
                                                            strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(diaryCreateTime).child(pageCreateTime).getDownloadUrl()
                                                                    .addOnSuccessListener(uri -> {
                                                                        String imageUrl = String.valueOf(uri);
                                                                        pushPage(content, timeStamp, key, imageUrl);
                                                                    });
                                                        });
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }
                        break;
                }

                binding.writeFragmentEditText.setText("");
            });
        }else Toast.makeText(getContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void pushPage(String content, long timeStamp, String diaryKey, String imageUrl){
        PageModel pageModel =
                new PageModel.Builder()
                        .setContent(content)
                        .setCreateTime(timeStamp)
                        .setImageUrl(imageUrl)
                        .build();

        netStat = NetworkStatus.getConnectivityStatus(getContext());
        if(netStat == TYPE_CONNECTED) {
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).push().setValue(pageModel)
                    .addOnSuccessListener(aVoid -> {
//                        Log.d("timestamp", timeStamp + "");
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
                                                    sendFCM(diaryKey);
                                                    Toast.makeText(getContext().getApplicationContext(), getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                    });
        }else Toast.makeText(getContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    public void sendFCM(String diaryKey){
        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                        Map<String, Boolean> likeUsers = diaryModel.getLikeUsers();

                        for(String user : likeUsers.keySet()){
                            Log.d("FCM, destUid", user);
                            if(likeUsers.get(user)) {
                                Log.d("FCM, like?", likeUsers.get(user)+"");
                                dbInstance.getReference().child(getString(R.string.fdb_users)).child(user)
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                                UserModel destinationUserModel = dataSnapshot.getValue(UserModel.class);
                                                String titleOfDiary = binding.writeFragmentSpinner.getSelectedItem().toString();
                                                sendRequest(getContext(), destinationUserModel, titleOfDiary);

                                                callback.replaceFragment(ACCOUNT_TYPE);
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

    public static void sendRequest(Context context, UserModel destinationUserModel, String titleOfDiary){
        Gson gson = new Gson();
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();

        String title = context.getString(R.string.fcm_title);
        String text = userName + context.getString(R.string.fcm_text_1) + " " + titleOfDiary + " " ;

        NotificationModel notificationModel = new NotificationModel(destinationUserModel.getPushToken());
        Log.d("FCM, userName", destinationUserModel.getUserName() + " / " + destinationUserModel.getPushToken());
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

            Glide.with(getContext())
                    .load(imageUri)
                    .apply(new RequestOptions().centerCrop())
                    .into(binding.writeFragmentCoverImageView);

            isImageChanged = true;
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
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof MainFragmentCallBack)
            callback = (MainFragmentCallBack) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback.findMyDiary();
        if(callback != null)
            callback = null;
    }
}

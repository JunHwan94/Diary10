package com.polarstation.diary10.fragment;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.polarstation.diary10.BaseActivity;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.FragmentWriteBinding;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.PageModel;

import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import static com.polarstation.diary10.MainActivity.LIST_KEY;
import static com.polarstation.diary10.MainActivity.NEW_DIARY_TYPE;
import static com.polarstation.diary10.MainActivity.NEW_PAGE_TYPE;
import static com.polarstation.diary10.MainActivity.TYPE_KEY;
import static com.polarstation.diary10.Util.DialogUtils.showProgressDialog;
import static com.polarstation.diary10.fragment.AccountFragment.PICK_FROM_ALBUM_CODE;

public class WriteFragment extends Fragment implements View.OnClickListener{
    private FragmentWriteBinding binding;
    private FirebaseStorage strInstance = FirebaseStorage.getInstance();
    private FirebaseDatabase dbInstance = FirebaseDatabase.getInstance();
    private String imageUrl;
    private Uri imageUri;
    private String uid;
    private ProgressDialog progressDialog;
    private Bundle bundle;
    private FragmentCallBack callback;


    public static final int LIST_TYPE = 0;
    public static final int CREATE_TYPE = 1;
    public static final int WRITE_TYPE= 2;
    public static final int ACCOUNT_TYPE = 3;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_write, container,false);
        BaseActivity.setGlobalFont(binding.getRoot());

        bundle = getArguments();
        int type = bundle.getInt(TYPE_KEY, 0);
        if(type == NEW_PAGE_TYPE){
            //새 일기장 만들건지 다이얼로그 보여주기
            // 확인 -> type = NEW_DIARY_TYPE
            // 취소 -> 진행
        }
        setUI(type);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        progressDialog = new ProgressDialog(getContext());

        binding.writeFragmentChildConstraintLayout.setOnClickListener(this);
        setSaveButtonListener(type);
        binding.writeFragmentCancelButton.setOnClickListener(this);

        return binding.getRoot();
    }

    private void setUI(int type){
        switch(type){
            case NEW_DIARY_TYPE:
                break;
            case NEW_PAGE_TYPE:
                List<String> diaryTitleList = bundle.getStringArrayList(LIST_KEY);
                setSpinner(diaryTitleList);
                binding.writeFragmentSpinner.setVisibility(View.VISIBLE);
                binding.writeFragmentSwitch.setVisibility(View.INVISIBLE);
                binding.writeFragmentGuideTextView.setText(R.string.select_image);
                binding.writeFragmentEditText.setHint(R.string.write_content);
                break;
        }
    }

    private void setSpinner(List<String> diaryTitleList){
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, diaryTitleList);
        binding.writeFragmentSpinner.setAdapter(spinnerAdapter);
    }

    private void setSaveButtonListener(int type){
        binding.writeFragmentSaveButton.setOnClickListener(v -> {
            String text = String.valueOf(binding.writeFragmentEditText.getText());
            switch(type){
                case NEW_DIARY_TYPE:
                    if(binding.writeFragmentCoverImageView.getVisibility() == View.INVISIBLE)
                        Toast.makeText(getContext(), getString(R.string.select_cover), Toast.LENGTH_SHORT).show();
                    else {
                        showProgressDialog(progressDialog, getString(R.string.uploading));

                        String title = text;
                        boolean isPrivate = binding.writeFragmentSwitch.isChecked();
                        long createTime = Calendar.getInstance().getTimeInMillis();
                        strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(createTime)).child(title).putFile(imageUri)
                                .addOnCompleteListener(task -> {
                                    strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(createTime)).child(title).getDownloadUrl().addOnSuccessListener(uri -> {
                                        imageUrl = String.valueOf(uri);
                                        DiaryModel diaryModel =
                                                new DiaryModel.Builder()
                                                        .setTitle(title)
                                                        .setUid(uid)
                                                        .setCoverImageUrl(imageUrl)
                                                        .setIsPrivate(isPrivate)
                                                        .setCreateTime(createTime)
                                                        .build();

                                        dbInstance.getReference().child(getString(R.string.fdb_diaries)).push().setValue(diaryModel);
                                    });

                                });
                    }
                    // 바로 쓸까요? 다이얼로그로 보여주고 확인하면 새 쓰기 프래그먼트
                    // 아니면 리스트 프래그먼트로 replace
                    /// Dialog.
//                    callback.replaceFragment(type);
                    break;
                case NEW_PAGE_TYPE:
                    String content = text;
                    String titleOfDiary = binding.writeFragmentSpinner.getSelectedItem().toString();
                    // uid와 제목이 같은 일기장에서 날짜 불러와서 그 날짜에 해당하는 storage의 폴더 안에 이미지 저장하고 데이터 삽입
                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    DiaryModel diaryModel = null;
                                    for (DataSnapshot item : dataSnapshot.getChildren()) {
                                        diaryModel = item.getValue(DiaryModel.class);
                                        if (diaryModel.getTitle().equals(titleOfDiary))
                                            break;
                                    }
                                    String diaryCreateTime = String.valueOf(diaryModel.getCreateTime());
                                    String pageCreateTime = String.valueOf(Calendar.getInstance().getTimeInMillis());
                                    String diaryKey = dataSnapshot.getKey();
                                    Object timeStamp = ServerValue.TIMESTAMP;

                                    showProgressDialog(progressDialog, getString(R.string.uploading));
                                    if(binding.writeFragmentCoverImageView.getVisibility() == View.INVISIBLE){
                                        pushPage(content, timeStamp,diaryKey, "");
                                    }else {
                                        strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(diaryCreateTime).child(pageCreateTime).putFile(imageUri)
                                                .addOnCompleteListener(task -> {
                                                    strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(diaryCreateTime).child(pageCreateTime).getDownloadUrl()
                                                            .addOnSuccessListener(uri -> {
                                                                String imageUrl = String.valueOf(uri);

                                                                pushPage(content, timeStamp, diaryKey, imageUrl);

                                                                Toast.makeText(getContext(), getString(R.string.uploaded), Toast.LENGTH_SHORT).show();
                                                                // 저장 후 내 페이지로 프래그먼트 변경
                                                                callback.replaceFragment(ACCOUNT_TYPE);
                                                            });
                                                });
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                    break;
            }
        });
    }

    private void pushPage(String content, Object timeStamp, String diaryKey, String imageUrl){
        PageModel pageModel =
                new PageModel.Builder()
                        .setContent(content)
                        .setCreateTime(timeStamp)
                        .setImageUrl(imageUrl)
                        .build();

        dbInstance.getReference().child(diaryKey).child(getString(R.string.fdb_pages)).push().setValue(pageModel)
                .addOnSuccessListener( aVoid -> progressDialog.cancel());
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
            case R.id.cancel_button:
                binding.writeFragmentCoverImageView.setVisibility(View.INVISIBLE);
                binding.writeFragmentSwitch.setChecked(false);
                binding.writeFragmentEditText.setText("");
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof FragmentCallBack)
            callback = (FragmentCallBack) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callback.findMyDiary();
        if(callback != null)
            callback = null;
    }
}

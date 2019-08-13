package com.polarstation.diary10.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.polarstation.diary10.BaseActivity;
import com.polarstation.diary10.WriteDiaryActivity;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.FragmentPageBinding;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.PageModel;
import com.polarstation.diary10.model.UserModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import static com.polarstation.diary10.DiaryActivity.IS_COVER_KEY;
import static com.polarstation.diary10.DiaryActivity.PAGE_MODEL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.IMAGE_URL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.KEY_KEY;
import static com.polarstation.diary10.fragment.ListFragment.TITLE_KEY;
import static com.polarstation.diary10.fragment.ListFragment.WRITER_UID_KEY;

public class PageFragment extends Fragment implements View.OnClickListener{
    private FragmentPageBinding binding;
    private String imageUrl;
    private String uid;
    private String writerUid;
    private Animation translateLeft;
    private Animation translateRight;
    static boolean isMenuOpen = false;
    boolean isCover;
    private FirebaseDatabase dbInstance;
    private FirebaseStorage strInstance;
    private String diaryKey;
    private String pageKey;
    private FragmentCallBack callback;

    private static final int ADD_PAGE_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_page, container, false);
        BaseActivity.setGlobalFont(binding.getRoot());
        dbInstance = FirebaseDatabase.getInstance();
        strInstance = FirebaseStorage.getInstance();

        Bundle bundle = getArguments();
        if(bundle != null)
            processBundle(bundle);

        setMenu();

        binding.pageFragmentMenuButton.setOnClickListener(this);
        binding.pageFragmentDeleteDiaryButton.setOnClickListener(this);
        binding.pageFragmentEditDiaryButton.setOnClickListener(this);
        binding.pageFragmentNewPageButton.setOnClickListener(this);

        return binding.getRoot();
    }

    private void setMenu(){
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if(uid.equals(writerUid)) {
            binding.pageFragmentMenuButton.setVisibility(View.VISIBLE);

            translateLeft = AnimationUtils.loadAnimation(getContext(), R.anim.translate_left);
            translateRight = AnimationUtils.loadAnimation(getContext(), R.anim.translate_right);
            Animation.AnimationListener listener = new SlidingAnimationListener();
            translateLeft.setAnimationListener(listener);
            translateRight.setAnimationListener(listener);
        }
    }

    private void processBundle(Bundle bundle){
        isCover = bundle.getBoolean(IS_COVER_KEY);
        if(isCover){
            binding.pageFragmentDateTextView.setVisibility(View.INVISIBLE);
            binding.pageFragmentWriterImageView.setVisibility(View.VISIBLE);
            binding.pageFragmentWriterTextView.setVisibility(View.VISIBLE);
            binding.pageFragmentContentTextView.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/scdream4.otf"));
            binding.pageFragmentNewPageButton.setVisibility(View.VISIBLE);

            String title = bundle.getString(TITLE_KEY);
            writerUid = bundle.getString(WRITER_UID_KEY);
            imageUrl = bundle.getString(IMAGE_URL_KEY);
            diaryKey = bundle.getString(KEY_KEY);

            dbInstance.getReference().child(getString(R.string.fdb_users)).child(writerUid)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            UserModel userModel = dataSnapshot.getValue(UserModel.class);
                            String writer = userModel.getUserName();

                            String writerImageUrl = userModel.getProfileImageUrl();
                            binding.pageFragmentWriterTextView.setText(writer);
                            Glide.with(getContext())
                                    .load(writerImageUrl)
                                    .apply(new RequestOptions().circleCrop())
                                    .into(binding.pageFragmentWriterImageView);

                            binding.pageFragmentContentTextView.setText(title);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
            setCoverImage();
        }else{
            PageModel pageModel = bundle.getParcelable(PAGE_MODEL_KEY);
            String content = pageModel.getContent();
            long createTime = pageModel.getCreateTime();
            imageUrl = pageModel.getImageUrl();
            pageKey = pageModel.getKey();
            diaryKey = bundle.getString(KEY_KEY);
            writerUid = bundle.getString(WRITER_UID_KEY);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 M월 d일");
            sdf.setTimeZone(TimeZone.getTimeZone(getString(R.string.time_zone)));
            Date date = new Date(createTime);

            binding.pageFragmentContentTextView.setTextSize(22.0f);
            binding.pageFragmentContentTextView.setText(content);
            binding.pageFragmentDateTextView.setText(sdf.format(date));

            binding.pageFragmentDeleteDiaryButton.setText(getString(R.string.delete_page));
            binding.pageFragmentEditDiaryButton.setText(getString(R.string.edit_page));
            binding.pageFragmentSlideMenu.removeView(binding.pageFragmentNewPageButton);
        }

        Glide.with(getContext())
                .load(imageUrl)
                .apply(new RequestOptions().centerCrop())
                .into(binding.pageFragmentImageView);
    }

    private void setCoverImage(){
        DisplayMetrics metrics = getMetrics();
        ViewGroup.LayoutParams params = binding.pageFragmentImageView.getLayoutParams();
        params.width = metrics.widthPixels;
        params.height = metrics.heightPixels;
        binding.pageFragmentLabel.setBackgroundColor(getResources().getColor(R.color.trans_white_deep));
    }

    private DisplayMetrics getMetrics(){
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) getContext().getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        return metrics;
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.pageFragment_menuButton:
                if(isMenuOpen)
                    binding.pageFragmentSlideMenu.startAnimation(translateRight);
                else {
                    binding.pageFragmentSlideMenu.setVisibility(View.VISIBLE);
                    binding.pageFragmentSlideMenu.startAnimation(translateLeft);
                }
                break;
            case R.id.pageFragment_deleteDiaryButton:
                if(isCover){
                    Log.d("diaryKey" ,diaryKey + "");
                    Log.d("uid" ,uid + "");
                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                                    long diaryCreateTime = diaryModel.getCreateTime();
                                    String title = diaryModel.getTitle();

                                    Log.d("diaryCreateTime" ,diaryCreateTime + "");
                                    strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(title).delete()
                                            .addOnSuccessListener( aVoid -> {
                                                dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).removeValue()
                                                        .addOnSuccessListener( aVoid1 -> {
                                                            Toast.makeText(getContext(), getString(R.string.delete_success), Toast.LENGTH_SHORT).show();

                                                            callback.finishActivity();
                                                        });
                                            });
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });


                }else{
                    Log.d("diaryKey" ,diaryKey + "");
                    Log.d("uid" ,uid + "");
                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                                    long diaryCreateTime = diaryModel.getCreateTime();
                                    long pageCreateTime = 0;
                                    for(PageModel pageModel : diaryModel.getPages().values()){
                                        if(pageModel.getKey().equals(pageKey)) {
                                            pageCreateTime = pageModel.getCreateTime();
                                            break;
                                        }
                                    }
                                    Log.d("diaryCreateTime" ,diaryCreateTime + "");
                                    Log.d("pageCreateTime" ,pageCreateTime+ "");
                                    Log.d("pageKey" ,pageKey+ "");
                                    strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(String.valueOf(pageCreateTime)).delete()
                                            .addOnSuccessListener(aVoid -> {
                                                dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).child(pageKey).removeValue()
                                                        .addOnSuccessListener(aVoid1 -> {}
                                                                // DiaryActivity애서 파이어베이스로 요청해서 업데이트 callback쓰기
                                                        );
                                            });

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                }
                break;
            case R.id.pageFragment_editDiaryButton:
                //diarykey 전달, 커버 이미지, 제목 수정
                Intent intent = new Intent(getContext(), WriteDiaryActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivityForResult(intent, ADD_PAGE_CODE);
                break;
            case R.id.pageFragment_newPageButton:
                //diarykey 전달, 하위 pages에 추가하는 액티비티
                Intent intent1 = new Intent(getContext(), WriteDiaryActivity.class);
                intent1.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);

                startActivity(intent1);
                break;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof FragmentCallBack)
            callback = (FragmentCallBack)context;
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

    class SlidingAnimationListener implements Animation.AnimationListener{
        @Override
        public void onAnimationStart(Animation animation) {

        }

        @Override
        public void onAnimationEnd(Animation animation) {
            if(isMenuOpen){
                binding.pageFragmentSlideMenu.setVisibility(View.INVISIBLE);
                isMenuOpen = false;
            }else
                isMenuOpen = true;
        }

        @Override
        public void onAnimationRepeat(Animation animation) {

        }
    }
}

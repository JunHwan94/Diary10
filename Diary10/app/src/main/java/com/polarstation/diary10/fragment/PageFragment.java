package com.polarstation.diary10.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.android.gms.ads.AdRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.polarstation.diary10.activity.BaseActivity;
import com.polarstation.diary10.activity.WriteDiaryActivity;
import com.polarstation.diary10.R;
import com.polarstation.diary10.activity.WriterAccountActivity;
import com.polarstation.diary10.databinding.FragmentPageBinding;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.PageModel;
import com.polarstation.diary10.model.UserModel;
import com.polarstation.diary10.util.NetworkStatus;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;

import static com.polarstation.diary10.activity.DiaryActivity.IS_COVER_KEY;
import static com.polarstation.diary10.activity.DiaryActivity.PAGE_MODEL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.IMAGE_URL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.DIARY_KEY_KEY;
import static com.polarstation.diary10.fragment.ListFragment.TITLE_KEY;
import static com.polarstation.diary10.fragment.ListFragment.WRITER_UID_KEY;
import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;

public class PageFragment extends Fragment implements View.OnClickListener{
    private FragmentPageBinding binding;
    private String imageUrl;
    private String uid;
    private String writerUid;
    private Animation translateLeft;
    private Animation translateRight;
    private static boolean isMenuOpen = false;
    private boolean isCover;
    private FirebaseDatabase dbInstance;
    private FirebaseStorage strInstance;
    private String diaryKey;
    private String pageKey;
    private String title;
    private String content;
    private long pageCreateTime;
    private PageFragmentCallback callback;
    private int netStat;
    private Context context;
    private AdRequest adRequest;

    public static final int EDIT_DIARY_CODE = 100;
    public static final String CONTENT_KEY = "contentKey";
    public static final String PAGE_KEY_KEY = "pageKeyKey";
    public static final String IS_NEW_KEY = "isNewKey";
    public static final String PAGE_CREATE_TIME_KEY = "pageCreateTimeKey";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_page, container, false);
        BaseActivity.setGlobalFont(binding.getRoot());

        isMenuOpen = false;
        netStat = NetworkStatus.getConnectivityStatus(context);
        if(netStat == TYPE_CONNECTED) {
            dbInstance = FirebaseDatabase.getInstance();
            strInstance = FirebaseStorage.getInstance();
            adRequest = new AdRequest.Builder().build();

            Bundle bundle = getArguments();
            if (bundle != null)
                processBundle(bundle);

            setMenu();
            loadLikeOrNot();

            binding.pageFragmentMenuButton.setOnClickListener(this);
            binding.pageFragmentDeleteDiaryButton.setOnClickListener(this);
            binding.pageFragmentEditDiaryButton.setOnClickListener(this);
            binding.pageFragmentWritePageButton.setOnClickListener(this);
            binding.pageFragmentWriterTextView.setOnClickListener(this);
            binding.pageFragmentLikeButton.setOnClickListener(this);
            binding.pageFragmentImageView.setOnClickListener(this);
            binding.pageFragmentLabel.setOnClickListener(this);
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();

        return binding.getRoot();
    }

    private void setMenu(){
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if(uid.equals(writerUid)) {
            binding.pageFragmentMenuButton.setVisibility(View.VISIBLE);
            binding.pageFragmentLikeButton.setVisibility(View.INVISIBLE);

            translateLeft = AnimationUtils.loadAnimation(context, R.anim.translate_left);
            translateRight = AnimationUtils.loadAnimation(context, R.anim.translate_right);
            Animation.AnimationListener listener = new SlidingAnimationListener();
            translateLeft.setAnimationListener(listener);
            translateRight.setAnimationListener(listener);
        }
    }

    private void loadLikeOrNot(){
        netStat = NetworkStatus.getConnectivityStatus(context);
        if(netStat == TYPE_CONNECTED) {
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                            Map<String, Boolean> likeUsers = diaryModel.getLikeUsers();
                            if(likeUsers.keySet().contains(uid) && likeUsers.get(uid)){
                                binding.pageFragmentLikeButton.setSelected(true);
                            }else
                                binding.pageFragmentLikeButton.setSelected(false);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void processBundle(Bundle bundle){
        isCover = bundle.getBoolean(IS_COVER_KEY);
        setViewWhenLoading();

        if(isCover){
            binding.pageFragmentDateTextView.setVisibility(View.INVISIBLE);
            binding.pageFragmentWritePageButton.setVisibility(View.VISIBLE);
            binding.pageFragmentAdView.loadAd(adRequest);

            title = bundle.getString(TITLE_KEY);
            writerUid = bundle.getString(WRITER_UID_KEY);
            imageUrl = bundle.getString(IMAGE_URL_KEY);
            diaryKey = bundle.getString(DIARY_KEY_KEY);

            netStat = NetworkStatus.getConnectivityStatus(context);
            if(netStat == TYPE_CONNECTED) {
                dbInstance.getReference().child(getString(R.string.fdb_users)).child(writerUid)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                UserModel userModel = dataSnapshot.getValue(UserModel.class);
                                String writer = userModel.getUserName();

                                String writerImageUrl = userModel.getProfileImageUrl();
                                binding.pageFragmentWriterTextView.setText(writer);
                                binding.pageFragmentContentTextView.setText(title);

                                Glide.with(context)
                                        .load(writerImageUrl)
                                        .apply(new RequestOptions().circleCrop().override(200,200))
                                        .listener(new RequestListener<Drawable>() {
                                            @Override
                                            public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                Toast.makeText(context, getString(R.string.image_load_failed), Toast.LENGTH_SHORT).show();
                                                return false;
                                            }

                                            @Override
                                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                binding.pageFragmentWriterImageView.setVisibility(View.VISIBLE);
                                                binding.pageFragmentWriterTextView.setVisibility(View.VISIBLE);

                                                return false;
                                            }
                                        })
                                        .into(binding.pageFragmentWriterImageView);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                setCoverImageViewSize();
            }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
        }else{
            PageModel pageModel = bundle.getParcelable(PAGE_MODEL_KEY);
            content = pageModel.getContent();
            pageCreateTime = pageModel.getCreateTime();
            imageUrl = pageModel.getImageUrl();
            pageKey = pageModel.getKey();
            diaryKey = bundle.getString(DIARY_KEY_KEY);
            writerUid = bundle.getString(WRITER_UID_KEY);

            SimpleDateFormat sdf = new SimpleDateFormat(getString(R.string.date_format));
//            sdf.setTimeZone(TimeZone.getTimeZone(getString(R.string.time_zone)));
            Date date = new Date(pageCreateTime);

            binding.pageFragmentContentTextView.setTextSize(22.0f);
            content = content.replace(" ", "\u00A0");
            binding.pageFragmentContentTextView.setText("\n" + content);
            binding.pageFragmentDateTextView.setText(sdf.format(date));

            binding.pageFragmentDeleteDiaryButton.setText(getString(R.string.delete_page));
            binding.pageFragmentEditDiaryButton.setText(getString(R.string.edit_page));
            binding.pageFragmentSlideMenu.removeView(binding.pageFragmentWritePageButton);
            binding.pageFragmentLikeButton.setVisibility(View.INVISIBLE);
        }

        Glide.with(context)
                .load(imageUrl)
                .apply(new RequestOptions().centerCrop().sizeMultiplier(0.4f))
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        e.printStackTrace();
                        if(!String.valueOf(imageUrl).equals(""))
                            Toast.makeText(context, getString(R.string.image_load_failed), Toast.LENGTH_SHORT).show();

                        setViewWhenDone();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        setViewWhenDone();
                        return false;
                    }
                })
                .into(binding.pageFragmentImageView);
    }

    private void setCoverImageViewSize(){
        DisplayMetrics metrics = getMetrics();
        ViewGroup.LayoutParams params = binding.pageFragmentImageView.getLayoutParams();
        params.width = metrics.widthPixels;
        params.height = metrics.heightPixels;
        binding.pageFragmentLabel.setBackgroundColor(getResources().getColor(R.color.trans_white_deep));
    }

    private DisplayMetrics getMetrics(){
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getApplicationContext()
                .getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        return metrics;
    }

    private void setViewWhenLoading(){
        binding.pageFragmentProgressLayout.setVisibility(View.VISIBLE);
        binding.pageFragmentMenuButton.setEnabled(false);
    }

    private void setViewWhenDone(){
        binding.pageFragmentProgressLayout.setVisibility(View.INVISIBLE);
        binding.pageFragmentMenuButton.setEnabled(true);
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
                netStat = NetworkStatus.getConnectivityStatus(context);
                if(isCover && netStat == TYPE_CONNECTED){
                    callback.dataChanges();
                    setViewWhenLoading();
                    dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                    DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                                    long diaryCreateTime = diaryModel.getCreateTime();
                                    long pageToDeleteCreateTime;

                                    strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(uid).delete()
                                            .addOnSuccessListener( aVoid -> {
                                                dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).removeValue()
                                                        .addOnSuccessListener( aVoid1 -> {
                                                            binding.pageFragmentProgressBar.setVisibility(View.INVISIBLE);
                                                            Toast.makeText(context, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();

                                                            callback.finishDiaryActivity();
                                                        });
                                            });

                                    for(PageModel pageModel : diaryModel.getPages().values()){
                                        pageToDeleteCreateTime = pageModel.getCreateTime();
                                        strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(String.valueOf(pageToDeleteCreateTime)).delete();
                                    }
                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                }else if(netStat == TYPE_CONNECTED){
                    callback.dataChanges();
                    setViewWhenLoading();
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
                                    if(!String.valueOf(imageUrl).equals("")) {
                                        strInstance.getReference().child(getString(R.string.fstr_diary_images)).child(uid).child(String.valueOf(diaryCreateTime)).child(String.valueOf(pageCreateTime)).delete()
                                                .addOnSuccessListener(aVoid -> {
                                                    deletePage();
                                                });
                                    }else deletePage();

                                }

                                @Override
                                public void onCancelled(@NonNull DatabaseError databaseError) {

                                }
                            });
                }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
                break;
            case R.id.pageFragment_editDiaryButton:
                //diarykey 전달, 커버 이미지, 제목 수정
                startWriteDiaryActivity(isCover);
                break;
            case R.id.pageFragment_writePageButton:
                //diarykey 전달, 하위 pages에 추가하는 액티비티
                Intent intent = new Intent(context, WriteDiaryActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.putExtra(TITLE_KEY, title);
                intent.putExtra(IS_NEW_KEY, true);
                intent.putExtra(DIARY_KEY_KEY, diaryKey);
                callback.getActivity().startActivityForResult(intent, EDIT_DIARY_CODE);
                break;
            case R.id.pageFragment_writerTextView:
                if(!uid.equals(writerUid)) {
                    Intent writerAccountActivityIntent = new Intent(context, WriterAccountActivity.class);
                    writerAccountActivityIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    writerAccountActivityIntent.putExtra(WRITER_UID_KEY, writerUid);
                    startActivity(writerAccountActivityIntent);
                }
                break;
            case R.id.pageFragment_likeButton:
                callback.dataChanges();
                boolean likeOrNot = view.isSelected();
                view.setSelected(!likeOrNot);
                processLike(!likeOrNot);
                break;
            case R.id.pageFragment_imageView:
            case R.id.pageFragment_label:
                if(isMenuOpen)
                    binding.pageFragmentSlideMenu.startAnimation(translateRight);
                break;
        }
    }

    private void deletePage(){
        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_pages)).child(pageKey).removeValue()
                .addOnSuccessListener(aVoid1 -> {
                    binding.pageFragmentProgressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(context, getString(R.string.delete_success), Toast.LENGTH_SHORT).show();

                    callback.finishDiaryActivity();
                });
    }

    private void processLike(boolean like){
        Map<String, Object> map = new HashMap<>();
        map.put(uid, like);
        netStat = NetworkStatus.getConnectivityStatus(context);
        if(netStat == TYPE_CONNECTED)
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey).child(getString(R.string.fdb_like_users)).updateChildren(map);
        else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void startWriteDiaryActivity(boolean isCover){
        Intent intent = new Intent(context, WriteDiaryActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IMAGE_URL_KEY, imageUrl);
        intent.putExtra(IS_COVER_KEY, isCover);
        intent.putExtra(DIARY_KEY_KEY, diaryKey);
        intent.putExtra(TITLE_KEY, title);
        if(!isCover) {
            intent.putExtra(CONTENT_KEY, content);
            intent.putExtra(PAGE_KEY_KEY, pageKey);
            intent.putExtra(PAGE_CREATE_TIME_KEY, pageCreateTime);
        }

        callback.getActivity().startActivityForResult(intent, EDIT_DIARY_CODE);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof PageFragmentCallback)
            callback = (PageFragmentCallback)context;
        this.context = context;
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
        binding.pageFragmentSlideMenu.setVisibility(View.INVISIBLE);
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

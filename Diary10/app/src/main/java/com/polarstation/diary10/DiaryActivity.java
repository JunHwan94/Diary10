package com.polarstation.diary10;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.databinding.ActivityDiaryBinding;
import com.polarstation.diary10.fragment.PageFragment;
import com.polarstation.diary10.fragment.PageFragmentCallback;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.PageModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.polarstation.diary10.fragment.DiariesFragment.SHOW_DIARY_CODE;
import static com.polarstation.diary10.fragment.ListFragment.IMAGE_URL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.DIARY_KEY_KEY;
import static com.polarstation.diary10.fragment.ListFragment.TITLE_KEY;
import static com.polarstation.diary10.fragment.ListFragment.WRITER_UID_KEY;
import static com.polarstation.diary10.fragment.PageFragment.EDIT_DIARY_CODE;

public class DiaryActivity extends AppCompatActivity implements PageFragmentCallback {
    private ActivityDiaryBinding binding;
    private boolean isCover = true;
    private ListPagerAdapter pagerAdapter;
    private ProgressDialog progressDialog;
    private String writerUid;
    private String diaryKey;
    private FirebaseDatabase dbInstance;

    public static final String IS_COVER_KEY = "isCoverKey";
    public static final String PAGE_MODEL_KEY = "pageModelKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_diary);
        dbInstance = FirebaseDatabase.getInstance();
        Intent intent = getIntent();
        progressDialog = new ProgressDialog(this);

//        showProgressDialog(progressDialog, getString(R.string.loading_data));
        pagerAdapter = new ListPagerAdapter(getSupportFragmentManager());
        if(intent != null){
            String title = intent.getStringExtra(TITLE_KEY);
            writerUid = intent.getStringExtra(WRITER_UID_KEY);
            String imageUrl = intent.getStringExtra(IMAGE_URL_KEY);
            diaryKey = intent.getStringExtra(DIARY_KEY_KEY);

            Bundle bundle = new Bundle();
            bundle.putBoolean(IS_COVER_KEY, isCover);
            bundle.putString(TITLE_KEY, title);
            bundle.putString(WRITER_UID_KEY, writerUid);
            bundle.putString(IMAGE_URL_KEY, imageUrl);
            bundle.putString(DIARY_KEY_KEY, diaryKey);
            PageFragment coverPageFragment = new PageFragment();
            coverPageFragment.setArguments(bundle);

            pagerAdapter.addItem(coverPageFragment);

            loadDiary(diaryKey);
        }

        binding.diaryActivityViewPager.setAdapter(pagerAdapter);
    }

    @Override
    public void loadDiary(String key){
        FirebaseDatabase.getInstance().getReference().child(getString(R.string.fdb_diaries)).child(key).child(getString(R.string.fdb_pages)).orderByChild(getString(R.string.fdb_createTime))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        PageFragment pageFragment = null;
                        List<PageModel> pageModelList = new ArrayList<>();
                        Bundle bundle;
                        PageModel pageModel = null;
                        for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            pageModel = snapshot.getValue(PageModel.class);
                            pageModelList.add(pageModel);
                        }
                        Collections.reverse(pageModelList);

                        for(PageModel sortedPageModel : pageModelList){
                            bundle = new Bundle();
                            bundle.putString(WRITER_UID_KEY, writerUid);
                            bundle.putString(DIARY_KEY_KEY, key);
                            bundle.putParcelable(PAGE_MODEL_KEY, sortedPageModel);
                            pageFragment = new PageFragment();
                            pageFragment.setArguments(bundle);
                            pagerAdapter.addItem(pageFragment);
                        }

                        pagerAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        progressDialog.cancel();
    }

    public static class ListPagerAdapter extends FragmentStatePagerAdapter{
        List<Fragment> fragmentList = new ArrayList<>();

        public ListPagerAdapter(FragmentManager fm){
            super(fm);
        }

        public void addItem(Fragment fragment){
            fragmentList.add(fragment);
        }

        void clear(){
            fragmentList.clear();
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }
    }

    @Override
    public void finishDiaryActivity() {
        finish();
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == EDIT_DIARY_CODE && resultCode == Activity.RESULT_OK){
            pagerAdapter = new ListPagerAdapter(getSupportFragmentManager());
            loadDiaryCover(pagerAdapter);
            loadDiary(diaryKey);

            binding.diaryActivityViewPager.setAdapter(pagerAdapter);
        }
    }

    private void loadDiaryCover(ListPagerAdapter pagerAdapter){
        dbInstance.getReference().child(getString(R.string.fdb_diaries)).child(diaryKey)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        DiaryModel diaryModel = dataSnapshot.getValue(DiaryModel.class);
                        String title = diaryModel.getTitle();
                        String imageUrl = diaryModel.getCoverImageUrl();
                        String writerUid = diaryModel.getUid();
                        String diaryKey = diaryModel.getKey();

                        Bundle bundle = new Bundle();
                        bundle.putBoolean(IS_COVER_KEY, isCover);
                        bundle.putString(TITLE_KEY, title);
                        bundle.putString(WRITER_UID_KEY, writerUid);
                        bundle.putString(IMAGE_URL_KEY, imageUrl);
                        bundle.putString(DIARY_KEY_KEY, diaryKey);

                        PageFragment coverPageFragment = new PageFragment();
                        coverPageFragment.setArguments(bundle);

                        pagerAdapter.addItem(coverPageFragment);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    @Override
    public void setResultIntent() {
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        setResult(SHOW_DIARY_CODE, intent);
    }
}

package com.polarstation.diary10.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import io.reactivex.Observable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.ActivityDiaryBinding;
import com.polarstation.diary10.fragment.PageFragment;
import com.polarstation.diary10.fragment.PageFragmentCallback;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.PageModel;
import com.polarstation.diary10.util.NetworkStatus;

import java.util.ArrayList;
import java.util.List;

import static com.polarstation.diary10.fragment.DiariesFragment.SHOW_DIARY_CODE;
import static com.polarstation.diary10.fragment.ListFragment.IMAGE_URL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.DIARY_KEY_KEY;
import static com.polarstation.diary10.fragment.ListFragment.TITLE_KEY;
import static com.polarstation.diary10.fragment.ListFragment.WRITER_UID_KEY;
import static com.polarstation.diary10.fragment.PageFragment.EDIT_DIARY_CODE;
import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;

public class DiaryActivity extends AppCompatActivity implements PageFragmentCallback {
    private ActivityDiaryBinding binding;
    private boolean isCover = true;
    private String title;
    private ListPagerAdapter pagerAdapter;
    private String writerUid;
    private String diaryKey;
    private FirebaseDatabase dbInstance;
    private int netStat;
    private boolean isDataChanged = false;

    public static final String IS_COVER_KEY = "isCoverKey";
    public static final String PAGE_MODEL_KEY = "pageModelKey";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_diary);

        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            dbInstance = FirebaseDatabase.getInstance();
            Intent intent = getIntent();

            pagerAdapter = new ListPagerAdapter(getSupportFragmentManager());
            binding.diaryActivityViewPager.setOffscreenPageLimit(2);
            if(intent != null){
                title = intent.getStringExtra(TITLE_KEY);
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
        }else {
            Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setViewWhenLoading(){
        binding.diaryActivityProgressBar.setVisibility(View.VISIBLE);
        binding.diaryActivityViewPager.setVisibility(View.INVISIBLE);
    }

    private void setViewWHenLoaded(){
        binding.diaryActivityProgressBar.setVisibility(View.INVISIBLE);
        binding.diaryActivityViewPager.setVisibility(View.VISIBLE);
    }

    private void loadDiaryCover(ListPagerAdapter pagerAdapter){
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            setViewWhenLoading();
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

                            setViewWHenLoaded();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void loadDiary(String key){
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            setViewWhenLoading();
            FirebaseDatabase.getInstance().getReference().child(getString(R.string.fdb_diaries)).child(key).child(getString(R.string.fdb_pages)).orderByChild(getString(R.string.fdb_createTime))
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            List<PageModel> pageModelList = new ArrayList<>();

                            // RxJava
                            Observable.fromIterable(dataSnapshot.getChildren()).subscribe(snapshot -> {
                                PageModel pageModel = snapshot.getValue(PageModel.class);
                                pageModelList.add(pageModel);
                            });

                            Observable.fromIterable(pageModelList).subscribe(pageModel -> {
                                Bundle bundle = new Bundle();
                                bundle.putString(TITLE_KEY, title);
                                bundle.putString(WRITER_UID_KEY, writerUid);
                                bundle.putString(DIARY_KEY_KEY, key);
                                bundle.putParcelable(PAGE_MODEL_KEY, pageModel);
                                PageFragment pageFragment = new PageFragment();
                                pageFragment.setArguments(bundle);
                                pagerAdapter.addItem(pageFragment);
                            });

                            pagerAdapter.notifyDataSetChanged();

                            setViewWHenLoaded();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void finishDiaryActivity() {
        onBackPressed();
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void dataChanges(){
        isDataChanged = !isDataChanged;
    }

    @Override
    public void onBackPressed() {
        if(isDataChanged){
            Intent intent = new Intent();
            setResult(SHOW_DIARY_CODE, intent);
            setResult(RESULT_OK, intent);
        }
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == EDIT_DIARY_CODE && resultCode == Activity.RESULT_OK){
            dataChanges();
            pagerAdapter = new ListPagerAdapter(getSupportFragmentManager());
            loadDiaryCover(pagerAdapter);
            loadDiary(diaryKey);

            binding.diaryActivityViewPager.setAdapter(pagerAdapter);
        }
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
}

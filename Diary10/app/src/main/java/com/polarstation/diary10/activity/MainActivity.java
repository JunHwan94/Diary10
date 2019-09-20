package com.polarstation.diary10.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.ActivityMainBinding;
import com.polarstation.diary10.fragment.AccountFragment;
import com.polarstation.diary10.fragment.MainFragmentCallBack;
import com.polarstation.diary10.fragment.ListFragment;
import com.polarstation.diary10.fragment.WriteFragment;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.util.NetworkStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;

import static com.polarstation.diary10.fragment.WriteFragment.ACCOUNT_TYPE;
import static com.polarstation.diary10.fragment.WriteFragment.CREATE_TYPE;
import static com.polarstation.diary10.fragment.WriteFragment.LIST_TYPE;
import static com.polarstation.diary10.fragment.WriteFragment.WRITE_TYPE;
import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;

public class MainActivity extends BaseActivity implements MainFragmentCallBack {
    private ActivityMainBinding binding;
    private String uid;
    private int type;
    private Bundle bundle;
    private ListFragment listFragment;
    private WriteFragment createDiaryFragment;
    private WriteFragment writeFragment;
    private AccountFragment accountFragment;
    private WriteFragment createOrWriteFragment;
    private FirebaseDatabase dbInstance;
    private int netStat;

    public static final int NEW_DIARY_TYPE = 0;
    public static final int NEW_PAGE_TYPE = 1;
    public static final String TYPE_KEY = "typeKey";
    public static final String LIST_KEY = "listKey";
    public static final String USER_MODEL_KEY = "userModelKey";
    public static final String PUSH_TOKEN = "pushToken";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        bundle = new Bundle();
        dbInstance = FirebaseDatabase.getInstance();
        setViewWhenLoading();

        listFragment = new ListFragment();
        createDiaryFragment = new WriteFragment();
        writeFragment = new WriteFragment();
        accountFragment = new AccountFragment();
        createOrWriteFragment = new WriteFragment();
        findMyDiary();

        getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_frameLayout, listFragment).commit();
        setNavigationViewListener();

        sendPushToken();
    }

    private void sendPushToken(){
        String token = FirebaseInstanceId.getInstance().getToken();
        Map<String, Object> map = new HashMap<>();
        map.put(PUSH_TOKEN, token);

        dbInstance.getReference().child(getString(R.string.fdb_users)).child(uid).updateChildren(map);
    }

    private void setNavigationViewListener(){
        binding.mainActivityBottomNavigationView.setOnNavigationItemSelectedListener(menuItem -> {
            switch(menuItem.getItemId()){
                case R.id.action_list:
                    menuItem.setChecked(true);
                    getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_frameLayout, listFragment).commit();
                    return true;
                case R.id.action_write:
                    menuItem.setChecked(true);
                    getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_frameLayout, createOrWriteFragment).commit();
                    return true;
                case R.id.action_account:
                    menuItem.setChecked(true);
                    getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_frameLayout, accountFragment).commit();
                    break;
            }
            return false;
        });
    }

    @Override
    public void findMyDiary(){
        netStat = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(netStat == TYPE_CONNECTED) {
            FirebaseDatabase.getInstance().getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid)
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            // 일기장이 있을 때
                            if (0 < dataSnapshot.getChildrenCount()) {
                                type = NEW_PAGE_TYPE;
                                bundle.putInt(TYPE_KEY, type);
                                ArrayList<String> diaryTitleList = new ArrayList<>();
                                for (DataSnapshot item : dataSnapshot.getChildren()) {
                                    DiaryModel diaryModel = item.getValue(DiaryModel.class);
                                    String title = diaryModel.getTitle();
                                    diaryTitleList.add(title);
                                }
                                Collections.sort(diaryTitleList);
                                bundle.putStringArrayList(LIST_KEY, diaryTitleList);
                                writeFragment.setArguments(bundle);
                                createOrWriteFragment = writeFragment;
                            }
                            // 일기장 없을 때
                            else {
                                type = NEW_DIARY_TYPE;
                                bundle.putInt(TYPE_KEY, type);
                                createDiaryFragment.setArguments(bundle);
                                createOrWriteFragment = createDiaryFragment;
                            }
                            setViewWhenDone();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }else Toast.makeText(getBaseContext(), getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void replaceFragment(int type) {
        setNavigationViewListener();
        switch(type){
            case LIST_TYPE:
                binding.mainActivityBottomNavigationView.setSelectedItemId(R.id.action_list);
                getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_frameLayout, listFragment).commit();
                break;
            case CREATE_TYPE:
                getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_frameLayout, createDiaryFragment).commit();
                break;
            case WRITE_TYPE:
                writeFragment = new WriteFragment();
                getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_frameLayout, writeFragment).commit();
                setViewWhenDone();
                break;
            case ACCOUNT_TYPE:
                Toast.makeText(getBaseContext(), getString(R.string.uploaded), Toast.LENGTH_LONG).show();
                binding.mainActivityBottomNavigationView.setSelectedItemId(R.id.action_account);
                getSupportFragmentManager().beginTransaction().replace(R.id.mainActivity_frameLayout, accountFragment).commit();
                setViewWhenDone();
                break;
        }
    }

    @Override
    public void quitApp() {
        finishAffinity();
        System.runFinalization();
        System.exit(0);
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    public void setViewWhenLoading() {
        binding.mainActivityProgressBar.setVisibility(View.VISIBLE);
        binding.mainActivityBottomNavigationView.setEnabled(false);
    }

    public void setViewWhenDone() {
        binding.mainActivityProgressBar.setVisibility(View.INVISIBLE);
        binding.mainActivityBottomNavigationView.setEnabled(true);
    }

    @Override
    public void setNavigationViewDisabled() {
        binding.mainActivityBottomNavigationView.setOnNavigationItemSelectedListener(null);
    }
}

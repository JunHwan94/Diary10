package com.polarstation.diary10.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import gun0912.tedkeyboardobserver.TedKeyboardObserver;
import io.reactivex.Observable;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.activity.BaseActivity;
import com.polarstation.diary10.activity.DiaryActivity;
import com.polarstation.diary10.data.DiaryRecyclerViewAdapter;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.FragmentListBinding;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.util.NetworkStatus;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static com.polarstation.diary10.util.NetworkStatus.TYPE_CONNECTED;

public class ListFragment extends Fragment implements View.OnClickListener{
    private FragmentListBinding binding;
    private DiaryRecyclerViewAdapter adapter;
    private List<DiaryModel> diaryModelList;
    private MainFragmentCallBack callback;
    private FirebaseDatabase dbInstance;
    private String uid;
    private int netStat;
    private Context context;

    public static final String TITLE_KEY = "titleKey";
    public static final String WRITER_UID_KEY = "writerKey";
    public static final String IMAGE_URL_KEY = "imageUrlKey";
    public static final String DIARY_KEY_KEY = "diaryKeyKey";

    @Nullable
    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_list, container, false);
        BaseActivity.setGlobalFont(binding.getRoot());

        netStat = NetworkStatus.getConnectivityStatus(context);
        if(netStat == TYPE_CONNECTED) {
            dbInstance = FirebaseDatabase.getInstance();
            uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

            adapter = new DiaryRecyclerViewAdapter();
            adapter.setOnItemClickListener((holder, view, position) -> {
                DiaryModel diaryModel = adapter.getItem(position);
                Intent intent = new Intent(context, DiaryActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // key 넘겨주기
                intent.putExtra(DIARY_KEY_KEY, diaryModel.getKey());
                intent.putExtra(TITLE_KEY, diaryModel.getTitle());
                intent.putExtra(WRITER_UID_KEY, diaryModel.getUid());
                intent.putExtra(IMAGE_URL_KEY, diaryModel.getCoverImageUrl());
                startActivity(intent);
            });

            diaryModelList = new ArrayList<>();
            GridLayoutManager layoutManager = new GridLayoutManager(context, 3);
            binding.listFragmentRecyclerView.setLayoutManager(layoutManager);
            binding.listFragmentRecyclerView.setAdapter(adapter);
            loadDiaries();

            binding.listFragmentRefreshButton.setOnClickListener(this);
            binding.listFragmentSearchButton.setOnClickListener(this);

            new TedKeyboardObserver(getActivity()).listen(isShow -> {
                if (!isShow) {
                    binding.listFragmentSearchEditText.clearFocus();
                }
            });
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();

        return binding.getRoot();
    }

    private void searchDiaries(String searchWord){
        netStat = NetworkStatus.getConnectivityStatus(context);
        if(netStat == TYPE_CONNECTED) {
            binding.listFragmentProgressBar.setVisibility(View.VISIBLE);
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_private)).equalTo(false)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            List<DiaryModel> diaryModelList = new ArrayList<>();

                            //RxJava
                            Observable.fromIterable(dataSnapshot.getChildren()).filter(snapshot -> {
                                DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
                                if(diaryModel.getTitle().contains(searchWord) && !diaryModel.getUid().equals(uid))
                                    return true;
                                else return false;
                            }).subscribe(snapshot -> {
                                DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
                                diaryModelList.add(diaryModel);
                            });

                            if (diaryModelList.size() == 0)
                                Toast.makeText(context, getString(R.string.no_result), Toast.LENGTH_SHORT).show();
                            adapter.addAll(diaryModelList);
                            adapter.notifyDataSetChanged();

                            binding.listFragmentProgressBar.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                        }
                    });
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    private void loadDiaries(){
        netStat = NetworkStatus.getConnectivityStatus(context);
        if(netStat == TYPE_CONNECTED) {
            binding.listFragmentProgressBar.setVisibility(View.VISIBLE);
            dbInstance.getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_private)).equalTo(false)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            diaryModelList.clear();
                            //RxJava
                            Observable.fromIterable(dataSnapshot.getChildren()).filter(snapshot ->
                                    !snapshot.getValue(DiaryModel.class).getUid().equals(uid)
                            ).subscribe(snapshot -> {
                                DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
                                diaryModelList.add(diaryModel);
                            });

                            adapter.addAll(diaryModelList);
                            adapter.notifyDataSetChanged();

                            binding.listFragmentProgressBar.setVisibility(View.INVISIBLE);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
        }else Toast.makeText(context, getString(R.string.network_not_connected), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.listFragment_searchButton:
                String searchWord = String.valueOf(binding.listFragmentSearchEditText.getText());
                searchDiaries(searchWord);
                binding.listFragmentSearchEditText.clearFocus();
                break;
            case R.id.listFragment_refreshButton:
                loadDiaries();
                binding.listFragmentSearchEditText.setText("");
                binding.listFragmentSearchEditText.clearFocus();
                break;
        }
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
        if(callback != null)
            callback = null;
    }
}

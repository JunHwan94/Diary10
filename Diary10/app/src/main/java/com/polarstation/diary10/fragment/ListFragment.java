package com.polarstation.diary10.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import gun0912.tedkeyboardobserver.TedKeyboardObserver;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.DiaryActivity;
import com.polarstation.diary10.DiaryRecyclerViewAdapter;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.FragmentListBinding;
import com.polarstation.diary10.model.DiaryModel;

import java.util.ArrayList;
import java.util.List;

public class ListFragment extends Fragment implements View.OnClickListener{
    private FragmentListBinding binding;
    private DiaryRecyclerViewAdapter adapter;
    private ProgressDialog progressDialog;
    private List<DiaryModel> diaryModelList;
    private MainFragmentCallBack callback;
    private FirebaseDatabase dbInstance;
    private String uid;

    public static final String TITLE_KEY = "titleKey";
    public static final String WRITER_UID_KEY = "writerKey";
    public static final String IMAGE_URL_KEY = "imageUrlKey";
    public static final String DIARY_KEY_KEY = "diaryKeyKey";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_list, container, false);
        dbInstance = FirebaseDatabase.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        progressDialog = new ProgressDialog(getContext());

        // progressBar 쓰기

        adapter = new DiaryRecyclerViewAdapter();
        adapter.setOnItemClickListener((holder, view, position) -> {
            DiaryModel diaryModel = adapter.getItem(position);
            Intent intent = new Intent(getContext(), DiaryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // key 넘겨주기
            intent.putExtra(DIARY_KEY_KEY, diaryModel.getKey());
            intent.putExtra(TITLE_KEY, diaryModel.getTitle());
            intent.putExtra(WRITER_UID_KEY, diaryModel.getUid());
            intent.putExtra(IMAGE_URL_KEY, diaryModel.getCoverImageUrl());
            startActivity(intent);
        });

        diaryModelList = new ArrayList<>();
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        binding.listFragmentRecyclerView.setLayoutManager(layoutManager);
        binding.listFragmentRecyclerView.setAdapter(adapter);
        loadDiaries();

        binding.listFragmentRefreshButton.setOnClickListener(this);
        binding.listFragmentSearchButton.setOnClickListener(this);

        new TedKeyboardObserver(getActivity()).listen(isShow-> {
            if(!isShow){
                binding.listFragmentSearchEditText.clearFocus();
            }
        });

        return binding.getRoot();
    }

    private void searchDiaries(String searchWord){
        dbInstance.getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_private)).equalTo(false)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        List<DiaryModel> diaryModelList = new ArrayList<>();
                        for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
                            String title = diaryModel.getTitle();
                            String writerUid = diaryModel.getUid();
                            if(title.contains(searchWord) && !writerUid.equals(uid))
                                diaryModelList.add(diaryModel);
                        }
                        if(diaryModelList.size() == 0)
                            Toast.makeText(getContext(), getString(R.string.no_result),Toast.LENGTH_SHORT).show();
                        adapter.addAll(diaryModelList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void loadDiaries(){
        dbInstance.getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_private)).equalTo(false)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                        diaryModelList.clear();
                        for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                            DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
                            if(!diaryModel.getUid().equals(uid))
                                diaryModelList.add(diaryModel);
                        }
                        adapter.addAll(diaryModelList);
                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(callback != null)
            callback = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        progressDialog.cancel();
    }
}

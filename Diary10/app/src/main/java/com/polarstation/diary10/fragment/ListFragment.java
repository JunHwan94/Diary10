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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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

import static com.polarstation.diary10.util.DialogUtils.showProgressDialog;

public class ListFragment extends Fragment {
    private FragmentListBinding binding;
    private DiaryRecyclerViewAdapter adapter;
    private ProgressDialog progressDialog;
    private List<DiaryModel> diaryModelList;
    private FragmentCallBack callback;

    public static final String TITLE_KEY = "titleKey";
    public static final String WRITER_UID_KEY = "writerKey";
    public static final String IMAGE_URL_KEY = "imageUrlKey";
    public static final String KEY_KEY = "keyKey";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_list, container, false);

        progressDialog = new ProgressDialog(getContext());
        //
        callback.showProgressDialog(getString(R.string.loading_data));

        adapter = new DiaryRecyclerViewAdapter();
        adapter.setOnItemClickListener((holder, view, position) -> {
            DiaryModel diaryModel = adapter.getItem(position);
            Intent intent = new Intent(getContext(), DiaryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);

            // key 넘겨주기
            intent.putExtra(KEY_KEY, diaryModel.getKey());
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

        callback.cancelDialog();

        return binding.getRoot();
    }

    private void loadDiaries(){
        FirebaseDatabase.getInstance().getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_private)).equalTo(false)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof FragmentCallBack)
            callback = (FragmentCallBack) context;
//        loadDiaries();
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

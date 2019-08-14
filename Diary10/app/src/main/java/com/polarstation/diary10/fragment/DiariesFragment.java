package com.polarstation.diary10.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
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
import com.polarstation.diary10.databinding.FragmentMyDiariesBinding;
import com.polarstation.diary10.model.DiaryModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import static com.polarstation.diary10.fragment.AccountFragment.FRAGMENT_TYPE_KEY;
import static com.polarstation.diary10.fragment.AccountFragment.LIKED_DIARY;
import static com.polarstation.diary10.fragment.AccountFragment.MY_DIARY;
import static com.polarstation.diary10.fragment.ListFragment.IMAGE_URL_KEY;
import static com.polarstation.diary10.fragment.ListFragment.DIARY_KEY_KEY;
import static com.polarstation.diary10.fragment.ListFragment.TITLE_KEY;
import static com.polarstation.diary10.fragment.ListFragment.WRITER_UID_KEY;

public class DiariesFragment extends Fragment {
    private FragmentMyDiariesBinding binding;
    private DiaryRecyclerViewAdapter adapter;
    private List<DiaryModel> diaryModelList;
    private String uid;
    private MainFragmentCallBack callback;
    private String type;

    public static final int SHOW_DIARY_CODE = 100;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_my_diaries, container, false);

        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Bundle bundle = getArguments();
        if(bundle != null){
            type = bundle.getString(FRAGMENT_TYPE_KEY);
            switch (type){
                case MY_DIARY:
                    loadMyDiaries();
                    break;
                case LIKED_DIARY:
                    loadLikedDiaries();
                    break;
            }
        }

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        binding.myDiariesFragmentRecyclerView.setLayoutManager(layoutManager);
        adapter = new DiaryRecyclerViewAdapter();
        adapter.setOnItemClickListener((holder, view, position) -> {
            DiaryModel diaryModel = adapter.getItem(position);
            Intent intent = new Intent(getContext(), DiaryActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra(TITLE_KEY, diaryModel.getTitle());
            intent.putExtra(WRITER_UID_KEY, diaryModel.getUid());
            intent.putExtra(IMAGE_URL_KEY, diaryModel.getCoverImageUrl());
            intent.putExtra(DIARY_KEY_KEY, diaryModel.getKey());
            callback.getActivity().startActivityForResult(intent, SHOW_DIARY_CODE);
        });
        binding.myDiariesFragmentRecyclerView.setAdapter(adapter);

        return binding.getRoot();
    }

    private void loadMyDiaries(){
        diaryModelList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.fdb_uid)).equalTo(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        diaryModelList.clear();
                        for(DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
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

    private void loadLikedDiaries(){
        diaryModelList = new ArrayList<>();
        FirebaseDatabase.getInstance().getReference().child(getString(R.string.fdb_diaries)).orderByChild(getString(R.string.like_users))
        .addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                    DiaryModel diaryModel = snapshot.getValue(DiaryModel.class);
                    Map<String, Boolean> map = diaryModel.getLikeUsers();
                    if(map.keySet().contains(uid) && map.get(uid)){
                        diaryModelList.add(diaryModel);
                    }
                    adapter.addAll(diaryModelList);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof MainFragmentCallBack)
            callback = (MainFragmentCallBack)context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(callback != null)
            callback = null;
    }
}
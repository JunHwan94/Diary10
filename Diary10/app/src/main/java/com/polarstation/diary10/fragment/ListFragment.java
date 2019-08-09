package com.polarstation.diary10.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.polarstation.diary10.BaseActivity;
import com.polarstation.diary10.DiaryRecyclerViewAdapter;
import com.polarstation.diary10.R;
import com.polarstation.diary10.databinding.FragmentListBinding;
import com.polarstation.diary10.model.DiaryModel;
import com.polarstation.diary10.model.UserModel;

import java.util.ArrayList;
import java.util.List;

import static com.polarstation.diary10.util.DialogUtils.showProgressDialog;

public class ListFragment extends Fragment {
    private FragmentListBinding binding;
    private DiaryRecyclerViewAdapter adapter;
    private ProgressDialog progressDialog;
    private List<DiaryModel> diaryModelList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_list, container, false);

        adapter = new DiaryRecyclerViewAdapter();
        diaryModelList = new ArrayList<>();
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        binding.listFragmentRecyclerView.setLayoutManager(layoutManager);
        binding.listFragmentRecyclerView.setAdapter(adapter);
//        loadDiaries();

        return binding.getRoot();
    }

    private void loadDiaries(){
        FirebaseDatabase.getInstance().getReference().child(getString(R.string.fdb_diaries))
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        diaryModelList.clear();
                        for(DataSnapshot snapshot : dataSnapshot.getChildren()){
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

        progressDialog.cancel();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        progressDialog = new ProgressDialog(getContext());
        showProgressDialog(progressDialog, getString(R.string.loading_data));
        loadDiaries();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }
}

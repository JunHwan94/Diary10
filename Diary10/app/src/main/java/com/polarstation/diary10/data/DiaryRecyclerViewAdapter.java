package com.polarstation.diary10.data;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.polarstation.diary10.R;
import com.polarstation.diary10.activity.BaseActivity;
import com.polarstation.diary10.model.DiaryModel;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class DiaryRecyclerViewAdapter extends RecyclerView.Adapter<DiaryRecyclerViewAdapter.DiaryViewHolder>{
    private List<DiaryModel> diaryModelList = new ArrayList<>();

    private OnItemClickListener listener;

    public static interface OnItemClickListener{
        public void onItemClick(DiaryViewHolder holder, View view, int position);
    }

    public DiaryRecyclerViewAdapter() {
    }

    @NonNull
    @Override
    public DiaryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_diary, viewGroup, false);
        return new DiaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DiaryRecyclerViewAdapter.DiaryViewHolder holder, int position) {
        DiaryModel diaryModel = diaryModelList.get(position);
        holder.setItem(diaryModel);
        holder.setOnItemClickListener(listener);
    }

    @Override
    public int getItemCount() {
        return diaryModelList.size();
    }

    public void addItem(DiaryModel diaryModel){
        diaryModelList.add(diaryModel);
    }

    public void addAll(List<DiaryModel> diaryModelList){
        this.diaryModelList = diaryModelList;
    }

    public DiaryModel getItem(int position){
        return diaryModelList.get(position);
    }

    public void setOnItemClickListener(OnItemClickListener listener){
        this.listener = listener;
    }

    public class DiaryViewHolder extends RecyclerView.ViewHolder{
        private ImageView coverImageView;
        private TextView titleTextView;
        private OnItemClickListener listener;

        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            BaseActivity.setGlobalFont(itemView);
            coverImageView = itemView.findViewById(R.id.diaryItem_coverImageView);
            titleTextView = itemView.findViewById(R.id.diaryItem_titleTextView);

            itemView.setOnClickListener( v->{
                int position = getAdapterPosition();

                if(listener != null)
                    listener.onItemClick(this, itemView, position);
            });
        }

        public void setItem(DiaryModel diaryModel){
            Glide.with(itemView.getContext())
                    .load(diaryModel.getCoverImageUrl())
                    .apply(new RequestOptions().centerCrop().override(250,300))
                    .into(coverImageView);
            titleTextView.setText(diaryModel.getTitle());
        }

        public void setOnItemClickListener(OnItemClickListener listener){
            this.listener = listener;
        }
    }
}

package com.polarstation.diary10.data;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.polarstation.diary10.R;
import com.polarstation.diary10.activity.BaseActivity;
import com.polarstation.diary10.databinding.ItemDiaryBinding;
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
        private OnItemClickListener listener;
        private ItemDiaryBinding binding;

        public DiaryViewHolder(@NonNull View itemView) {
            super(itemView);
            binding = ItemDiaryBinding.bind(itemView);
            BaseActivity.setGlobalFont(itemView);

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
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            Animation fadeIn = AnimationUtils.loadAnimation(itemView.getContext(), R.anim.fade_in);
                            binding.diaryItemTitleTextView.setVisibility(View.VISIBLE);
                            itemView.startAnimation(fadeIn);
                            return false;
                        }
                    })
                    .into(binding.diaryItemCoverImageView);
            binding.diaryItemTitleTextView.setText(diaryModel.getTitle());
        }

        public void setOnItemClickListener(OnItemClickListener listener){
            this.listener = listener;
        }
    }
}

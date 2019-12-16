package com.polarstation.diary10.data

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.polarstation.diary10.R
import com.polarstation.diary10.util.FontUtil
import com.polarstation.diary10.databinding.ItemDiaryBinding
import com.polarstation.diary10.model.DiaryModel

class DiaryRecyclerViewAdapter: RecyclerView.Adapter<DiaryRecyclerViewAdapter.DiaryViewHolder>() {
    private val diaryModelList = ArrayList<DiaryModel>()
    private lateinit var listener: OnItemClickListener

    interface OnItemClickListener{
        fun onItemClick(holder: DiaryViewHolder, view: View, position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiaryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diary, parent, false)
        return DiaryViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiaryViewHolder, position: Int) {
        val diaryModel = diaryModelList[position]
        holder.setItem(diaryModel)
        holder.setOnItemClickListener(listener)
    }

    override fun getItemCount(): Int = diaryModelList.size
    fun addItem(diaryModel: DiaryModel) = diaryModelList.add(diaryModel)
    fun clear() = diaryModelList.clear()
    fun getItem(position: Int) = diaryModelList[position]
    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    class DiaryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private lateinit var listener: OnItemClickListener
        private var binding: ItemDiaryBinding = ItemDiaryBinding.bind(itemView)

        init {
            FontUtil.setGlobalFont(binding.root)
            itemView.setOnClickListener {
                val position = adapterPosition
                if(listener != null) listener.onItemClick(this, itemView, position)
            }
        }

        fun setItem(diaryModel: DiaryModel){
            Glide.with(itemView.context)
                    .load(diaryModel.coverImageUrl)
                    .apply(RequestOptions().centerCrop().override(250, 300))
                    .listener(object : RequestListener<Drawable>{
                        override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                            val fadeIn = AnimationUtils.loadAnimation(itemView.context, R.anim.fade_in)
                            binding.diaryItemParentCardView.visibility = View.VISIBLE
                            itemView.startAnimation(fadeIn)
                            return false
                        }

                        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                            return false
                        }
                    })
                    .into(binding.diaryItemCoverImageView)
            binding.diaryItemTitleTextView.text = diaryModel.title
        }

        fun setOnItemClickListener(listener: OnItemClickListener){
            this.listener = listener
        }
    }
}
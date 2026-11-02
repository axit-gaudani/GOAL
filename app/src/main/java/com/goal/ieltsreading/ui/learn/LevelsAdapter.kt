package com.goal.ieltsreading.ui.learn

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.repo.LevelItem
import com.goal.ieltsreading.databinding.ItemLevelBinding
import com.goal.ieltsreading.ui.common.visibleIf

class LevelsAdapter(private val onClick: (LevelItem) -> Unit) :
    ListAdapter<LevelItem, LevelsAdapter.Holder>(Diff) {

    class Holder(val binding: ItemLevelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemLevelBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val b = holder.binding
        val ctx = b.root.context
        b.textLevelNumber.text = item.level.sortOrder.toString()
        b.textLevelTitle.text = item.level.title
        b.textLevelTitleGu.text = item.level.titleGu
        b.textLevelBand.text = item.level.band
        b.textLevelDescription.text = item.level.descriptionGu.ifBlank { item.level.description }
        b.progressLevel.setProgressCompat(item.percent, false)
        b.textLevelProgress.text = ctx.getString(R.string.lessons_done_format, item.completedLessons, item.totalLessons)
        b.iconLock.visibleIf(!item.unlocked)
        b.iconDone.visibleIf(item.finished)
        b.root.alpha = if (item.unlocked) 1f else 0.55f
        b.root.setOnClickListener { onClick(item) }
    }

    object Diff : DiffUtil.ItemCallback<LevelItem>() {
        override fun areItemsTheSame(a: LevelItem, b: LevelItem) = a.level.id == b.level.id
        override fun areContentsTheSame(a: LevelItem, b: LevelItem) = a == b
    }
}

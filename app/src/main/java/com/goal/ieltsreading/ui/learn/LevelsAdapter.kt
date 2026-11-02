package com.goal.ieltsreading.ui.learn

import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
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
        // Level −1 shows "ABC" in its circle.
        b.textLevelNumber.text = if (item.level.sortOrder < 0) ctx.getString(R.string.abc_short) else item.level.sortOrder.toString()
        b.textLevelTitle.text = item.level.title
        b.textLevelTitleGu.text = item.level.titleGu
        b.textLevelBand.text = item.level.band
        b.textLevelDescription.text = item.level.descriptionGu.ifBlank { item.level.description }
        b.progressLevel.setProgressCompat(item.percent, false)
        b.textLevelProgress.text = ctx.getString(R.string.lessons_done_format, item.completedLessons, item.totalLessons)
        b.iconLock.visibleIf(!item.unlocked)
        b.iconDone.visibleIf(item.finished)

        // Timeline state: done = green, current = blue with border, locked = grey.
        val current = item.unlocked && !item.finished
        val density = ctx.resources.displayMetrics.density
        b.textLevelNumber.setBackgroundResource(
            when {
                item.finished -> R.drawable.bg_circle_success
                current -> R.drawable.bg_circle_primary
                else -> R.drawable.bg_circle_track
            }
        )
        b.textLevelNumber.setTextColor(ContextCompat.getColor(ctx, if (item.unlocked) R.color.white else R.color.text_secondary))
        b.lineDown.setBackgroundColor(ContextCompat.getColor(ctx, if (item.finished) R.color.success else R.color.track))
        b.lineDown.visibility = if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE
        b.card.strokeWidth = if (current) (2 * density).toInt() else 0
        b.card.setCardBackgroundColor(ContextCompat.getColor(ctx, if (item.unlocked) R.color.surface else R.color.surface_variant))
        b.groupProgress.visibleIf(current || item.finished)
        b.textLevelDescription.visibleIf(current)
        b.card.alpha = if (item.unlocked) 1f else 0.75f
        b.card.setOnClickListener { onClick(item) }
    }

    object Diff : DiffUtil.ItemCallback<LevelItem>() {
        override fun areItemsTheSame(a: LevelItem, b: LevelItem) = a.level.id == b.level.id
        override fun areContentsTheSame(a: LevelItem, b: LevelItem) = a == b
    }
}

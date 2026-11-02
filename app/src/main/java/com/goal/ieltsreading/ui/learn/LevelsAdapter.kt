package com.goal.ieltsreading.ui.learn

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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
        fun color(res: Int) = ContextCompat.getColor(ctx, res)

        // Level −1 (ABC) shows "−1" in its dot, as in the design.
        b.textLevelNumber.text = item.level.sortOrder.toString().replace('-', '−')
        b.textLevelTitle.text = item.level.title
        b.textLevelTitleGu.text = item.level.titleGu
        b.textLevelBand.text = item.level.band
        b.textLevelDescription.text = item.level.descriptionGu.ifBlank { item.level.description }
        b.progressLevel.setProgressCompat(item.percent, false)
        b.textLevelProgress.text = ctx.getString(R.string.g2_done_of_total, item.completedLessons, item.totalLessons)
        // The dot shows the state, so the small icons stay hidden (as in the design).
        b.iconLock.visibility = View.GONE
        b.iconDone.visibility = View.GONE
        b.textLevelDescription.visibility = View.GONE

        // Timeline state: done = green dot, current = blue dot with soft-blue halo, locked = grey dot.
        val current = item.unlocked && !item.finished
        val density = ctx.resources.displayMetrics.density
        b.textLevelNumber.setBackgroundResource(
            when {
                item.finished -> R.drawable.bg_g2_dot_done
                current -> R.drawable.bg_circle_primary
                else -> R.drawable.bg_circle_track
            }
        )
        b.dotHalo.background = if (current) ContextCompat.getDrawable(ctx, R.drawable.bg_g2_halo) else null
        b.textLevelNumber.setTextColor(color(if (item.unlocked) R.color.white else R.color.text_secondary))

        // Line above the dot takes the colour of the previous level; line below takes this level's colour.
        val previousDone = position > 0 && getItem(position - 1).finished
        b.lineUp.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        b.lineUp.setBackgroundColor(color(if (previousDone) R.color.g2_correct else R.color.track))
        b.lineDown.visibility = if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE
        b.lineDown.setBackgroundColor(color(if (item.finished) R.color.g2_correct else R.color.track))

        // Card: current = white with 2dp blue border, done = white with thin grey border, locked = grey, faded.
        b.card.setCardBackgroundColor(color(if (item.unlocked) R.color.surface else R.color.surface_variant))
        b.card.strokeColor = color(
            when {
                current -> R.color.brand_primary
                item.unlocked -> R.color.track
                else -> R.color.surface_variant
            }
        )
        b.card.strokeWidth = ((if (current) 2 else 1) * density).toInt()
        b.card.alpha = if (item.unlocked) 1f else 0.8f

        // Band chip: soft blue, or white on locked levels.
        b.textLevelBand.setBackgroundResource(if (item.unlocked) R.drawable.bg_pill else R.drawable.bg_g2_chip_white)
        b.textLevelBand.setTextColor(color(if (item.unlocked) R.color.brand_on_primary_container else R.color.text_secondary))

        // Progress bar only on the current level.
        b.groupProgress.visibleIf(current)

        val state = ctx.getString(
            when {
                item.finished -> R.string.g2_status_finished
                current -> R.string.g2_status_current
                else -> R.string.status_locked
            }
        )
        b.card.contentDescription = ctx.getString(R.string.g2_level_cd_format, item.level.title, item.level.band, state)
        b.card.setOnClickListener { onClick(item) }
    }

    object Diff : DiffUtil.ItemCallback<LevelItem>() {
        override fun areItemsTheSame(a: LevelItem, b: LevelItem) = a.level.id == b.level.id
        override fun areContentsTheSame(a: LevelItem, b: LevelItem) = a == b
    }
}

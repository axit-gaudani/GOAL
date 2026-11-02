package com.goal.ieltsreading.ui.learn

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.repo.LessonItem
import com.goal.ieltsreading.databinding.ItemLessonBinding

class LessonsAdapter(private val onClick: (LessonItem) -> Unit) :
    ListAdapter<LessonItem, LessonsAdapter.Holder>(Diff) {

    class Holder(val binding: ItemLessonBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemLessonBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val b = holder.binding
        val ctx = b.root.context
        b.textLessonNumber.text = item.lesson.sortOrder.toString()
        b.textLessonTitle.text = item.lesson.title
        b.textLessonTitleGu.text = item.lesson.titleGu
        b.textLessonMeta.text = ctx.getString(R.string.lesson_meta_format, item.lesson.skill, item.lesson.minutes)
        val status = when {
            item.completed -> ctx.getString(R.string.status_completed, item.progress?.bestScore ?: 0)
            !item.unlocked -> ctx.getString(R.string.status_locked)
            (item.progress?.attempts ?: 0) > 0 -> ctx.getString(R.string.status_try_again, item.progress?.bestScore ?: 0)
            else -> ctx.getString(R.string.status_ready)
        }
        b.textLessonStatus.text = status
        fun color(res: Int) = ContextCompat.getColor(ctx, res)
        val current = item.unlocked && !item.completed
        val tried = (item.progress?.attempts ?: 0) > 0

        // Same states as the learning path: done = green, next = blue with border, locked = grey.
        b.textLessonNumber.setBackgroundResource(
            when {
                item.completed -> R.drawable.bg_g2_dot_done
                current -> R.drawable.bg_circle_primary
                else -> R.drawable.bg_circle_track
            }
        )
        b.textLessonNumber.setTextColor(color(if (item.unlocked) R.color.white else R.color.text_secondary))
        b.textLessonStatus.setBackgroundResource(
            when {
                item.completed -> R.drawable.bg_g2_pill_green
                !item.unlocked -> R.drawable.bg_g2_chip_white
                tried -> R.drawable.bg_pill_amber
                else -> R.drawable.bg_pill
            }
        )
        b.textLessonStatus.setTextColor(
            color(
                when {
                    item.completed -> R.color.g2_correct_text
                    !item.unlocked -> R.color.text_secondary
                    tried -> R.color.g2_amber_text
                    else -> R.color.brand_on_primary_container
                }
            )
        )
        b.iconStatus.setImageResource(
            when {
                item.completed -> R.drawable.ic_check_circle
                !item.unlocked -> R.drawable.ic_lock
                else -> R.drawable.ic_g2_play
            }
        )
        ImageViewCompat.setImageTintList(b.iconStatus, ColorStateList.valueOf(
            color(
                when {
                    item.completed -> R.color.g2_correct
                    !item.unlocked -> R.color.text_secondary
                    else -> R.color.brand_primary
                }
            )
        ))
        val density = ctx.resources.displayMetrics.density
        b.root.setCardBackgroundColor(color(if (item.unlocked) R.color.surface else R.color.surface_variant))
        b.root.strokeColor = color(
            when {
                current -> R.color.brand_primary
                item.unlocked -> R.color.track
                else -> R.color.surface_variant
            }
        )
        b.root.strokeWidth = ((if (current) 2 else 1) * density).toInt()
        b.root.alpha = if (item.unlocked) 1f else 0.8f
        b.root.setOnClickListener { onClick(item) }
    }

    object Diff : DiffUtil.ItemCallback<LessonItem>() {
        override fun areItemsTheSame(a: LessonItem, b: LessonItem) = a.lesson.id == b.lesson.id
        override fun areContentsTheSame(a: LessonItem, b: LessonItem) = a == b
    }
}

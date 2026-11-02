package com.goal.ieltsreading.ui.learn

import android.view.LayoutInflater
import android.view.ViewGroup
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
        b.iconStatus.setImageResource(
            when {
                item.completed -> R.drawable.ic_check_circle
                !item.unlocked -> R.drawable.ic_lock
                else -> R.drawable.ic_play
            }
        )
        b.root.alpha = if (item.unlocked) 1f else 0.55f
        b.root.setOnClickListener { onClick(item) }
    }

    object Diff : DiffUtil.ItemCallback<LessonItem>() {
        override fun areItemsTheSame(a: LessonItem, b: LessonItem) = a.lesson.id == b.lesson.id
        override fun areContentsTheSame(a: LessonItem, b: LessonItem) = a == b
    }
}

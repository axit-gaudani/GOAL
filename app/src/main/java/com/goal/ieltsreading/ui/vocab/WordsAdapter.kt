package com.goal.ieltsreading.ui.vocab

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.db.WordWithProgress
import com.goal.ieltsreading.databinding.ItemWordRowBinding
import com.goal.ieltsreading.domain.SpacedRepetition

class WordsAdapter(private val onClick: (WordWithProgress) -> Unit) :
    ListAdapter<WordWithProgress, WordsAdapter.Holder>(Diff) {

    class Holder(val binding: ItemWordRowBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemWordRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val w = getItem(position)
        val b = holder.binding
        val ctx = b.root.context
        b.textWord.text = w.word
        b.textPos.text = w.pos
        b.textMeaning.text = w.meaningGu
        b.textStatus.text = when {
            w.box == null -> ctx.getString(R.string.word_status_new)
            w.box >= SpacedRepetition.MAX_BOX -> ctx.getString(R.string.word_status_mastered)
            else -> ctx.getString(R.string.word_status_box, w.box, SpacedRepetition.MAX_BOX)
        }
        b.root.setOnClickListener { onClick(w) }
    }

    object Diff : DiffUtil.ItemCallback<WordWithProgress>() {
        override fun areItemsTheSame(a: WordWithProgress, b: WordWithProgress) = a.id == b.id
        override fun areContentsTheSame(a: WordWithProgress, b: WordWithProgress) = a == b
    }
}

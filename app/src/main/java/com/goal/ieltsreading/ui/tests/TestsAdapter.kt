package com.goal.ieltsreading.ui.tests

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.goal.ieltsreading.R
import com.goal.ieltsreading.data.repo.TestItem
import com.goal.ieltsreading.databinding.ItemTestBinding
import com.goal.ieltsreading.domain.BandCalculator
import com.goal.ieltsreading.ui.common.visibleIf

class TestsAdapter(private val onClick: (TestItem) -> Unit) : ListAdapter<TestItem, TestsAdapter.Holder>(Diff) {

    class Holder(val binding: ItemTestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(ItemTestBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        val b = holder.binding
        val ctx = b.root.context
        // Badge shows just the band number ("Band 5.5–6" → "5.5–6").
        b.textBand.text = item.test.targetBand.removePrefix("Band").trim()
        b.textBand.textSize = if (b.textBand.text.length > 4) 13f else 17f
        b.textTitle.text = item.test.title
        b.textTitleGu.text = item.test.titleGu
        b.textBest.text = item.bestBand?.let { ctx.getString(R.string.best_band_format, BandCalculator.format(it), item.attempts) }
            ?: ctx.getString(R.string.g3_not_tried_format, item.test.minutes)
        b.textLocked.text = ctx.getString(R.string.test_locked_format, item.requiredLevelTitle)
        b.textLocked.visibleIf(!item.unlocked)
        b.iconLock.visibleIf(!item.unlocked)
        b.iconChevron.visibleIf(item.unlocked)
        b.textBest.visibleIf(item.unlocked)
        // Locked tests: grey card and white badge, as in the design.
        b.root.setCardBackgroundColor(ContextCompat.getColor(ctx, if (item.unlocked) R.color.surface else R.color.surface_variant))
        b.textBand.background.mutate().setTint(
            ContextCompat.getColor(ctx, if (item.unlocked) R.color.brand_primary_container else R.color.surface)
        )
        b.textBand.setTextColor(
            ContextCompat.getColor(ctx, if (item.unlocked) R.color.brand_on_primary_container else R.color.text_secondary)
        )
        b.root.alpha = if (item.unlocked) 1f else 0.85f
        b.root.setOnClickListener { onClick(item) }
    }

    object Diff : DiffUtil.ItemCallback<TestItem>() {
        override fun areItemsTheSame(a: TestItem, b: TestItem) = a.test.id == b.test.id
        override fun areContentsTheSame(a: TestItem, b: TestItem) = a == b
    }
}

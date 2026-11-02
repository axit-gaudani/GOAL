package com.goal.ieltsreading.ui.common

import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.AndroidViewModel
import com.goal.ieltsreading.IeltsApp
import com.goal.ieltsreading.data.repo.LearningRepository

val Fragment.app: IeltsApp get() = requireActivity().application as IeltsApp

val AndroidViewModel.repository: LearningRepository get() = getApplication<IeltsApp>().repository

fun View.visibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/** Splits text into paragraphs on blank lines. */
fun String.paragraphs(): List<String> = split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }

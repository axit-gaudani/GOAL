package com.goal.ieltsreading.ui.tests

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.goal.ieltsreading.R
import com.goal.ieltsreading.databinding.FragmentTestResultBinding
import com.goal.ieltsreading.databinding.ItemReviewQuestionBinding
import com.goal.ieltsreading.databinding.ItemWordCardBinding
import com.goal.ieltsreading.domain.AnswerChecker
import com.goal.ieltsreading.domain.BandCalculator
import com.goal.ieltsreading.ui.common.app
import com.goal.ieltsreading.ui.common.visibleIf
import kotlinx.coroutines.launch

/** Shows the estimated band and a full answer review with Gujarati explanations. */
class TestResultFragment : Fragment() {

    private var _binding: FragmentTestResultBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTestResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.btnDone.setOnClickListener { findNavController().navigateUp() }
        val attemptId = requireArguments().getLong("attemptId")
        val repo = app.repository
        val showGu = repo.prefs.showGujarati

        viewLifecycleOwner.lifecycleScope.launch {
            val attempt = repo.attempt(attemptId) ?: return@launch
            val test = repo.test(attempt.testId)
            val questions = repo.questions(attempt.testId)
            val glossary = repo.vocabulary(attempt.testId)
            val answers = repo.answersOf(attempt)
            val b = _binding ?: return@launch

            b.textBand.text = BandCalculator.format(attempt.band)
            b.textTestTitle.text = test?.title.orEmpty()
            b.textScore.text = getString(R.string.score_format, attempt.correct, attempt.total)
            b.textTime.text = getString(R.string.time_taken_format, attempt.durationSeconds / 60, attempt.durationSeconds % 60)
            b.textAdvice.setText(
                when {
                    attempt.band >= 7.0 -> R.string.advice_high
                    attempt.band >= 5.5 -> R.string.advice_mid
                    else -> R.string.advice_low
                }
            )

            val ctx = requireContext()
            b.reviewContainer.removeAllViews()
            questions.forEachIndexed { i, q ->
                val item = ItemReviewQuestionBinding.inflate(layoutInflater, b.reviewContainer, false)
                val userAnswer = answers[q.id]
                val correct = AnswerChecker.isCorrect(q, userAnswer)
                item.textNumber.text = (i + 1).toString()
                item.textNumber.setBackgroundResource(if (correct) R.drawable.bg_circle_success else R.drawable.bg_circle_error)
                item.textPrompt.text = q.prompt
                item.textPromptGu.text = q.promptGu
                item.textPromptGu.visibleIf(showGu && q.promptGu.isNotBlank())
                item.textYourAnswer.text = getString(R.string.your_answer_format, userAnswer ?: getString(R.string.no_answer))
                item.textYourAnswer.setTextColor(ContextCompat.getColor(ctx, if (correct) R.color.success else R.color.error))
                item.textCorrectAnswer.text = getString(R.string.correct_answer_format, q.answer)
                item.textCorrectAnswer.visibleIf(!correct)
                item.textExplanation.text = q.explanation
                item.textExplanation.visibleIf(q.explanation.isNotBlank())
                item.textExplanationGu.text = q.explanationGu
                item.textExplanationGu.visibleIf(q.explanationGu.isNotBlank())
                b.reviewContainer.addView(item.root)
            }

            b.textGlossaryHeader.visibleIf(glossary.isNotEmpty())
            b.glossaryContainer.removeAllViews()
            for (w in glossary) {
                val item = ItemWordCardBinding.inflate(layoutInflater, b.glossaryContainer, false)
                item.textWord.text = w.word
                item.textPos.text = w.pos
                item.textMeaningGu.text = w.meaningGu
                item.textMeaningEn.text = w.meaningEn
                item.textExample.text = w.example
                item.textExampleGu.text = w.exampleGu
                b.glossaryContainer.addView(item.root)
            }
            b.btnAddGlossary.visibleIf(glossary.isNotEmpty())
            b.btnAddGlossary.setOnClickListener {
                app.appScope.launch { repo.addLessonWordsToDeck(attempt.testId) }
                b.btnAddGlossary.isEnabled = false
                b.btnAddGlossary.setText(R.string.words_added)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

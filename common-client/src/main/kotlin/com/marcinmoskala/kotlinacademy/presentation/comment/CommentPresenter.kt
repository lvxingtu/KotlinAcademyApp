package com.marcinmoskala.kotlinacademy.presentation.comment

import com.marcinmoskala.kotlinacademy.common.launchUI
import com.marcinmoskala.kotlinacademy.data.Feedback
import com.marcinmoskala.kotlinacademy.presentation.BasePresenter
import com.marcinmoskala.kotlinacademy.respositories.CommentRepository

class CommentPresenter(val view: CommentView): BasePresenter() {

    private val commentRepository by CommentRepository.lazyGet()

    fun onSendCommentClicked(feedback: Feedback) {
        view.loading = true
        jobs += launchUI {
            try {
                commentRepository.addComment(feedback)
                view.backToNewsAndShowSuccess()
            } catch (e: Throwable) {
                view.showError(e)
            } finally {
                view.loading = false
            }
        }
    }
}
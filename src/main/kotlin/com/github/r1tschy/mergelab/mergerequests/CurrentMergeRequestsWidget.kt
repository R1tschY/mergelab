package com.github.r1tschy.mergelab.mergerequests

import com.intellij.dvcs.DvcsUtil
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.EditorBasedWidget
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import com.intellij.util.Consumer
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.GitUtil
import git4idea.config.GitVcsSettings
import git4idea.repo.GitRepository
import org.jetbrains.annotations.Nls
import java.awt.Component
import java.awt.event.MouseEvent

class CurrentMergeRequestsWidget(project: Project) : EditorBasedWidget(project), StatusBarWidget.TextPresentation {

    private var mergeRequest: MergeRequest? = null
    private val mrService = project.service<CurrentMergeRequestsService>()

    init {
        update()

        mrService.subscribeChanges(this, object : CurrentMergeRequestsChangesListener {
            override fun onCurrentMergeRequestsChanged(remotes: List<MergeRequestWorkingCopy>) {
                invokeLater { update() }
            }
        })
    }

    override fun ID(): String = WIDGET_ID

    @RequiresEdt
    override fun getTooltipText(): String? {
        return mergeRequest?.let {
            return "Merge Request ${it.id}: ${it.title}"
        }
    }

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer<MouseEvent> {
            mergeRequest?.webUrl?.let { BrowserUtil.browse(it) }
        }
    }

    override fun getText(): String {
        return mergeRequest?.let { "!${it.iid.asString()}" } ?: "!???"
    }

    override fun getAlignment(): Float {
        return Component.LEFT_ALIGNMENT
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation {
        return this
    }

    private fun update() {
        val guessRepo = guessCurrentRepository()

        val allMergeRequests = mrService.getCurrentMergeRequests()
        val mergeRequests = guessRepo
            ?.let { repo -> allMergeRequests.filter { repo.root == it.repoRoot } }
            ?: allMergeRequests

        val openMergeRequests = mergeRequests.filter { it.mr.state == MergeRequestState.OPEN }

        mergeRequest = if (openMergeRequests.isNotEmpty()) {
            // TODO: use newest
            openMergeRequests[0].mr
        } else if (mergeRequests.isNotEmpty()) {
            // TODO: use newest
            mergeRequests[0].mr
        } else {
            null
        }
    }

    private fun guessCurrentRepository(): GitRepository? {
        return DvcsUtil.guessCurrentRepositoryQuick(
            project, GitUtil.getRepositoryManager(project),
            GitVcsSettings.getInstance(project).recentRootPath
        )
    }

    class Factory : StatusBarWidgetFactory {

        override fun getId(): String {
            return WIDGET_ID
        }

        override fun getDisplayName(): @Nls String {
            return "GitLab Merge Request"
        }

        override fun isAvailable(project: Project): Boolean {
            return project.service<CurrentMergeRequestsService>().getCurrentMergeRequests().isNotEmpty()
        }

        override fun createWidget(project: Project): StatusBarWidget {
            return CurrentMergeRequestsWidget(project)
        }

        override fun disposeWidget(widget: StatusBarWidget) {
            Disposer.dispose(widget)
        }

        override fun canBeEnabledOn(statusBar: StatusBar): Boolean {
            return true
        }
    }

    class ChangesListener(private val project: Project) : CurrentMergeRequestsChangesListener {
        override fun onCurrentMergeRequestsChanged(remotes: List<MergeRequestWorkingCopy>) {
            project.service<StatusBarWidgetsManager>().updateWidget(Factory::class.java)
        }
    }

    companion object {
        const val WIDGET_ID: String = "mergelab-mr"
    }
}
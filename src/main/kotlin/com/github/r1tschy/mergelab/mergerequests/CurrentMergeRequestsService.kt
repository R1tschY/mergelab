package com.github.r1tschy.mergelab.mergerequests

import com.github.r1tschy.mergelab.api.GitLabApiService
import com.github.r1tschy.mergelab.model.GitLabRemote
import com.github.r1tschy.mergelab.services.GitLabRemotesManager
import com.github.r1tschy.mergelab.services.GitlabRemoteChangesListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.BranchChangeListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.messages.Topic
import git4idea.repo.GitRepository
import org.jetbrains.annotations.CalledInAny
import java.util.stream.Collectors.toList

data class MergeRequestWorkingCopy(val repoRoot: VirtualFile, val mr: MergeRequest)

@Service
class CurrentMergeRequestsService(private val project: Project) : Disposable {

    @Volatile
    private var currentMergeRequests: List<MergeRequestWorkingCopy> = listOf()

    init {
        project.service<GitLabRemotesManager>()
            .subscribeRemotesChanges(this, object : GitlabRemoteChangesListener {
                override fun onRemotesChanged(remotes: List<GitLabRemote>) {
                    updateCurrentMergeRequests()
                }
            })
    }

    @CalledInAny
    fun getCurrentMergeRequests(): List<MergeRequestWorkingCopy> {
        return currentMergeRequests
    }

    @CalledInAny
    fun getCurrentMergeRequests(repo: GitRepository): List<MergeRequest> {
        val repoRoot = repo.root
        return currentMergeRequests.stream().filter { it.repoRoot == repoRoot }.map { it.mr }.collect(toList())
    }

    @RequiresBackgroundThread
    private fun fetchCurrentMergeRequests(progressIndicator: ProgressIndicator): List<MergeRequestWorkingCopy> {
        LOG.info("Detecting merge requests for current branches")
        val remotesManager: GitLabRemotesManager = project.service()
        val apiService: GitLabApiService = service()

        val result: MutableList<MergeRequestWorkingCopy> = mutableListOf()
        for (remote in remotesManager.remotes) {
            val currentBranch = remote.repo.currentBranch
            if (currentBranch != null) {
                val branchTrackInfo = remote.repo.getBranchTrackInfo(currentBranch.name)
                if (branchTrackInfo != null && branchTrackInfo.remote == remote.remote) {
                    val remoteBranchName = branchTrackInfo.remoteBranch.nameForRemoteOperations

                    // TODO: notify when token is missing
                    LOG.info("Searching for merge requests for ${currentBranch.name} on ${remote.projectCoord.server} ...")
                    val mergeRequests = apiService.apiFor(remote.projectCoord.server)
                        ?.findMergeRequestsUsingSourceBranch(
                            remote.projectCoord.projectPath,
                            remoteBranchName,
                            progressIndicator
                        )

                    if (mergeRequests != null && mergeRequests.isNotEmpty()) {
                        LOG.info("Found for merge requests for ${currentBranch.name} on ${remote.projectCoord.server}: $mergeRequests")
                        for (mr in mergeRequests) {
                            result.add(MergeRequestWorkingCopy(remote.repo.root, mr))
                        }
                    }
                }
            }
        }

        return result
    }

    private fun updateCurrentMergeRequests() {
        object : Task.Backgroundable(project, "Detecting GitLab merge requests") {
            override fun run(indicator: ProgressIndicator) {
                val currentMergeRequests = fetchCurrentMergeRequests(indicator)

                this@CurrentMergeRequestsService.currentMergeRequests = currentMergeRequests
                project.messageBus.syncPublisher(CurrentMergeRequestsChangesListener.TOPIC)
                    .onCurrentMergeRequestsChanged(currentMergeRequests)
            }
        }.queue()
    }

    class VcsChangesListener(private val project: Project) : BranchChangeListener {
        override fun branchWillChange(branchName: String) {}

        override fun branchHasChanged(branchName: String) {
            if (!project.isDisposed) {
                project.service<CurrentMergeRequestsService>().updateCurrentMergeRequests()
            }
        }
    }

    fun subscribeChanges(disposable: Disposable, listener: CurrentMergeRequestsChangesListener) {
        project.messageBus.connect(disposable).subscribe(CurrentMergeRequestsChangesListener.TOPIC, listener)
    }

    override fun dispose() {}

    companion object {
        private val LOG = logger<GitLabRemotesManager>()
    }
}

interface CurrentMergeRequestsChangesListener {
    fun onCurrentMergeRequestsChanged(remotes: List<MergeRequestWorkingCopy>)

    companion object {
        val TOPIC = Topic.create("Current Merge Requests Changes", CurrentMergeRequestsChangesListener::class.java)
    }
}
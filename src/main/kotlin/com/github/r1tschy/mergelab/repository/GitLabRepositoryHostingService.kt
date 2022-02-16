// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.repository

import com.intellij.dvcs.hosting.RepositoryListLoader
import com.intellij.openapi.project.Project
import git4idea.remote.GitRepositoryHostingService
import git4idea.remote.InteractiveGitHttpAuthDataProvider

class GitLabRepositoryHostingService : GitRepositoryHostingService() {
    override fun getServiceDisplayName(): String {
        return "GitLab"
    }

    override fun getRepositoryListLoader(project: Project): RepositoryListLoader? {
        return null
    }

    override fun getInteractiveAuthDataProvider(project: Project, url: String): InteractiveGitHttpAuthDataProvider? {
        return null
    }

    override fun getInteractiveAuthDataProvider(
        project: Project,
        url: String,
        login: String
    ): InteractiveGitHttpAuthDataProvider? {
        return null
    }
}
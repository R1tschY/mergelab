package com.github.r1tschy.mergelab.api

interface GitLabApi : GitLabUserApiService, GitlabProtectedBranchesApiService, GitlabProjectsApiService,
    GitlabMergeRequestsApiService
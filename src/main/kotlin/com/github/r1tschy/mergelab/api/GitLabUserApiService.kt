package com.github.r1tschy.mergelab.api

import com.intellij.collaboration.auth.AccountDetails
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import java.awt.Image

/**
 * Details about GitLab user.
 */
data class UserDetails(
    /**
     * Unique name within GitLab instance.
     */
    val username: String,
    /**
     * Display name.
     */
    override val name: String,
    /**
     * URL for avatar (can be absolute or relative).
     */
    val avatarLocation: String?
) : AccountDetails


/**
 * Service to load user information.
 */
interface GitLabUserApiService {
    @RequiresBackgroundThread
    fun getUserDetails(processIndicator: ProgressIndicator): UserDetails

    @RequiresBackgroundThread
    fun getAvatar(processIndicator: ProgressIndicator, location: String): Image?
}
// Copyright 2022 Richard Liebscher.
// Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.r1tschy.mergelab.accounts.ui

import com.github.r1tschy.mergelab.accounts.GitLabAccount
import com.github.r1tschy.mergelab.accounts.GitLabAuthService
import com.github.r1tschy.mergelab.accounts.GitLabAvatarService
import com.github.r1tschy.mergelab.api.GitLabApi
import com.github.r1tschy.mergelab.api.GitLabApiService
import com.github.r1tschy.mergelab.api.UserDetails
import com.github.r1tschy.mergelab.exceptions.UnauthorizedAccessException
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.auth.ui.LoadingAccountsDetailsProvider
import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import java.awt.Image
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

internal class GitLabAccountsDetailsProvider(
    progressIndicatorsProvider: ProgressIndicatorsProvider, private val accountsModel: GitLabAccountsModel
) : LoadingAccountsDetailsProvider<GitLabAccount, UserDetails>(progressIndicatorsProvider) {
    override fun scheduleLoad(
        account: GitLabAccount,
        indicator: ProgressIndicator
    ): CompletableFuture<DetailsLoadingResult<UserDetails>> {
        val api: GitLabApi = (accountsModel.newCredentials[account] ?: service<GitLabAuthService>().getToken(account))
            ?.let { service<GitLabApiService>().apiFor(account.server, it) }
            ?: return completedFuture(tokenError("Missing access token"))

        return ProgressManager.getInstance()
            .submitIOTask(indicator) {
                try {
                    val userDetails = api.getUserDetails(it)
                    val image = userDetails.avatarLocation?.let { location ->
                        service<GitLabAvatarService>().loadAvatarSync(api, location, indicator)
                    }
                    success(userDetails, image)
                } catch (e: UnauthorizedAccessException) {
                    tokenError("Access token is not valid")
                }
            }
    }

    private fun tokenError(message: String) = DetailsLoadingResult<UserDetails>(null, null, message, true)
    private fun success(user: UserDetails, image: Image?) = DetailsLoadingResult(user, image, null, false)
}
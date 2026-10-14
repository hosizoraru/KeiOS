package os.kei.ui.page.main.github.page

internal fun shouldDismissGitHubActionsForNavigation(token: Int, consumedToken: Int): Boolean =
    token > 0 && token != consumedToken

internal fun canConsumeGitHubActionsOpenRequest(
    token: Int,
    consumedToken: Int,
    ownerActive: Boolean,
    contentReady: Boolean,
    trackId: String?,
): Boolean =
    token != 0 &&
        token != consumedToken &&
        ownerActive &&
        contentReady &&
        !trackId.isNullOrBlank()

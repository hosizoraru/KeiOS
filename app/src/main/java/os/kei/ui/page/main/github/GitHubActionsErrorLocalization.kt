package os.kei.ui.page.main.github

import android.content.Context
import os.kei.R

internal fun localizedGitHubActionsErrorMessage(
    context: Context,
    rawMessage: String?
): String {
    val message = rawMessage?.trim().orEmpty()
    if (message.isBlank()) return context.getString(R.string.common_unknown)
    val lower = message.lowercase()
    return when {
        message == "A GitHub token is required to download Actions artifacts" ||
            message == "GitHub Actions API Token mode requires a token" ||
            message == LEGACY_ACTIONS_DOWNLOAD_TOKEN_REQUIRED ||
            message == LEGACY_ACTIONS_API_TOKEN_REQUIRED ->
            context.getString(R.string.github_actions_error_detail_token_required)

        message == "The artifact is missing a download URL and repository information" ||
            message == "The artifact is missing a nightly.link download URL" ||
            message == LEGACY_ARTIFACT_MISSING_DOWNLOAD ->
            context.getString(R.string.github_actions_error_detail_artifact_missing_download)

        message == "GitHub Actions artifact download returned no redirect URL" ||
            message == LEGACY_ARTIFACT_NO_REDIRECT ->
            context.getString(R.string.github_actions_error_detail_artifact_no_redirect)

        message.startsWith("GitHub public API found no matching workflow:") ||
            message.startsWith(LEGACY_WORKFLOW_NOT_FOUND_PREFIX) -> {
            val workflowId = message.substringAfterLast(':')
                .substringAfterLast('\uff1a')
                .trim()
                .ifBlank { context.getString(R.string.common_unknown) }
            context.getString(R.string.github_actions_error_detail_workflow_not_found, workflowId)
        }

        message == "GitHub Actions token is invalid or expired" ||
            message == LEGACY_ACTIONS_TOKEN_INVALID ->
            context.getString(R.string.github_actions_error_detail_token_invalid)

        message.startsWith("GitHub Actions guest API is rate limited") ||
            message.startsWith(LEGACY_ACTIONS_GUEST_RATE_LIMITED_PREFIX) ->
            context.getString(R.string.github_actions_error_detail_guest_rate_limited)

        message == "GitHub Actions API is rate limited" ||
            message == LEGACY_ACTIONS_API_RATE_LIMITED ->
            context.getString(R.string.github_actions_error_detail_api_rate_limited)

        message.startsWith("GitHub Actions API access was denied") ||
            message.startsWith(LEGACY_ACTIONS_API_FORBIDDEN_PREFIX) -> {
            val suffix = message
                .substringAfter("denied", "")
                .ifBlank { message.substringAfter(LEGACY_ACTIONS_API_FORBIDDEN_PREFIX, "") }
            context.getString(R.string.github_actions_error_detail_api_forbidden, suffix)
        }

        message == "The repository or GitHub Actions resource does not exist, or the current token lacks access" ||
            message == LEGACY_ACTIONS_RESOURCE_MISSING ->
            context.getString(R.string.github_actions_error_detail_resource_missing)

        message == "The GitHub Actions artifact has expired" ||
            message == LEGACY_ACTIONS_ARTIFACT_EXPIRED ->
            context.getString(R.string.github_actions_error_detail_artifact_expired)

        message.startsWith("nightly.link cannot directly download raw APK artifact") ||
            message.startsWith(LEGACY_NIGHTLY_RAW_ARTIFACT_PREFIX) -> {
            val name = message
                .substringAfter("artifact", "")
                .substringAfter(':', "")
                .substringAfter('\uff1a', "")
                .substringBefore('.')
                .substringBefore('\u3002')
                .trim()
                .ifBlank { "artifact" }
            context.getString(R.string.github_actions_error_detail_nightly_raw_artifact, name)
        }

        message == "nightly.link did not read workflow artifacts" ||
            message == LEGACY_NIGHTLY_NO_WORKFLOW_ARTIFACT ->
            context.getString(R.string.github_actions_error_detail_nightly_no_workflow_artifact)

        message.startsWith("nightly.link did not read downloadable artifacts") ||
            message.startsWith(LEGACY_NIGHTLY_NO_DOWNLOADABLE_ARTIFACTS_PREFIX) ->
            context.getString(R.string.github_actions_error_detail_nightly_no_downloadable_artifacts)

        lower.contains("access denied") || message.contains(LEGACY_ACCESS_DENIED) ->
            context.getString(R.string.github_actions_error_detail_nightly_access_denied)

        lower.contains("could not find the actions resource") ||
            lower.contains("could not find matching actions resources") ||
            message.contains(LEGACY_ACTIONS_RESOURCE_NOT_FOUND) ->
            context.getString(R.string.github_actions_error_detail_nightly_resource_missing)

        lower.contains("expired artifact") || message.contains(LEGACY_ARTIFACT_EXPIRED) ->
            context.getString(R.string.github_actions_error_detail_nightly_artifact_expired)

        lower.contains("rate limited") || message.contains(LEGACY_RATE_LIMITED) ->
            context.getString(R.string.github_actions_error_detail_nightly_rate_limited)

        message.startsWith("GitHub Actions request failed") ||
            message.startsWith(LEGACY_ACTIONS_REQUEST_FAILED_PREFIX) ->
            context.getString(R.string.github_actions_error_detail_request_failed, message)

        lower.contains("read failed") || message.contains(LEGACY_READ_FAILED) ->
            context.getString(R.string.github_actions_error_detail_nightly_read_failed, message)

        else -> message
    }
}

internal fun localizedGitHubShareImportErrorMessage(
    context: Context,
    rawMessage: String?
): String {
    val message = rawMessage?.trim().orEmpty()
    if (message.isBlank()) return context.getString(R.string.common_unknown)
    return when (message) {
        "No valid GitHub link was detected",
        LEGACY_SHARE_NO_VALID_LINK ->
            context.getString(R.string.github_share_import_error_no_valid_link)

        "The target release contains no usable APK",
        LEGACY_SHARE_NO_USABLE_APK ->
            context.getString(R.string.github_share_import_error_no_usable_apk)

        "Share import resolution failed",
        LEGACY_SHARE_RESOLVE_FAILED ->
            context.getString(R.string.github_share_import_error_resolve_failed)

        "This repository has no usable release",
        LEGACY_SHARE_NO_RELEASE ->
            context.getString(R.string.github_share_import_error_no_release)

        "The shared link is missing a release tag",
        LEGACY_SHARE_MISSING_RELEASE_TAG ->
            context.getString(R.string.github_share_import_error_missing_release_tag)

        "This repository has no stable release",
        LEGACY_SHARE_NO_STABLE_RELEASE ->
            context.getString(R.string.github_share_import_error_no_stable_release)

        else -> localizedGitHubActionsErrorMessage(context, message)
    }
}

private const val LEGACY_ACTIONS_DOWNLOAD_TOKEN_REQUIRED =
    "\u4e0b\u8f7d GitHub Actions artifact \u9700\u8981\u586b\u5199 token"
private const val LEGACY_ACTIONS_API_TOKEN_REQUIRED =
    "GitHub Actions API Token \u9700\u8981\u586b\u5199 token"
private const val LEGACY_ARTIFACT_MISSING_DOWNLOAD =
    "artifact \u7f3a\u5c11\u4e0b\u8f7d\u5730\u5740\uff0c\u4e14\u672a\u63d0\u4f9b\u4ed3\u5e93\u4fe1\u606f"
private const val LEGACY_ARTIFACT_NO_REDIRECT =
    "GitHub Actions artifact \u4e0b\u8f7d\u672a\u8fd4\u56de\u8df3\u8f6c\u5730\u5740"
private const val LEGACY_WORKFLOW_NOT_FOUND_PREFIX =
    "GitHub \u516c\u5f00 API \u6ca1\u6709\u627e\u5230\u5339\u914d workflow\uff1a"
private const val LEGACY_ACTIONS_TOKEN_INVALID =
    "GitHub Actions token \u65e0\u6548\u6216\u5df2\u8fc7\u671f"
private const val LEGACY_ACTIONS_GUEST_RATE_LIMITED_PREFIX =
    "GitHub Actions \u6e38\u5ba2 API \u5df2\u9650\u6d41"
private const val LEGACY_ACTIONS_API_RATE_LIMITED =
    "GitHub Actions API \u5df2\u9650\u6d41"
private const val LEGACY_ACTIONS_API_FORBIDDEN_PREFIX =
    "GitHub Actions API \u88ab\u62d2\u7edd\u8bbf\u95ee"
private const val LEGACY_ACTIONS_RESOURCE_MISSING =
    "\u4ed3\u5e93\u6216 GitHub Actions \u8d44\u6e90\u4e0d\u5b58\u5728\uff0c\u6216\u5f53\u524d token \u65e0\u6743\u8bbf\u95ee"
private const val LEGACY_ACTIONS_ARTIFACT_EXPIRED =
    "GitHub Actions artifact \u5df2\u8fc7\u671f"
private const val LEGACY_NIGHTLY_RAW_ARTIFACT_PREFIX =
    "nightly.link \u5f53\u524d\u65e0\u6cd5\u76f4\u63a5\u4e0b\u8f7d\u539f\u59cb APK artifact"
private const val LEGACY_NIGHTLY_NO_WORKFLOW_ARTIFACT =
    "nightly.link \u6ca1\u6709\u8bfb\u53d6\u5230 workflow artifact"
private const val LEGACY_NIGHTLY_NO_DOWNLOADABLE_ARTIFACTS_PREFIX =
    "nightly.link \u6ca1\u6709\u8bfb\u53d6\u5230"
private const val LEGACY_ACCESS_DENIED = "\u8bbf\u95ee\u88ab\u62d2\u7edd"
private const val LEGACY_ACTIONS_RESOURCE_NOT_FOUND = "\u6ca1\u6709\u627e\u5230\u5bf9\u5e94 Actions \u8d44\u6e90"
private const val LEGACY_ARTIFACT_EXPIRED = "artifact \u5df2\u8fc7\u671f"
private const val LEGACY_RATE_LIMITED = "\u9650\u6d41"
private const val LEGACY_ACTIONS_REQUEST_FAILED_PREFIX = "GitHub Actions \u8bf7\u6c42\u5931\u8d25"
private const val LEGACY_READ_FAILED = "\u8bfb\u53d6\u5931\u8d25"
private const val LEGACY_SHARE_NO_VALID_LINK =
    "\u672a\u8bc6\u522b\u5230\u6709\u6548\u7684 GitHub \u94fe\u63a5"
private const val LEGACY_SHARE_NO_USABLE_APK =
    "\u76ee\u6807 release \u4e2d\u672a\u627e\u5230\u53ef\u7528 APK"
private const val LEGACY_SHARE_RESOLVE_FAILED =
    "\u5206\u4eab\u5bfc\u5165\u89e3\u6790\u5931\u8d25"
private const val LEGACY_SHARE_NO_RELEASE =
    "\u8be5\u4ed3\u5e93\u6682\u65e0\u53ef\u7528 release"
private const val LEGACY_SHARE_MISSING_RELEASE_TAG =
    "\u5206\u4eab\u94fe\u63a5\u7f3a\u5c11 release tag"
private const val LEGACY_SHARE_NO_STABLE_RELEASE =
    "\u8be5\u4ed3\u5e93\u6682\u65e0\u7a33\u5b9a\u7248 release"

package os.kei.feature.github.domain

import os.kei.feature.github.model.GitHubActionsArtifact
import os.kei.feature.github.model.GitHubActionsDownloadRecord
import os.kei.feature.github.model.GitHubActionsWorkflow
import os.kei.feature.github.model.GitHubActionsWorkflowRun
import java.util.Locale

object GitHubActionsDownloadHistoryMatcher {
    fun latestForArtifact(
        artifact: GitHubActionsArtifact,
        history: List<GitHubActionsDownloadRecord>
    ): GitHubActionsDownloadRecord? {
        if (history.isEmpty()) return null
        return history
            .asSequence()
            .filter { record ->
                val exactId = artifact.id > 0L && record.artifactId == artifact.id
                val sameDigest = artifact.digest.isNotBlank() &&
                    record.artifactDigest.equals(artifact.digest, ignoreCase = true)
                val sameName = artifact.name.isNotBlank() &&
                    record.artifactName.equals(artifact.name, ignoreCase = true)
                exactId || sameDigest || sameName
            }
            .maxByOrNull { it.downloadedAtMillis }
    }

    fun latestForRun(
        run: GitHubActionsWorkflowRun,
        history: List<GitHubActionsDownloadRecord>
    ): GitHubActionsDownloadRecord? {
        if (history.isEmpty()) return null
        return history
            .asSequence()
            .filter { record ->
                val exactRun = run.id > 0L && record.runId == run.id
                val sameSha = run.headSha.isNotBlank() &&
                    record.headSha.equals(run.headSha, ignoreCase = true)
                exactRun || sameSha
            }
            .maxByOrNull { it.downloadedAtMillis }
    }

    fun latestForWorkflow(
        workflow: GitHubActionsWorkflow,
        history: List<GitHubActionsDownloadRecord>
    ): GitHubActionsDownloadRecord? {
        if (history.isEmpty()) return null
        val normalizedPath = workflow.path.normalized()
        val normalizedName = workflow.name.normalized()
        return history
            .asSequence()
            .filter { record ->
                val exactWorkflow = workflow.id > 0L && record.workflowId == workflow.id
                val samePath = normalizedPath.isNotBlank() && record.workflowPath.normalized() == normalizedPath
                val sameName = normalizedName.isNotBlank() && record.workflowName.normalized() == normalizedName
                exactWorkflow || samePath || sameName
            }
            .maxByOrNull { it.downloadedAtMillis }
    }

    fun recencyScore(record: GitHubActionsDownloadRecord, nowMillis: Long = System.currentTimeMillis()): Int {
        val ageMillis = (nowMillis - record.downloadedAtMillis).coerceAtLeast(0L)
        return when {
            ageMillis <= 3L * dayMillis -> 18
            ageMillis <= 14L * dayMillis -> 12
            ageMillis <= 45L * dayMillis -> 6
            else -> 2
        }
    }

    private fun String.normalized(): String {
        return trim().lowercase(Locale.ROOT)
    }

    private const val dayMillis = 24L * 60L * 60L * 1000L
}

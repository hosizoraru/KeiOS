package com.example.keios.ui.page.main

import com.example.keios.feature.github.model.GitHubLookupConfig
import com.example.keios.feature.github.model.GitHubLookupStrategyOption
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GitHubPageModelsTest {
    @Test
    fun `overview api label uses compact fine grained fingerprint`() {
        val config = GitHubLookupConfig(
            selectedStrategy = GitHubLookupStrategyOption.GitHubApiToken,
            apiToken = "github_pat_abCDef_1234567890XYZ"
        )

        assertEquals("FG ab…YZ", config.overviewApiLabel())
    }

    @Test
    fun `overview api label uses compact classic fingerprint`() {
        val config = GitHubLookupConfig(
            selectedStrategy = GitHubLookupStrategyOption.GitHubApiToken,
            apiToken = "ghp_abcd1234wxyz"
        )

        assertEquals("CL ab…yz", config.overviewApiLabel())
    }

    @Test
    fun `overview api label shows guest when token is blank`() {
        val config = GitHubLookupConfig(
            selectedStrategy = GitHubLookupStrategyOption.GitHubApiToken,
            apiToken = "   "
        )

        assertEquals("游客", config.overviewApiLabel())
    }

    @Test
    fun `overview api label shows unused outside api strategy`() {
        val config = GitHubLookupConfig(
            selectedStrategy = GitHubLookupStrategyOption.AtomFeed,
            apiToken = "ghp_abcd1234wxyz"
        )

        assertEquals("未使用", config.overviewApiLabel())
    }

    @Test
    fun `fine grained template url presets read only contents`() {
        val url = buildGitHubFineGrainedTokenTemplateUrl()

        assertTrue(url.contains("contents=read"))
        assertTrue(url.contains("expires_in=90"))
        assertTrue(url.contains("KeiOS%20Release%20Read"))
    }

    @Test
    fun `recommended token guide clarifies public access and selected repo cap`() {
        assertTrue(
            githubRecommendedTokenGuide.collapsedSummary.contains("Fine-grained PAT")
        )
        assertTrue(
            githubRecommendedTokenGuide.summary.contains("公开仓库追踪不受选仓限制")
        )
        assertTrue(
            githubRecommendedTokenGuide.fields.any { it.label == "上限" && it.value.contains("50") }
        )
        assertTrue(
            githubRecommendedTokenGuide.notes.any {
                it.contains("Only select repositories") && it.contains("public repo")
            }
        )
        assertTrue(
            githubRecommendedTokenGuide.notes.any {
                it.contains("MAX 50 repositories") && it.contains("当前 owner")
            }
        )
    }
}

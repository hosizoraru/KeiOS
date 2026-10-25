package os.kei.ci

import org.junit.Test
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitHubWorkflowContractTest {
    @Test
    fun `debug apk workflow includes unit tests as parallel job`() {
        val workflow = workflowText("ci-debug-apk.yml")
        val setupAction = actionText("setup-android-gradle-build/action.yml")

        // An unqualified task selector also covers modules added after this test was written.
        val testCommands = runCommandsOf(workflow).map { it.split(Regex("\\s+")) }
            .filter { "testDebugUnitTest" in it }
        assertEquals(1, testCommands.size, "CI must run every module's unit tests")
        val testCommand = testCommands.single()
        listOf("./gradlew", ":build-logic:check", "testDebugUnitTest",
            "-Proborazzi.test.verify=true", "--continue", "--stacktrace").forEach {
            assertContains(testCommand, it, "The unit test job must validate build logic and screenshots")
        }
        // Commands only, so the comment explaining the history can still name the old task.
        val qualified = runCommandsOf(workflow).filter { MODULE_QUALIFIED_TEST_TASK.containsMatchIn(it) }
        assertTrue(
            qualified.isEmpty(),
            "run unit tests with the unqualified `testDebugUnitTest`; a module-qualified task tests only " +
                "that module: $qualified",
        )
        // Only the automatic debug APK job writes the shared cache.
        assertContains(
            workflow,
            "cache-read-only: \"true\"",
            message = "the unit test job is cache-read-only on purpose; see the comment at its call site",
        )
        assertSetupActionUsesPinnedActions(setupAction)
    }

    @Test
    fun `apk workflows keep expected assemble tasks and artifact signer verification`() {
        val debugWorkflow = workflowText("ci-debug-apk.yml")
        val benchmarkWorkflow = workflowText("ci-benchmark-apk.yml")

        assertWorkflowUsesPinnedActions(debugWorkflow)
        assertContains(debugWorkflow, "./gradlew :app:assembleDebug --stacktrace")
        assertContains(debugWorkflow, "EXPECTED_APK_SIGNER_SHA256")
        assertContains(debugWorkflow, "apksigner\" verify --print-certs")

        assertWorkflowUsesPinnedActions(benchmarkWorkflow)
        assertContains(benchmarkWorkflow, "\":app:assembleBenchmark\"")
        assertContains(benchmarkWorkflow, "lintVitalBenchmark")
        assertContains(benchmarkWorkflow, "EXPECTED_APK_SIGNER_SHA256")
        assertContains(benchmarkWorkflow, "apksigner\" verify --print-certs")
    }

    /**
     * The predecessor of this test named `app`, `core-io` and eighteen other module paths in an
     * *allowlist*, which is the wrong shape and had already failed: by the time anyone looked,
     * `core-download`, `core-notification`, `core-versioning`, `feature-keepalive` and
     * `feature-github-engine` existed and were in neither the workflows nor this test, so a commit
     * touching only one of them ran no CI at all and said nothing about it. A list written by hand
     * cannot catch the module added after it was written.
     *
     * So this asks the question the other way round, against the module list Gradle itself uses: no
     * module, and none of the files that decide how every module builds, may be excluded from a run.
     */
    @Test
    fun `no module is excluded from the apk workflows`() {
        val buildInputs =
            gradleModules().map { "$it/build.gradle.kts" } +
                listOf(
                    "build.gradle.kts",
                    "settings.gradle.kts",
                    "gradle.properties",
                    "gradle/libs.versions.toml",
                    "gradlew",
                    ".github/actions/setup-android-gradle-build/action.yml",
                    "build-logic/build.gradle.kts",
                    "build-logic/settings.gradle.kts",
                ) + File(repoRoot(), "build-logic/src").walkTopDown()
                    .filter { it.isFile && it.extension in setOf("kt", "kts") }
                    .map { it.relativeTo(repoRoot()).invariantSeparatorsPath }
                    .toList()

        listOf("ci-debug-apk.yml", "ci-benchmark-apk.yml").forEach { name ->
            val workflow = workflowText(name)

            val filters = triggerPathFilters(workflow)
            assertTrue(filters.isNotEmpty(), "$name has no path filters to check")
            filters.forEach { filter ->
                if (filter.includesPaths) {
                    assertEquals(
                        "**",
                        filter.patterns.first().first,
                        "$name must start by including every module, including future modules",
                    )
                }
                (buildInputs + "future-module/src/main/kotlin/Feature.kt").forEach { path ->
                    assertTrue(
                        filter.triggersOn(path),
                        "$name would skip a change to $path under ${filter.patterns.map { it.first }}",
                    )
                }
            }
        }
    }

    @Test
    fun `debug ci includes its reporter while excluding local documentation and helpers`() {
        val filters = triggerPathFilters(workflowText("ci-debug-apk.yml"))
        assertTrue(filters.isNotEmpty())
        filters.forEach { filter ->
            assertTrue(filter.triggersOn("scripts/qa/test_report.sh"))
            listOf(
                "README.md",
                "docs/build/github-actions.md",
                ".agents/skills/example/SKILL.md",
                "scripts/qa/device.local.sh",
                "scripts/perf/profile.sh",
            ).forEach { path ->
                assertFalse(filter.triggersOn(path), "Local-only changes should not trigger CI: $path")
            }
        }
    }

    @Test
    fun `tracked workflow set stays explicit`() {
        val workflows =
            workflowsDir()
                .listFiles { file -> file.isFile && file.extension == "yml" }
                .orEmpty()
                .map { it.name }
                .sorted()

        assertEquals(
            listOf(
                "ci-benchmark-apk.yml",
                "ci-debug-apk.yml",
            ),
            workflows,
        )
    }

    private fun assertWorkflowUsesPinnedActions(workflow: String) {
        val used = actionsUsedBy(workflow)
        assertActionPinned(used, "actions/checkout")
        assertActionPinned(used, "actions/upload-artifact")
        assertContains(workflow, "persist-credentials: false")
    }

    private fun assertSetupActionUsesPinnedActions(action: String) {
        val used = actionsUsedBy(action)
        assertActionPinned(used, "actions/setup-java")
        assertActionPinned(used, "gradle/actions/setup-gradle")
        // Wrapper validation belongs to setup-gradle, not to a second action of its own.
        assertContains(action, "validate-wrappers: true")
        assertFalse(
            used.any { it.startsWith("gradle/actions/wrapper-validation") },
            "wrapper validation is setup-gradle's `validate-wrappers`; a separate action repeats it",
        )
        assertFalse(
            used.any { it.startsWith("android-actions/setup-android") },
            "the runner image ships build-tools and the platform this build names, and that " +
                "action's default package list is what took CI down when Google deleted `tools`",
        )
    }

    private fun assertActionPinned(used: List<String>, action: String) {
        val references = used.filter { it.startsWith("$action@") }
        assertTrue(references.isNotEmpty(), "Expected a use of $action")
        references.forEach { reference ->
            assertTrue(
                Regex("[0-9a-f]{40}").matches(reference.substringAfter('@')),
                "$action must be fixed to a full commit SHA",
            )
        }
    }

    /**
     * The actions a workflow or composite action actually runs, read from its `uses:` lines --
     * so that the prose explaining why something was removed can name it without tripping a check.
     */
    private fun actionsUsedBy(text: String): List<String> =
        USES_LINE.findAll(text).map { it.groupValues[1] }.toList()

    /** The command of every single-line `run:` step. */
    private fun runCommandsOf(workflow: String): List<String> =
        RUN_LINE.findAll(workflow).map { it.groupValues[1] }.toList()

    private fun gradleModules(): List<String> =
        GRADLE_INCLUDE
            .findAll(File(repoRoot(), "settings.gradle.kts").readText())
            .map { it.groupValues[1] }
            .toList()
            .also { assertTrue(it.size > 10, "Parsed only ${it.size} modules from settings.gradle.kts") }

    private data class TriggerPathFilter(
        val includesPaths: Boolean,
        val patterns: MutableList<Pair<String, Regex>> = mutableListOf(),
    ) {
        fun triggersOn(path: String): Boolean {
            if (!includesPaths) return patterns.none { (_, regex) -> regex.matches(path) }
            return patterns.fold(false) { included, (glob, regex) ->
                if (regex.matches(path)) !glob.startsWith('!') else included
            }
        }
    }

    /** Preserve ordering: a later positive paths entry can restore an excluded CI helper. */
    private fun triggerPathFilters(workflow: String): List<TriggerPathFilter> {
        val filters = mutableListOf<TriggerPathFilter>()
        var active: TriggerPathFilter? = null
        workflow.lineSequence().forEach { line ->
            val header = PATH_FILTER_HEADER.matchEntire(line)
            val entry = PATH_FILTER_ENTRY.matchEntire(line)
            when {
                header != null -> {
                    active = TriggerPathFilter(header.groupValues[1] == "paths")
                    filters += checkNotNull(active)
                }
                active == null -> Unit
                entry != null -> {
                    val glob = entry.groupValues[1]
                    active?.patterns?.add(glob to globToRegex(glob.removePrefix("!")))
                }
                line.isBlank() || line.trimStart().startsWith("#") -> Unit
                else -> active = null
            }
        }
        filters.forEach { assertTrue(it.patterns.isNotEmpty(), "Empty trigger path filter") }
        return filters
    }

    private fun globToRegex(glob: String): Regex {
        val pattern = StringBuilder()
        var index = 0
        while (index < glob.length) {
            when {
                glob.startsWith("**/", index) -> { pattern.append("(?:.*/)?"); index += 3 }
                glob.startsWith("**", index) -> { pattern.append(".*"); index += 2 }
                glob[index] == '*' -> { pattern.append("[^/]*"); index += 1 }
                else -> { pattern.append(Regex.escape(glob[index].toString())); index += 1 }
            }
        }
        return Regex(pattern.toString())
    }

    private fun workflowText(name: String): String {
        val file = File(workflowsDir(), name)
        assertTrue(file.isFile, "Missing workflow: $name")
        return file.readText()
    }

    private fun actionText(name: String): String {
        val file = File(actionsDir(), name)
        assertTrue(file.isFile, "Missing action: $name")
        return file.readText()
    }

    private fun workflowsDir(): File = File(repoRoot(), ".github/workflows")

    private fun actionsDir(): File = File(repoRoot(), ".github/actions")

    private fun repoRoot(): File {
        val start = File(checkNotNull(System.getProperty("user.dir"))).absoluteFile
        return generateSequence(start) { it.parentFile }
            .firstOrNull { File(it, ".github/workflows").isDirectory && File(it, "settings.gradle.kts").isFile }
            ?: error("Cannot locate the repository root from ${start.path}")
    }

    private companion object {
        val GRADLE_INCLUDE = Regex("""^\s*include\("::?([A-Za-z0-9._-]+)"\)""", RegexOption.MULTILINE)
        val PATH_FILTER_HEADER = Regex("""\s*(paths|paths-ignore):\s*""")
        val PATH_FILTER_ENTRY = Regex("""\s+- "([^"]+)"\s*""")
        val USES_LINE = Regex("""^\s*(?:- )?uses:\s*(\S+)(?:\s+#.*)?\s*$""", RegexOption.MULTILINE)
        val RUN_LINE = Regex("""^\s*(?:- )?run:\s*(\S.*)$""", RegexOption.MULTILINE)
        val MODULE_QUALIFIED_TEST_TASK = Regex(""":[A-Za-z0-9._-]+:test[A-Za-z]*UnitTest\b""")
    }
}

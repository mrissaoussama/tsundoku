package eu.kanade.presentation.more.settings.screen

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

/**
 * Tests for the settings reset logic that skips source-specific preference files.
 *
 * When resetting preferences, files with "source_" prefix must be preserved
 * to avoid losing novel source registration state (which would cause novels
 * to appear in the wrong library tab).
 */
@Execution(ExecutionMode.CONCURRENT)
class SettingsResetFilterTest {

    /**
     * Simulates the filter logic from SettingsAdvancedScreen reset handler.
     * Returns true if the preference file should be SKIPPED (not deleted).
     */
    private fun shouldSkipPrefFile(prefName: String): Boolean {
        return prefName.startsWith("source_")
    }

    @Test
    fun `source preference files should be skipped`() {
        shouldSkipPrefFile("source_12345") shouldBe true
        shouldSkipPrefFile("source_67890") shouldBe true
        shouldSkipPrefFile("source_novel_plugin") shouldBe true
    }

    @Test
    fun `non-source preference files should not be skipped`() {
        shouldSkipPrefFile("reader_settings") shouldBe false
        shouldSkipPrefFile("library_settings") shouldBe false
        shouldSkipPrefFile("app_state") shouldBe false
    }

    @Test
    fun `general preferences should not be skipped`() {
        shouldSkipPrefFile("tachiyomi") shouldBe false
        shouldSkipPrefFile("_has_set_default_values") shouldBe false
    }

    @Test
    fun `prefix match is case-sensitive`() {
        shouldSkipPrefFile("Source_12345") shouldBe false
        shouldSkipPrefFile("SOURCE_12345") shouldBe false
    }

    @Test
    fun `edge case - just the prefix without ID`() {
        shouldSkipPrefFile("source_") shouldBe true
    }

    @Test
    fun `files containing source_ but not as prefix`() {
        shouldSkipPrefFile("my_source_file") shouldBe false
        shouldSkipPrefFile("data_source_123") shouldBe false
    }

    @Test
    fun `filter correctly separates source and non-source files`() {
        val prefFiles = listOf(
            "tachiyomi",
            "source_12345",
            "reader_settings",
            "source_67890",
            "library_display",
            "source_novel_lnreader",
        )

        val toDelete = prefFiles.filterNot { shouldSkipPrefFile(it) }
        val toSkip = prefFiles.filter { shouldSkipPrefFile(it) }

        toDelete.size shouldBe 3
        toSkip.size shouldBe 3
        toDelete shouldBe listOf("tachiyomi", "reader_settings", "library_display")
        toSkip shouldBe listOf("source_12345", "source_67890", "source_novel_lnreader")
    }
}

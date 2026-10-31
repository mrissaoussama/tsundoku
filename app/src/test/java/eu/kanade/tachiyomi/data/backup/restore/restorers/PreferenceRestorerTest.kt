package eu.kanade.tachiyomi.data.backup.restore.restorers

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PreferenceRestorerTest {

    private val secretKey = "translation_openai_api_key"

    @Test
    fun `a blank restored secret keeps the saved one`() {
        assertTrue(keepsSavedSecret(secretKey, "", "sk-current"))
        assertTrue(keepsSavedSecret(secretKey, "  ", "sk-current"))
    }

    @Test
    fun `a non blank restored secret replaces the saved one`() {
        assertFalse(keepsSavedSecret(secretKey, "sk-restored", "sk-current"))
    }

    @Test
    fun `a blank restored secret is written when nothing is saved`() {
        assertFalse(keepsSavedSecret(secretKey, "", null))
        assertFalse(keepsSavedSecret(secretKey, "", ""))
    }

    @Test
    fun `a blank restored value for a normal key still replaces it`() {
        assertFalse(keepsSavedSecret("translation_target_language", "", "fr"))
    }
}

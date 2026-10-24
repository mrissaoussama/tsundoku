package mihon.core.migration.migrations

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference

class TranslationSecretsPrivateMigrationTest {

    private val key = "translation_openai_api_key"

    @Test
    fun `moves a plain key to its private key`() {
        val store = InMemoryPreferenceStore()
        store.getString(key, "").set("sk-test")

        moveTranslationSecrets(store)

        assertEquals("sk-test", store.getString(Preference.privateKey(key), "").get())
        assertFalse(store.getString(key, "").isSet())
    }

    @Test
    fun `keeps an existing private value over a restored plain one`() {
        val store = InMemoryPreferenceStore()
        store.getString(Preference.privateKey(key), "").set("current")
        store.getString(key, "").set("restored")

        moveTranslationSecrets(store)

        assertEquals("current", store.getString(Preference.privateKey(key), "").get())
        assertFalse(store.getString(key, "").isSet())
    }

    @Test
    fun `moves the custom http url because it can embed a key`() {
        val store = InMemoryPreferenceStore()
        val urlKey = "translation_custom_http_url"
        store.getString(urlKey, "").set("https://example.com/translate?key=abc")

        moveTranslationSecrets(store)

        assertEquals(
            "https://example.com/translate?key=abc",
            store.getString(Preference.privateKey(urlKey), "").get(),
        )
        assertFalse(store.getString(urlKey, "").isSet())
    }
}

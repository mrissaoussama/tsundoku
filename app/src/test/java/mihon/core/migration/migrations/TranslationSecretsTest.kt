package mihon.core.migration.migrations

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference
import tachiyomi.domain.translation.service.TranslationPreferences

class TranslationSecretsTest {

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
    fun `on launch a leftover plain key does not replace the private value`() {
        val store = InMemoryPreferenceStore()
        store.getString(Preference.privateKey(key), "").set("current")
        store.getString(key, "").set("leftover")

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

    @Test
    fun `a restored plain key replaces the private value`() {
        val store = InMemoryPreferenceStore()
        store.getString(Preference.privateKey(key), "").set("current")
        store.getString(key, "").set("restored")

        moveTranslationSecrets(store, restoredWins = true)

        assertEquals("restored", store.getString(Preference.privateKey(key), "").get())
        assertFalse(store.getString(key, "").isSet())
    }

    @Test
    fun `a blank restored key keeps the private value`() {
        val store = InMemoryPreferenceStore()
        store.getString(Preference.privateKey(key), "").set("current")
        store.getString(key, "").set("")

        moveTranslationSecrets(store, restoredWins = true)

        assertEquals("current", store.getString(Preference.privateKey(key), "").get())
        assertFalse(store.getString(key, "").isSet())
    }

    @Test
    fun `every private translation preference is listed in SECRET_KEYS`() {
        val prefs = TranslationPreferences(InMemoryPreferenceStore())
        val privateKeys = TranslationPreferences::class.java.methods
            .filter { it.parameterCount == 0 && Preference::class.java.isAssignableFrom(it.returnType) }
            .map { (it.invoke(prefs) as Preference<*>).key() }
            .filter { Preference.isPrivate(it) }
            .toSet()

        assertEquals(TranslationPreferences.SECRET_KEYS.map { Preference.privateKey(it) }.toSet(), privateKeys)
    }
}

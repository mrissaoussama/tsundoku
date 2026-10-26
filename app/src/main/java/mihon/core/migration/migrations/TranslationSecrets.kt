package mihon.core.migration.migrations

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.translation.service.TranslationPreferences

/**
 * Moves translation secrets saved under their plain keys (older versions, or a restored backup)
 * to their private keys. A restored value replaces the current one unless it is blank.
 */
internal fun moveTranslationSecrets(preferenceStore: PreferenceStore, restoredWins: Boolean = false) {
    TranslationPreferences.SECRET_KEYS.forEach { key ->
        val legacy = preferenceStore.getString(key, "")
        if (!legacy.isSet()) return@forEach
        val secret = preferenceStore.getString(Preference.privateKey(key), "")
        val restored = legacy.get()
        if ((restoredWins && restored.isNotBlank()) || !secret.isSet()) secret.set(restored)
        legacy.delete()
    }
}

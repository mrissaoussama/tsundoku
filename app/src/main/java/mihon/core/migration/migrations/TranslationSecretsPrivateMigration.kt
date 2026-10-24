package mihon.core.migration.migrations

import mihon.core.migration.Migration
import mihon.core.migration.MigrationContext
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.domain.translation.service.TranslationPreferences

class TranslationSecretsPrivateMigration : Migration {
    override val version: Float = Migration.ALWAYS

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val preferenceStore = migrationContext.get<PreferenceStore>() ?: return false
        moveTranslationSecrets(preferenceStore)
        return true
    }
}

internal fun moveTranslationSecrets(preferenceStore: PreferenceStore) {
    TranslationPreferences.SECRET_KEYS.forEach { key ->
        val legacy = preferenceStore.getString(key, "")
        if (!legacy.isSet()) return@forEach
        val secret = preferenceStore.getString(Preference.privateKey(key), "")
        if (secret.get().isEmpty()) secret.set(legacy.get())
        legacy.delete()
    }
}

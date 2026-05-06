package com.zai.autoresponder.data.dao

import androidx.room.*
import com.zai.autoresponder.data.entity.AppSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface AppSettingsDao {

    @Query("SELECT * FROM app_settings WHERE id = 'default' LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    @Query("SELECT * FROM app_settings WHERE id = 'default' LIMIT 1")
    suspend fun getSettingsSync(): AppSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: AppSettings)

    @Update
    suspend fun update(settings: AppSettings)

    @Query("UPDATE app_settings SET serviceEnabled = :enabled WHERE id = 'default'")
    suspend fun updateServiceEnabled(enabled: Boolean)

    @Query("UPDATE app_settings SET apiKey = :apiKey, apiKeyValid = :valid WHERE id = 'default'")
    suspend fun updateApiKey(apiKey: String?, valid: Boolean)

    @Query("UPDATE app_settings SET replyWhatsApp = :enabled WHERE id = 'default'")
    suspend fun updateWhatsApp(enabled: Boolean)

    @Query("UPDATE app_settings SET replyMessenger = :enabled WHERE id = 'default'")
    suspend fun updateMessenger(enabled: Boolean)

    @Query("UPDATE app_settings SET replyTelegram = :enabled WHERE id = 'default'")
    suspend fun updateTelegram(enabled: Boolean)

    @Query("UPDATE app_settings SET aiPersona = :persona WHERE id = 'default'")
    suspend fun updatePersona(persona: String)
}

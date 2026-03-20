package com.zai.autoresponder.data.repository

import com.zai.autoresponder.AIAutoResponderApp
import com.zai.autoresponder.data.dao.AppSettingsDao
import com.zai.autoresponder.data.dao.AutoReplyRuleDao
import com.zai.autoresponder.data.dao.ReplyHistoryDao
import com.zai.autoresponder.data.entity.AppSettings
import com.zai.autoresponder.data.entity.AutoReplyRule
import com.zai.autoresponder.data.entity.ReplyHistory
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class AutoReplyRepository {

    private val database = AIAutoResponderApp.getInstance().database
    private val ruleDao = database.autoReplyRuleDao()
    private val settingsDao = database.appSettingsDao()
    private val historyDao = database.replyHistoryDao()

    // Rules
    fun getAllRules(): Flow<List<AutoReplyRule>> = ruleDao.getAllRules()
    fun getEnabledRules(): Flow<List<AutoReplyRule>> = ruleDao.getEnabledRules()
    suspend fun getRuleById(id: String) = ruleDao.getRuleById(id)
    suspend fun getRuleByTrigger(trigger: String) = ruleDao.getRuleByTrigger(trigger)

    suspend fun createRule(trigger: String, response: String?, useAI: Boolean): AutoReplyRule {
        val rule = AutoReplyRule(
            id = UUID.randomUUID().toString(),
            trigger = trigger.lowercase().trim(),
            response = response,
            useAI = useAI,
            enabled = true,
            priority = 0
        )
        ruleDao.insert(rule)
        return rule
    }

    suspend fun updateRule(rule: AutoReplyRule) {
        ruleDao.update(rule.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteRule(rule: AutoReplyRule) {
        ruleDao.delete(rule)
    }

    suspend fun toggleRule(id: String, enabled: Boolean) {
        val rule = ruleDao.getRuleById(id) ?: return
        ruleDao.update(rule.copy(enabled = enabled, updatedAt = System.currentTimeMillis()))
    }

    // Settings
    fun getSettings(): Flow<AppSettings?> = settingsDao.getSettings()
    suspend fun getSettingsSync(): AppSettings? = settingsDao.getSettingsSync()

    suspend fun updateSettings(settings: AppSettings) {
        settingsDao.insert(settings)
    }

    suspend fun toggleService(enabled: Boolean) {
        val settings = settingsDao.getSettingsSync() ?: AppSettings()
        settingsDao.insert(settings.copy(serviceEnabled = enabled))
    }

    suspend fun updateApiKey(apiKey: String?, valid: Boolean) {
        val settings = settingsDao.getSettingsSync() ?: AppSettings()
        settingsDao.insert(settings.copy(apiKey = apiKey, apiKeyValid = valid))
    }

    // History
    fun getAllHistory(): Flow<List<ReplyHistory>> = historyDao.getAllHistory()
    fun getRecentHistory(limit: Int = 50): Flow<List<ReplyHistory>> = historyDao.getRecentHistory(limit)

    fun getTotalReplies(): Flow<Int> = historyDao.getTotalCount()
    fun getTodayReplies(): Flow<Int> {
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        return historyDao.getTodayCount(startOfDay)
    }

    suspend fun addHistory(
        platform: String,
        originalMsg: String,
        sentReply: String,
        triggerMatch: String? = null,
        usedAI: Boolean = false,
        responseTime: Int? = null
    ) {
        val history = ReplyHistory(
            id = UUID.randomUUID().toString(),
            platform = platform,
            originalMsg = originalMsg,
            sentReply = sentReply,
            triggerMatch = triggerMatch,
            usedAI = usedAI,
            responseTime = responseTime
        )
        historyDao.insert(history)
    }

    suspend fun clearHistory() {
        historyDao.clearAll()
    }

    companion object {
        @Volatile
        private var instance: AutoReplyRepository? = null

        fun getInstance(): AutoReplyRepository {
            return instance ?: synchronized(this) {
                instance ?: AutoReplyRepository().also { instance = it }
            }
        }
    }
}

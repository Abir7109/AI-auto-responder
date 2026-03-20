package com.zai.autoresponder.data.dao

import androidx.room.*
import com.zai.autoresponder.data.entity.UserProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 'default'")
    fun getProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 'default'")
    suspend fun getProfileSync(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(profile: UserProfile)

    @Query("UPDATE user_profile SET name = :name, updatedAt = :timestamp WHERE id = 'default'")
    suspend fun updateName(name: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET profession = :profession, updatedAt = :timestamp WHERE id = 'default'")
    suspend fun updateProfession(profession: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET bio = :bio, updatedAt = :timestamp WHERE id = 'default'")
    suspend fun updateBio(bio: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET hobbies = :hobbies, updatedAt = :timestamp WHERE id = 'default'")
    suspend fun updateHobbies(hobbies: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET customBehavior = :behavior, updatedAt = :timestamp WHERE id = 'default'")
    suspend fun updateCustomBehavior(behavior: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET customFAQ = :faq, updatedAt = :timestamp WHERE id = 'default'")
    suspend fun updateCustomFAQ(faq: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET availability = :availability, updatedAt = :timestamp WHERE id = 'default'")
    suspend fun updateAvailability(availability: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET age = :age, location = :location, updatedAt = :timestamp WHERE id = 'default'")
    suspend fun updateAgeAndLocation(age: Int, location: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE user_profile SET responseLength = :length, updatedAt = :timestamp WHERE id = 'default'")
    suspend fun updateResponseLength(length: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
}

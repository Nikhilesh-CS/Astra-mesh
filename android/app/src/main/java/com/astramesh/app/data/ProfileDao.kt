package com.astramesh.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertProfile(profile: ProfileEntity)

    @Query("SELECT * FROM profiles WHERE contactKey = :contactKey LIMIT 1")
    fun getProfileFlow(contactKey: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profiles WHERE contactKey = :contactKey LIMIT 1")
    fun getProfileSync(contactKey: String): ProfileEntity?
}

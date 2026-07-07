package com.astramesh.app.identity.profile

import com.astramesh.app.crypto.CryptoManager
import com.astramesh.app.data.AppDatabase
import com.astramesh.app.data.ProfileEntity
import com.astramesh.app.identity.IdentityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Orchestrates ProfileEntity database operations, IdentityManager updates, and caching.
 */
interface ProfileRepository {
    /**
     * Updates the local user's profile and synchronizes the name with IdentityManager.
     */
    suspend fun updateLocalProfile(profile: ProfileEntity, avatarData: ByteArray?, thumbnailData: ByteArray? = null)
    
    /**
     * Fetches a contact's profile as a Flow from the database.
     */
    fun getContactProfile(contactKey: String): Flow<ProfileEntity?>
    
    /**
     * Saves a profile received from another user over the mesh network.
     */
    suspend fun saveReceivedProfile(profile: ProfileEntity, avatarData: ByteArray?, thumbnailData: ByteArray? = null)
    
    /**
     * Helper to get the local profile synchronously.
     */
    suspend fun getLocalProfile(): ProfileEntity?
}

class ProfileRepositoryImpl(
    private val database: AppDatabase,
    private val identityManager: IdentityManager,
    private val cacheManager: ProfileCacheManager
) : ProfileRepository {

    // Access the ProfileDao added to AppDatabase
    private val profileDao = database.profileDao()

    override suspend fun updateLocalProfile(
        profile: ProfileEntity, 
        avatarData: ByteArray?, 
        thumbnailData: ByteArray?
    ) = withContext(Dispatchers.IO) {
        // Keep IdentityManager in sync with the new profile name
        identityManager.updateName(profile.name)
        
        // Save to Database
        profileDao.insertProfile(profile)
        
        // Save avatar if provided
        if (avatarData != null) {
            cacheManager.saveProfileAvatar(profile.contactKey, avatarData)
        }
        
        // Save thumbnail if provided
        if (thumbnailData != null) {
            cacheManager.saveThumbnail(profile.contactKey, thumbnailData)
        }
    }

    override fun getContactProfile(contactKey: String): Flow<ProfileEntity?> {
        // We assume ProfileDao has a getProfileFlow method returning Flow<ProfileEntity?>
        return profileDao.getProfileFlow(contactKey).flowOn(Dispatchers.IO)
    }

    override suspend fun saveReceivedProfile(
        profile: ProfileEntity, 
        avatarData: ByteArray?, 
        thumbnailData: ByteArray?
    ) = withContext(Dispatchers.IO) {
        profileDao.insertProfile(profile)
        
        if (avatarData != null) {
            cacheManager.saveProfileAvatar(profile.contactKey, avatarData)
        }
        
        if (thumbnailData != null) {
            cacheManager.saveThumbnail(profile.contactKey, thumbnailData)
        }
    }

    override suspend fun getLocalProfile(): ProfileEntity? = withContext(Dispatchers.IO) {
        val identity = identityManager.loadIdentity() ?: return@withContext null
        val contactKey = CryptoManager.toHex(identity.signingPublicKey)
        // We assume ProfileDao has a getProfileSync method returning ProfileEntity?
        profileDao.getProfileSync(contactKey)
    }
}

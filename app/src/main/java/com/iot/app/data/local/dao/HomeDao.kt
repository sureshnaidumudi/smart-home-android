package com.iot.app.data.local.dao

import androidx.room.*
import com.iot.app.data.local.entity.HomeEntity
import com.iot.app.data.local.relation.HomeWithRooms
import com.iot.app.data.local.relation.HomeWithRoomsAndDevices
import kotlinx.coroutines.flow.Flow

@Dao
interface HomeDao {
    @Query("SELECT * FROM homes")
    fun getAllHomes(): Flow<List<HomeEntity>>

    @Transaction
    @Query("SELECT * FROM homes")
    fun getHomesWithRooms(): Flow<List<HomeWithRooms>>

    @Transaction
    @Query("SELECT * FROM homes")
    fun getHomesWithRoomsAndDevices(): Flow<List<HomeWithRoomsAndDevices>>

    @Query("SELECT * FROM homes WHERE homeId = :homeId")
    suspend fun getHomeById(homeId: String): HomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHome(home: HomeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHomes(homes: List<HomeEntity>)

    @Delete
    suspend fun deleteHome(home: HomeEntity)

    @Query("DELETE FROM homes WHERE homeId = :homeId")
    suspend fun deleteHomeById(homeId: String)

    @Query("SELECT COUNT(*) FROM homes")
    suspend fun getHomeCount(): Int
}

package com.iot.app.data.local.dao

import androidx.room.*
import com.iot.app.data.local.entity.RoomEntity
import com.iot.app.data.local.relation.RoomWithDevices
import kotlinx.coroutines.flow.Flow

@Dao
interface RoomDao {
    @Query("SELECT * FROM rooms")
    fun getAllRooms(): Flow<List<RoomEntity>>

    @Query("SELECT * FROM rooms WHERE homeOwnerId = :homeId")
    fun getRoomsByHomeId(homeId: String): Flow<List<RoomEntity>>

    @Transaction
    @Query("SELECT * FROM rooms WHERE roomId = :roomId")
    fun getRoomWithDevices(roomId: String): Flow<RoomWithDevices?>

    @Query("SELECT * FROM rooms WHERE roomId = :roomId")
    suspend fun getRoomById(roomId: String): RoomEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoom(room: RoomEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRooms(rooms: List<RoomEntity>)

    @Delete
    suspend fun deleteRoom(room: RoomEntity)

    @Query("DELETE FROM rooms WHERE roomId = :roomId")
    suspend fun deleteRoomById(roomId: String)
}

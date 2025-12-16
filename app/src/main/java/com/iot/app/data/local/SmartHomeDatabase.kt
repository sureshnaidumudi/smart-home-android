package com.iot.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.iot.app.data.local.dao.DeviceDao
import com.iot.app.data.local.dao.HomeDao
import com.iot.app.data.local.dao.RoomDao
import com.iot.app.data.local.entity.DeviceEntity
import com.iot.app.data.local.entity.HomeEntity
import com.iot.app.data.local.entity.RoomEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

@Database(
    entities = [HomeEntity::class, RoomEntity::class, DeviceEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SmartHomeDatabase : RoomDatabase() {
    abstract fun homeDao(): HomeDao
    abstract fun roomDao(): RoomDao
    abstract fun deviceDao(): DeviceDao

    companion object {
        @Volatile
        private var INSTANCE: SmartHomeDatabase? = null

        fun getDatabase(context: Context): SmartHomeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartHomeDatabase::class.java,
                    "smart_home_database"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate database with default data on first creation
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    populateDefaultData(database)
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateDefaultData(database: SmartHomeDatabase) {
            val homeDao = database.homeDao()
            val roomDao = database.roomDao()
            val deviceDao = database.deviceDao()

            // Check if database is empty
            if (homeDao.getHomeCount() > 0) {
                return // Data already exists
            }

            // Create sample homes
            val home1Id = UUID.randomUUID().toString()
            val home2Id = UUID.randomUUID().toString()
            
            homeDao.insertHomes(
                listOf(
                    HomeEntity(homeId = home1Id, name = "Living Room Home"),
                    HomeEntity(homeId = home2Id, name = "Office Space")
                )
            )

            // Create sample rooms
            val room1Id = UUID.randomUUID().toString()
            val room2Id = UUID.randomUUID().toString()
            val room3Id = UUID.randomUUID().toString()
            val room4Id = UUID.randomUUID().toString()

            roomDao.insertRooms(
                listOf(
                    RoomEntity(roomId = room1Id, homeOwnerId = home1Id, name = "Living Room"),
                    RoomEntity(roomId = room2Id, homeOwnerId = home1Id, name = "Bedroom"),
                    RoomEntity(roomId = room3Id, homeOwnerId = home1Id, name = "Kitchen"),
                    RoomEntity(roomId = room4Id, homeOwnerId = home2Id, name = "Conference Room")
                )
            )

            // Create sample devices
            deviceDao.insertDevices(
                listOf(
                    DeviceEntity(
                        deviceId = UUID.randomUUID().toString(),
                        roomOwnerId = room1Id,
                        name = "Ceiling Light",
                        type = "BULB",
                        state = "ON",
                        stateValue = null,
                        isOnline = true
                    ),
                    DeviceEntity(
                        deviceId = UUID.randomUUID().toString(),
                        roomOwnerId = room1Id,
                        name = "Wall Fan",
                        type = "FAN",
                        state = "OFF",
                        stateValue = null,
                        isOnline = true
                    ),
                    DeviceEntity(
                        deviceId = UUID.randomUUID().toString(),
                        roomOwnerId = room1Id,
                        name = "Smart Socket",
                        type = "SOCKET",
                        state = "ON",
                        stateValue = null,
                        isOnline = true
                    ),
                    DeviceEntity(
                        deviceId = UUID.randomUUID().toString(),
                        roomOwnerId = room2Id,
                        name = "Bed Light",
                        type = "BULB",
                        state = "OFF",
                        stateValue = null,
                        isOnline = true
                    ),
                    DeviceEntity(
                        deviceId = UUID.randomUUID().toString(),
                        roomOwnerId = room2Id,
                        name = "AC Unit",
                        type = "AC",
                        state = "ON",
                        stateValue = null,
                        isOnline = true
                    ),
                    DeviceEntity(
                        deviceId = UUID.randomUUID().toString(),
                        roomOwnerId = room3Id,
                        name = "Temperature Sensor",
                        type = "SENSOR",
                        state = "VALUE",
                        stateValue = 22.5f,
                        isOnline = true
                    ),
                    DeviceEntity(
                        deviceId = UUID.randomUUID().toString(),
                        roomOwnerId = room4Id,
                        name = "Projector",
                        type = "SOCKET",
                        state = "OFF",
                        stateValue = null,
                        isOnline = true
                    )
                )
            )
        }
    }
}

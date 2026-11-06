package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MeshNodeEntity::class,
        MeshMessageEntity::class,
        EmergencyAlertEntity::class,
        RadioConfigEntity::class,
        PendingPacketEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SosMeshDatabase : RoomDatabase() {
    abstract fun meshDao(): MeshDao

    companion object {
        @Volatile
        private var INSTANCE: SosMeshDatabase? = null

        fun getInstance(context: Context): SosMeshDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SosMeshDatabase::class.java,
                    "sos_mesh.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

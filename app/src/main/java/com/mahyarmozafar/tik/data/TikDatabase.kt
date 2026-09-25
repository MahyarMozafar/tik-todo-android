package com.mahyarmozafar.tik.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ListEntity::class, TaskEntity::class, SubtaskEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TikDatabase : RoomDatabase() {
    abstract fun dao(): TikDao

    companion object {
        fun open(context: Context): TikDatabase =
            Room.databaseBuilder(context, TikDatabase::class.java, "tik.db").build()

        fun inMemory(context: Context): TikDatabase =
            Room.inMemoryDatabaseBuilder(context, TikDatabase::class.java).build()
    }
}

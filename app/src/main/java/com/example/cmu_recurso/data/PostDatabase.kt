package com.example.cmu_recurso.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Post::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class,StringListConverter::class)
abstract class PostDatabase : RoomDatabase() {
    abstract fun postDao() : PostDao

    companion object {
        @Volatile
        private var INSTANCE: PostDatabase? = null

        fun getDatabase(context: Context): PostDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context,
                    PostDatabase::class.java,
                    "post-database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
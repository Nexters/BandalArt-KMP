package com.nexters.bandalart.core.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import com.nexters.bandalart.core.database.entity.BandalartCellDBEntity
import com.nexters.bandalart.core.database.entity.BandalartDBEntity

@Database(
    entities = [
        BandalartDBEntity::class,
        BandalartCellDBEntity::class,
    ],
    version = 1,
)
@ConstructedBy(BandalartDatabaseConstructor::class)
abstract class BandalartDatabase : RoomDatabase() {
    abstract val bandalartDao: BandalartDao

    companion object {
        const val DB_NAME = "bandalart.db"
    }
}

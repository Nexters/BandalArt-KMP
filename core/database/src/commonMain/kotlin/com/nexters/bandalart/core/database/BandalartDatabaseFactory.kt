package com.nexters.bandalart.core.database

import androidx.room.RoomDatabase

expect class BandalartDatabaseFactory {
    fun create(): RoomDatabase.Builder<BandalartDatabase>
}

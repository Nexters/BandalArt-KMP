package com.nexters.bandalart.core.database

import androidx.room.RoomDatabaseConstructor

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object BandalartDatabaseConstructor : RoomDatabaseConstructor<BandalartDatabase> {
    override fun initialize(): BandalartDatabase
}

package com.nexters.bandalart.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// 반다라트 메인 테이블
@Entity(tableName = "bandalarts")
data class BandalartDBEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long? = null,

    @ColumnInfo(name = "mainColor")
    val mainColor: String = "#FF3FFFBA",

    @ColumnInfo(name = "subColor")
    val subColor: String = "#FF111827",

    @ColumnInfo(name = "profileEmoji")
    val profileEmoji: String? = null,

    @ColumnInfo(name = "title")
    val title: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "dueDate")
    val dueDate: String? = null,

    @ColumnInfo(name = "isCompleted")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "completionRatio")
    val completionRatio: Int = 0,
)

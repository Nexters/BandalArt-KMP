package com.nexters.bandalart.core.database.entity

data class BandalartWidgetSnapshotDto(
    val bandalart: BandalartDBEntity,
    val subGoal: BandalartCellDBEntity?,
    val tasks: List<BandalartCellDBEntity>,
)

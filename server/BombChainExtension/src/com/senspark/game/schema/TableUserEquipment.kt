package com.senspark.game.schema

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Table.Dual.integer

class TableUserEquipment(
    val name: String = "user_equipment",
    val equipmentId: Column<Int> = integer("equipment_id"),
    val equipmentType: Column<Int> = integer("equipment_type"),
    val heroId: Column<Int> = integer("hero_id"),
    val network: Column<Int> = integer("network"),
    val userId: Column<Int> = integer("user_id")
) : Table(name) {
    override val primaryKey = PrimaryKey(equipmentId, equipmentType)
}
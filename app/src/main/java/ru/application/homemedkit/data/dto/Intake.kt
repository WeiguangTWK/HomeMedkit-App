package ru.application.homemedkit.data.dto

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.PrimaryKey
import ru.application.homemedkit.helpers.BLANK
import ru.application.homemedkit.helpers.SchemaTypes

@Entity(
    tableName = "intakes",
    foreignKeys = [
        ForeignKey(
            entity = Medicine::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onUpdate = CASCADE,
            onDelete = CASCADE
        )
    ]
)
data class Intake(
    @PrimaryKey(autoGenerate = true)
    val intakeId: Long = 0L,
    val medicineId: Long = 0L,
    val interval: Int = 0,
    val foodType: Int = -1,
    val period: Int = 0,
    val startDate: String = BLANK,
    val finalDate: String = BLANK,
    val schemaType: SchemaTypes = SchemaTypes.BY_DAYS,
    val sameAmount: Boolean = true,
    val fullScreen: Boolean = false,
    val noSound: Boolean = false,
    val preAlarm: Boolean = false,
    val cancellable: Boolean = true
)
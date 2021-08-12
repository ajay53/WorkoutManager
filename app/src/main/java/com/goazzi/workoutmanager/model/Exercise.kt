package com.goazzi.workoutmanager.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tbl_exercise")
data class Exercise(
    @PrimaryKey(autoGenerate = false)
    val id: String,
    var timeStamp: Long,
    val exerciseName: String,
    val workoutId: String
)
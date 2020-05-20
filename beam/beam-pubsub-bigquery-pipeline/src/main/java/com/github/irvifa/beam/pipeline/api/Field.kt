package com.github.irvifa.beam.pipeline.api

import java.io.Serializable

data class Field(
    val name: String,
    val mode: FieldMode,
    val type: FieldType,
    val fields: List<Field>? = listOf()
) : Serializable

enum class FieldType {
    RECORD, INTEGER, FLOAT, BOOLEAN, STRING, NUMERIC, DATETIME, DATE, TIMESTAMP
}

enum class FieldMode {
    REPEATED, REQUIRED, NULLABLE
}

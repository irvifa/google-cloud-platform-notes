package com.github.irvifa.beam.pipeline.gcp

import com.github.irvifa.beam.pipeline.api.BigQuerySchema
import com.github.irvifa.beam.pipeline.api.FieldMode
import com.github.irvifa.beam.pipeline.api.FieldType
import com.github.irvifa.beam.pipeline.exceptions.DataTypeNotFoundException
import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableSchema
import com.google.cloud.bigquery.BigQueryOptions
import com.google.cloud.bigquery.Clustering
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import com.google.cloud.bigquery.TimePartitioning

class Bigquery(private val projectId: String, private val datasetName: String) {
    val bigquery = BigQueryOptions.newBuilder().setProjectId(projectId).build().service

    internal fun getBigQuerySchema(bigQuerySchema: BigQuerySchema): Schema {
        val fields: MutableList<Field> = mutableListOf()
        bigQuerySchema.fields.forEach { field
            ->
            fields.add(
                Field.of(field.name, convertDataTypeToStandardSql(field.type.name))
            )
        }
        return Schema.of(fields)
    }

    fun createFieldSchema(bigQuerySchema: BigQuerySchema): TableSchema {
        val fields: MutableList<TableFieldSchema> = mutableListOf()
        bigQuerySchema.fields.forEach { field ->
            val schemaField = TableFieldSchema()
            schemaField.name = field.name
            schemaField.type = field.type.name
            schemaField.mode = field.mode.name
            fields.add(schemaField)
        }
        return TableSchema().setFields(enrichWithDataId(fields))
    }

    private fun enrichWithDataId(fields: MutableList<TableFieldSchema>): MutableList<TableFieldSchema> {
        val dataIdFieldSchema = TableFieldSchema()
        dataIdFieldSchema.name = ID_COLUMN_NAME
        dataIdFieldSchema.type = FieldType.STRING.toString()
        dataIdFieldSchema.mode = FieldMode.NULLABLE.toString()
        fields.add(dataIdFieldSchema)
        return fields
    }

    private fun getClusteringColumnName(bigQuerySchema: BigQuerySchema): MutableList<String> {
        val clusteringColumns = mutableListOf<String>()
        bigQuerySchema.clusteringColumns.forEach { column -> clusteringColumns.add(column.name) }
        return clusteringColumns
    }

    internal fun convertDataTypeToStandardSql(targetType: String): LegacySQLTypeName {
        return when (targetType) {
            "STRING" -> LegacySQLTypeName.STRING
            "INTEGER" -> LegacySQLTypeName.INTEGER
            "TIMESTAMP" -> LegacySQLTypeName.TIMESTAMP
            "DATETIME" -> LegacySQLTypeName.DATETIME
            else -> throw DataTypeNotFoundException(targetType)
        }
    }

    internal fun getTableInfo(bigQuerySchema: BigQuerySchema): TableInfo {
        val tableId = TableId.of(datasetName, bigQuerySchema.tableName)
        val partitioning: TimePartitioning = TimePartitioning.of(TimePartitioning.Type.DAY)
        val schema = this.getBigQuerySchema(bigQuerySchema)
        val clusteringColumns = this.getClusteringColumnName(bigQuerySchema)
        val clustering: Clustering =
            Clustering.newBuilder().setFields(clusteringColumns).build()
        val tableDefinition = StandardTableDefinition.newBuilder()
            .setSchema(schema)
            .setTimePartitioning(partitioning)
            .setClustering(clustering)
            .build()
        return TableInfo.newBuilder(tableId, tableDefinition).build()
    }

    fun createTableIfNotExist(bigQuerySchema: BigQuerySchema) {
        val tableInfo = getTableInfo(bigQuerySchema)
        try {
            bigquery.create(tableInfo)
        } catch (e: Exception) {
            // noop
        }
    }

    companion object {
        private const val ID_COLUMN_NAME = "data_id"
    }
}

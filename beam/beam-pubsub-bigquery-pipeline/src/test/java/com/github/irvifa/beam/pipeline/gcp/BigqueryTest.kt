package com.github.irvifa.beam.pipeline.gcp

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.irvifa.beam.pipeline.api.FieldMode
import com.github.irvifa.beam.pipeline.api.PipelineConfig
import com.github.irvifa.beam.pipeline.exceptions.DataTypeNotFoundException
import com.google.api.services.bigquery.model.TableFieldSchema
import com.google.api.services.bigquery.model.TableSchema
import com.google.cloud.bigquery.Clustering
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.LegacySQLTypeName
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import com.google.cloud.bigquery.TimePartitioning
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.Before
import org.junit.Test

class BigqueryTest {
    private lateinit var underTest: Bigquery

    @Before
    internal fun before() {
        this.underTest = Bigquery(
            "test",
            "test_dataset"
        )
    }

    @Test
    fun `should be able to create Bigquery Schema`() {
        val expected = Schema.of(
            listOf(
                Field.of("cookie_id", LegacySQLTypeName.STRING),
                Field.of("name", LegacySQLTypeName.STRING)
            )
        )
        val bigquerySchema = loadPipelineConfigFromResource(configPath).bigquerySchema
        assertThat(this.underTest.getBigQuerySchema(bigquerySchema))
            .isEqualToComparingFieldByFieldRecursively(expected)
    }

    @Test
    fun `should be able to create field schema`() {
        val inputs = mapOf(
            "cookie_id" to "STRING",
            "name" to "STRING",
            "data_id" to "STRING"
        )
        val fields = mutableListOf<TableFieldSchema>()
        inputs.forEach { key, value ->
            val schemaField = TableFieldSchema()
            schemaField.name = key
            schemaField.type = value
            schemaField.mode = FieldMode.NULLABLE.name
            fields.add(schemaField)
        }
        val expected = TableSchema().setFields(fields)
        val bigquerySchema = loadPipelineConfigFromResource(configPath).bigquerySchema
        assertThat(this.underTest.createFieldSchema(bigquerySchema))
            .isEqualToComparingFieldByFieldRecursively(expected)
    }

    @Test
    fun `should be able to get table info`() {
        val expectedSchema = Schema.of(
            listOf(
                Field.of("cookie_id", LegacySQLTypeName.STRING),
                Field.of("name", LegacySQLTypeName.STRING)
            )
        )
        val expectedTableDefinition = StandardTableDefinition.newBuilder()
            .setSchema(expectedSchema)
            .setTimePartitioning(TimePartitioning.of(TimePartitioning.Type.DAY))
            .setClustering(Clustering.newBuilder().setFields(listOf("cookie_id")).build())
            .build()
        val expected =
            TableInfo.newBuilder(
                TableId.of("test_dataset", "test_clustered"),
                expectedTableDefinition
            )
                .build()
        val bigquerySchema = loadPipelineConfigFromResource(configPath).bigquerySchema
        assertThat(this.underTest.getTableInfo(bigquerySchema))
            .isEqualToComparingFieldByFieldRecursively(expected)
    }

    @Test
    fun `should be able to get type to standard sql`() {
        val inputs = listOf("STRING", "INTEGER", "TIMESTAMP", "DATETIME")
        val expected = listOf(
            LegacySQLTypeName.STRING,
            LegacySQLTypeName.INTEGER,
            LegacySQLTypeName.TIMESTAMP,
            LegacySQLTypeName.DATETIME
        )

        for (i in 0 until inputs.size) {
            val underTest = this.underTest.convertDataTypeToStandardSql(inputs[i])
            assertThat(underTest).isEqualTo(expected[i])
        }
    }

    @Test
    fun `should not be able to get type to standard sql throws exception`() {
        assertThrows(DataTypeNotFoundException::class.java) {
            this.underTest.convertDataTypeToStandardSql("RECORD")
        }
    }

    companion object {
        internal var configPath = "test_clustered.yaml"
        internal var parser = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
        internal fun loadPipelineConfigFromResource(url: String): PipelineConfig {
            val resource = Thread.currentThread().contextClassLoader.getResource(url)
            val pipelineConfig: PipelineConfig = parser.readValue(
                resource,
                object : TypeReference<PipelineConfig>() {})

            return pipelineConfig
        }
    }
}

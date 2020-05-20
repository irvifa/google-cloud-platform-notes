package com.github.irvifa.beam.pipeline.dofn

import com.github.irvifa.beam.pipeline.PipelineStringUtil
import com.github.irvifa.beam.pipeline.api.Field
import com.github.irvifa.beam.pipeline.api.FieldType
import com.github.irvifa.beam.pipeline.exceptions.DataTypeNotFoundException
import com.google.api.services.bigquery.model.TableRow
import org.apache.beam.sdk.transforms.DoFn
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.text.SimpleDateFormat

class JsonDatumProcessing(
    private val fields: List<Field>
) : DoFn<String, TableRow>() {
    private val logger = LoggerFactory.getLogger(JsonDatumProcessing::class.java)
    private val frontendNewTrackingJsonPubsubTimestampField = "publishTimestamp"
    private val originalTimestampPartitionField = "kafkaPublishTimestamp"
    private val fallbackTimestampPartitionField = "timestamp"
    private val dataIdField = "dataId"

    @ProcessElement
    fun processElement(c: ProcessContext) {
        val input = c.element()
        try {
            var rawData = JSONObject(input).toMap()
            rawData = enrichData(rawData, input)
            val data = mutableMapOf<String, Any?>()
            rawData.forEach { key, value ->
                val convertedKey = PipelineStringUtil.convertCamelToSnake(key)
                data[convertedKey] = value
            }
            val row = createTableRow(data)
            c.output(row)
        } catch (e: Exception) {
            logger.error("Error when processing $input")
        }
    }

    internal fun createTableRow(data: Map<String, Any?>): TableRow {
        val row = TableRow()
        fields.forEach { field ->
            when (field.type) {
                FieldType.INTEGER -> {
                    if (data.containsKey(field.name) && data[field.name] != null) {
                        val value: Long? = data[field.name].toString().toLongOrNull()
                        row.set(field.name, value)
                    }
                }
                FieldType.FLOAT -> {
                    if (data.containsKey(field.name) && data[field.name] != null) {
                        val value: Double? = data[field.name].toString().toDoubleOrNull()
                        row.set(field.name, value)
                    }
                }
                FieldType.BOOLEAN -> {
                    if (data.containsKey(field.name) && data[field.name] != null) {
                        val value: Boolean? = data[field.name].toString().toBoolean()
                        row.set(field.name, value)
                    }
                }
                FieldType.STRING -> {
                    if (data.containsKey(field.name) && data[field.name] != null) {
                        val value: String? = data[field.name].toString()
                        row.set(field.name, value)
                    }
                }
                FieldType.DATETIME -> {
                    if (data.containsKey(field.name) && data[field.name] != null) {
                        val value: Long? = data[field.name].toString().toLongOrNull()
                        val sf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                        row.set(field.name, sf.format(value))
                    }
                }
                else -> throw DataTypeNotFoundException(field.name)
            }
        }
        return row
    }

    private fun enrichData(
        data: MutableMap<String, Any?>,
        input: String
    ): MutableMap<String, Any?> {
        data[dataIdField] = PipelineStringUtil.getHashValueOf(input)
        return if (data.containsKey(frontendNewTrackingJsonPubsubTimestampField) ||
            data.containsKey(originalTimestampPartitionField)
        ) {
            data
        } else {
            val currentTimestamp = System.currentTimeMillis()
            if (data.containsKey(fallbackTimestampPartitionField)) {
                val value: Long? = data[fallbackTimestampPartitionField].toString().toLongOrNull()
                data[originalTimestampPartitionField] = value
            } else {
                data[originalTimestampPartitionField] = currentTimestamp
            }
            data
        }
    }
}

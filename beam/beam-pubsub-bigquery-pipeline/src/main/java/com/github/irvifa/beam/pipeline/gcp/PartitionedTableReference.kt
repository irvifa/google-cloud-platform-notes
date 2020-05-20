package com.github.irvifa.beam.pipeline.gcp

import com.google.api.services.bigquery.model.TableReference
import com.google.api.services.bigquery.model.TableRow
import org.apache.beam.sdk.io.gcp.bigquery.TableDestination
import org.apache.beam.sdk.transforms.SerializableFunction
import org.apache.beam.sdk.values.ValueInSingleWindow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

class PartitionedTableReference(
    private val projectId: String,
    private val datasetId: String,
    private val tableName: String
) : SerializableFunction<ValueInSingleWindow<TableRow>, TableDestination> {
    private val timeZone = TimeZone.getTimeZone("UTC")
    private val dateFormat = "yyyyMMdd"

    override fun apply(input: ValueInSingleWindow<TableRow>?): TableDestination {
        val tableReference = TableReference()
        tableReference.projectId = projectId
        tableReference.datasetId = datasetId
        tableReference.tableId = "$tableName$${this.getPartition()}"
        return TableDestination(tableReference, null)
    }

    internal fun getPartition(): String {
        val timeMillis = System.currentTimeMillis()
        return this.convertToDate(timeMillis)
    }

    private fun convertToDate(number: Long): String {
        val sdf = SimpleDateFormat(dateFormat)
        sdf.timeZone =
            timeZone
        return sdf.format(Date(number))
    }
}

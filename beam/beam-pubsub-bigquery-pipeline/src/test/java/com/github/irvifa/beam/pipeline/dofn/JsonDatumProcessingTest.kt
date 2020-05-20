package com.github.irvifa.beam.pipeline.dofn

import com.github.irvifa.beam.pipeline.gcp.BigqueryTest
import com.google.api.services.bigquery.model.TableRow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class JsonDatumProcessingTest {
    private lateinit var underTest: JsonDatumProcessing

    @Before
    internal fun before() {
        val fields = BigqueryTest.loadPipelineConfigFromResource(BigqueryTest.configPath)
            .bigquerySchema.fields
        this.underTest = JsonDatumProcessing(fields)
    }

    @Test
    fun `should be able to create TableRow`() {
        val data = mapOf("cookie_id" to "1234567", "name" to "nomunomuchua")
        val expected = TableRow()
        expected.set("cookie_id", "1234567")
        expected.set("name", "nomunomuchua")
        assertThat(this.underTest.createTableRow(data)).isEqualToComparingFieldByFieldRecursively(expected)
    }
}

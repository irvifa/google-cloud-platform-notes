package com.github.irvifa.beam.pipeline.gcp

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PartitionedTableReferenceTest {
    private lateinit var underTest: PartitionedTableReference

    @BeforeEach
    internal fun before() {
        this.underTest = PartitionedTableReference("nomu", "nomu", "chua")
    }

    @Test
    fun `should be able to create table destination`() {
        val timeZone = TimeZone.getTimeZone("UTC")
        val dateFormat = "yyyyMMdd"
        val sdf = SimpleDateFormat(dateFormat)
        sdf.timeZone = timeZone
        val expected = sdf.format(Date(System.currentTimeMillis()))
        assertThat(expected).isEqualTo(this.underTest.getPartition())
    }
}

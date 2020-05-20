package com.github.irvifa.beam.pipeline

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PipelineStringUtilTest {
    @Test
    fun `should be able to convert camel case to snake case`() {
        val expected = listOf(
            "aaa_bbb",
            "aaa_bbb_ccc",
            "normalcase",
            "!@#$%^&*()",
            "_id"
        )
        val inputs = listOf(
            "aaaBbb",
            "aaaBbbCcc",
            "normalcase",
            "!@#$%^&*()",
            "_id"
        )
        for (i in 0 until inputs.size) {
            val underTest = PipelineStringUtil.convertCamelToSnake(inputs[i])
            assertThat(underTest).isEqualTo(expected[i])
        }
    }

    @Test
    fun `should be able to generate digest`() {
        val input = "input"
        val underTest = PipelineStringUtil.getHashValueOf(input)

        assertThat(underTest).isEqualTo("c96c6d5be8d08a12e7b5cdc1b207fa6b2430974c86803d8891675e76fd992c20")
    }
}

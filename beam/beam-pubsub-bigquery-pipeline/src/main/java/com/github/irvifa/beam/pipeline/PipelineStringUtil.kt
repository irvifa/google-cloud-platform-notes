package com.github.irvifa.beam.pipeline

import java.security.MessageDigest

class PipelineStringUtil {
    companion object {
        fun getPubsubSubscription(topicName: String, projectId: String): String {
            return "projects/$projectId/subscriptions/$TOPIC_PREFIX.$topicName"
        }

        fun convertCamelToSnake(input: String): String {
            val re1 = Regex(UPPER_CASE_THEN_LOWER_CASE_REGEX)
            val re2 = Regex(LOWER_CASE_OR_DIGIT_THEN_UPPER_CASE_REGEX)
            var result = re1.replace(input, "$1_$2")
            result = re2.replace(result, "$1_$2")
            result = result.replace(".", "_").replace("-", "_").replace(" ", "_")

            while (result.contains(DOUBLE_UNDERSCORE))
                result = result.replace(DOUBLE_UNDERSCORE, "_")
            return result.toLowerCase()
        }

        fun getHashValueOf(input: String): String {
            val digest = MessageDigest.getInstance(DEFAULT_HASHING_ALGORITHM)
            val hashedBytes = digest.digest(input.toByteArray())
            val stringBuffer = StringBuffer()

            hashedBytes.forEach { byte ->
                var hash = byte.toInt()
                hash = hash.and(0xFF)
                hash += 0x100
                stringBuffer.append(Integer.toString(hash, DEFAULT_HEX_RADIX).substring(1))
            }
            return stringBuffer.toString()
        }

        private const val TOPIC_PREFIX = "test"
        private const val UPPER_CASE_THEN_LOWER_CASE_REGEX = "(.)([A-Z][a-z]+)"
        private const val LOWER_CASE_OR_DIGIT_THEN_UPPER_CASE_REGEX = "([a-z0-9])([A-Z])"
        private const val DOUBLE_UNDERSCORE = "__"
        private const val DEFAULT_HASHING_ALGORITHM = "SHA-256"
        private const val DEFAULT_HEX_RADIX = 16
    }
}

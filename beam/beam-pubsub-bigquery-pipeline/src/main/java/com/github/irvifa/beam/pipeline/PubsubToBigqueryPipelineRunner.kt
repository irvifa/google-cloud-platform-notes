package com.github.irvifa.beam.pipeline

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.github.irvifa.beam.pipeline.api.PipelineConfig
import com.github.irvifa.beam.pipeline.dofn.JsonDatumProcessing
import com.github.irvifa.beam.pipeline.gcp.Bigquery
import com.github.irvifa.beam.pipeline.gcp.PartitionedTableReference
import com.google.api.services.bigquery.model.TableRow
import org.apache.beam.sdk.Pipeline
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO
import org.apache.beam.sdk.io.gcp.bigquery.BigQueryIO.Write.WriteDisposition.WRITE_APPEND
import org.apache.beam.sdk.io.gcp.bigquery.InsertRetryPolicy
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO
import org.apache.beam.sdk.options.PipelineOptionsFactory
import org.apache.beam.sdk.transforms.ParDo
import org.apache.beam.sdk.values.PCollection
import org.apache.beam.vendor.grpc.v1p21p0.com.google.gson.Gson
import org.slf4j.LoggerFactory

class PubsubToBigqueryPipelineRunner {
    companion object {
        private val parser = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())
        private val gson = Gson()
        private val logger = LoggerFactory.getLogger(PubsubToBigqueryPipelineRunner::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            val configPath = this.getArgFromInputString(args[0])
            val projectId = this.getArgFromInputString(args[1])
            val resultProjectId = this.getArgFromInputString(args[2])
            val datasetName = this.getArgFromInputString(args[3])
            val pipelineConfig = loadPipelineConfigFromResource(configPath)
            PipelineOptionsFactory.register(PubsubToBigqueryPipelineConfig::class.java)
            val options: PubsubToBigqueryPipelineConfig =
                enrichOptions(
                    projectId,
                    pipelineConfig,
                    PipelineOptionsFactory.fromArgs(*args).withValidation().`as`(
                        PubsubToBigqueryPipelineConfig::class.java
                    ),
                    true
                )
            val pipeline = Pipeline.create(options)
            val tableName = pipelineConfig.bigquerySchema.tableName
            val nameSuffix: String = pipelineConfig.topicName + "->" + tableName
            val pubsubMessageString: PCollection<String> =
                pipeline.apply(
                    "Read-$nameSuffix",
                    PubsubIO.readStrings().fromSubscription(options.subscriptionName)
                )
            logger.info("Finished read data from pubsub with topic ${pipelineConfig.topicName}")

            val tableRowData: PCollection<TableRow> =
                pubsubMessageString.apply(
                    "Transform-$nameSuffix",
                    ParDo.of<String, TableRow>(
                        JsonDatumProcessing(
                            pipelineConfig.bigquerySchema.fields
                        )
                    )
                )
            logger.info("Finished transform data from pubsub with topic ${pipelineConfig.topicName}")

            val bigquery = Bigquery(resultProjectId, datasetName)
            bigquery.createTableIfNotExist(pipelineConfig.bigquerySchema)
            val partitionedTableReference = PartitionedTableReference(
                resultProjectId,
                datasetName,
                pipelineConfig.bigquerySchema.tableName
            )
            val schema = bigquery.createFieldSchema(pipelineConfig.bigquerySchema)
            tableRowData.apply(
                "Write-$nameSuffix",
                BigQueryIO.writeTableRows()
                    .to(partitionedTableReference)
                    .withSchema(schema)
                    .withWriteDisposition(WRITE_APPEND)
                    .withFailedInsertRetryPolicy(InsertRetryPolicy.retryTransientErrors())
            )
            logger.info("Finished transform data to bigquery with table name ${pipelineConfig.bigquerySchema.tableName}")
            pipeline.run()
        }

        private fun loadPipelineConfigFromResource(url: String): PipelineConfig {
            val resource = Thread.currentThread().contextClassLoader.getResource(url)
            val pipelineConfig: PipelineConfig = parser.readValue(
                resource,
                object : TypeReference<PipelineConfig>() {})

            return pipelineConfig
        }

        private fun enrichOptions(
            projectId: String,
            pipelineConfig: PipelineConfig,
            options: PubsubToBigqueryPipelineConfig,
            isStreaming: Boolean
        ): PubsubToBigqueryPipelineConfig {
            options.isStreaming = isStreaming
            options.projectId = projectId
            options.tempLocation = "gs://realtime-nrtprod-beam-tmp/tmp"
            options.topicName = pipelineConfig.topicName
            options.subscriptionName = PipelineStringUtil.getPubsubSubscription(
                pipelineConfig.topicName, projectId
            )
            return options
        }

        private fun getArgFromInputString(input: String): String {
            return input.split("=")[1]
        }
    }
}

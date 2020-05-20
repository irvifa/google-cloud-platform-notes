package com.github.irvifa.beam.pipeline.api

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable

data class PipelineConfig(
    @JsonProperty("topic_name") val topicName: String,
    @JsonProperty("bigquery_schema") val bigquerySchema: BigQuerySchema
) : Serializable

data class ClusteringColumn(val name: String) : Serializable

data class BigQuerySchema(
    @JsonProperty("table_name") val tableName: String,
    val fields: List<Field>,
    @JsonProperty("clustering_column_list") val clusteringColumns: List<ClusteringColumn>
) : Serializable

package com.github.irvifa.beam.pipeline

import org.apache.beam.sdk.options.PipelineOptions
import org.apache.beam.sdk.options.StreamingOptions

interface PubsubToBigqueryPipelineConfig : PipelineOptions, StreamingOptions {
    var configPath: String?

    var projectId: String?

    var resultProjectId: String?

    var pipelineJobName: String?

    var datasetName: String?

    var topicName: String?

    var subscriptionName: String?
}

### Getting Started With Beam

Beam is an open source programming model that can be used by both of batch or
stream processing. 

Inside GCP we can use `DataflowRunner` to run a beam pipeline. Move forward I'll mainly use Kotlin
for examples. 

In this case let's say I want to subscribe to a topic (realtime processing) and then put
the data to BigQuery, then I'll need to do the following steps:

- Define a config.
```aidl
interface PubsubToBigqueryPipelineConfig : PipelineOptions, StreamingOptions {
  var configPath: String?

  var projectId: String?

  var resultProjectId: String?

  var pipelineJobName: String?

  var datasetName: String?

  var topicName: String?

  var subscriptionName: String?
}
```
Note that the `Configuration` should implements both of `PipelineOptions` and `StreamingOptions`.

- Define a driver function, consisted of 3 components:
    - Subscribe from subscription
      ```aidl
      val pubsubMessageString: PCollection<String> =
          pipeline.apply(
              "Read-$nameSuffix",
              PubsubIO.readStrings().fromSubscription(options.subscriptionName)
          )
      ``` 
    - Transform the PubSub message into TableRow
    ```aidl
      val tableRowData: PCollection<TableRow> =
        pubsubMessageString.apply(
            "Transform-$nameSuffix",
            ParDo.of<String, TableRow>(
                JsonDatumProcessing(
                    pipelineConfig.bigquerySchema.fields
                )
            )
        ) 
    ```
    Here, we will need to do a `DoFn`:
    ```aidl
      class JsonDatumProcessing(
          private val fields: List<Field>
      ) : DoFn<String, TableRow>() {
          private val logger = LoggerFactory.getLogger(JsonDatumProcessing::class.java)
      
          @ProcessElement
          fun processElement(c: ProcessContext) {
              val input = c.element()
              try {
                  var data = JSONObject(input).toMap()
                  val row = createTableRow(data)
                  // createTableRow will return a TableRow
                  c.output(row)
              } catch (e: Exception) {
                  logger.error("Error when processing $input")
              }
          }
        }
    ```
    - Load the TableRow into BigQuery
    ```aidl
     tableRowData.apply(
         "Write-$nameSuffix",
         BigQueryIO.writeTableRows()
             .to(partitionedTableReference)
             .withSchema(schema)
             .withWriteDisposition(WRITE_APPEND)
             .withFailedInsertRetryPolicy(InsertRetryPolicy.retryTransientErrors())
     ) 
    ```
    Here, we will also need a `partitionTableReference`:
    ```aidl
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
    ```

Putting those three together we can get the following function:
```aidl
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
```
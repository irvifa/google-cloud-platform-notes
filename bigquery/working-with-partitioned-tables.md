### Working with Partitioned Tables

#### BigQuery CLI

The following is the example of query-ing using `bq` cli:

For example:

```aidl
gcloud config set project <YOUR-PROJECT>
bq query --allow_large_results --replace --nouse_legacy_sql --destination_table '<DATSET_NAME>.<TABLE_NAME>$<PARTITION>' '<YOUR_QUERY'
```

The query will in general create a new partition and replace it in any case it's being re-run.

##### Create a Partitioned Tables Based on SQL

You can save your query in certain directory and query-ing using your `bq` cli command.

#### Updating TTL of your _PARTITIONTIME 

```aidl
bq update --time_partitioning_expiration <TTL_IN_SECONDS> <PROJECT_ID>.<DATSET_NAME>.<TABLE_NAME>
```

### Exporting Stackdriver Logs to BigQuery in Multiple Projects

#### Background

You can export copies of some or all of your logs outside of [Stackdriver Logging](https://cloud.google.com/logging/). You might want to export logs for the following reasons:
- To store logs for extended periods. Logging typically holds logs for weeks, not years.
For more information, see the [Quota Policy](https://cloud.google.com/logging/quotas).
- To use big-data analysis tools on your logs.
- To stream your logs to other applications, other repositories, or third parties.

Exporting involves writing a **filter** that selects the log entries you want to export, and choosing a **destination** in [Cloud Storage](https://cloud.google.com/storage/), [BigQuery](https://cloud.google.com/bigquery/), or [Cloud Pub/Sub](https://cloud.google.com/pubsub/). The filter and destination are held in an object called a **sink**. Sinks can be created in projects, organizations, folders, and billing accounts.

There are no costs or limitations in Logging for exporting logs, but the export destinations charge for storing or transmitting the log data.

In this document, we will demonstrate the step by step of exporting the logs to BigQuery with multiple projects. Further documentation about Logs Exports can be read here:
- [Overview of Logs Exports](https://cloud.google.com/logging/docs/export/)
- [Exporting with the Logs Viewer](https://cloud.google.com/logging/docs/export/configure_export_v2)
- [Exporting Logs in the API](https://cloud.google.com/logging/docs/api/tasks/exporting-logs)
- [Aggregated Exports](https://cloud.google.com/logging/docs/export/aggregated_exports)
- [Using Exported Logs](https://cloud.google.com/logging/docs/export/using_exported_logs)
- [BigQuery Schema of Exported Logs](https://cloud.google.com/logging/docs/export/bigquery)

#### Steps

- Asking Permission

  From **Logging** -> **Exports** if you can't click the `CREATE EXPORT` button.

- Create BigQuery dataset
  Assumed that you have your own project (you have `owner` access in a project), follow steps explained in [this document](https://cloud.google.com/bigquery/docs/datasets#creating_a_dataset) to create the BigQuery dataset. 

- Create Export
  After clicking the `Export Button` then the windows will look like this
  - Customize the filter as preferred
  - Fill the `Sink Name`
  - Select `Sink Service` to `Custom destination`
  - Fill the `Sink Destination String` with format `bigquery.googleapis.com/projects/[PROJECT_ID]/datasets/[DATASET_ID]`

- Add the Newly Created Service Account to BigQuery
After clicking `Create Sink` you will be prompted with a mini-dialog like this
**Don't close it yet!** \
Copy the value and grant it a `Can Edit` access to your new dataset created in step 2

Then the log will automatically be inserted to the table with name `{your_application_name}_{timestamp in format YYYY-MM-DD}`

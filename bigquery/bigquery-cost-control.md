### BigQuery Cost Control

You probably know already that other than storage cost and insert cost, BigQuery also charges per query executed. The cost of a query was determined by the size of data that the query will need to access (currently USD 5/TB, rounded up to nearest MB, min. 10 MB per table, min. 10 MB per query). Therefore controlling the cost of a query essentially boils down to controlling the amount of data accessed.

Some idea on finding out whether your query makes sense is by translating it into dollar value.

#### Best Practices

1. Google already has quite an extensive documentation on how to control query cost in BigQuery Best Practices: Controlling Costs (i really recommend you also read this if you’re a frequent user). Some that i would like to highlight and add on:

    You might want to check the size of accessed data for your query. The easiest way to do this would be via BQ web UI.
    If you usually write BQ queries from a third party tool, simply go to https://bigquery.cloud.google.com/, select your project, copy-paste your query to the query text box, then click on the green checkmark on the right. You can see the amount of data that will be processed and determine if you need to make improvement or if the optimizations you’re making are working.

2. Avoid SELECT *. Data in BigQuery is stored in columnar fashion so that I can selectively access data in only columns that i need, and doing a SELECT * would defeat this purpose (unless you really do need all columns). 
Instead of SELECT *, list down only the columns you need. If you already have a huge query with many SELECT *, it can be hard see what columns are actually accessed.
One way to refactor this is by copy+pasting your query to BQ Web UI, remove the * (replacing it with SELECT "foo" or something) and add missing columns one by one as hinted by the error message.

3. If the table(s) you’re querying is partitioned, filter by _PARTITIONTIME whenever possible. 
Partitioning is a great way to improve performance and reduce cost, because you will be able to selectively read only partitions you need instead off full column scan. 
If you’re querying one of your most actively queried dataset, it’s already partitioned by insert time. An example of a query by _PARTITIONTIME

   You may want to pay attention to what the type of timestamp is a table partitioned by (insert time, or event time, or received-by-backend time, etc). Also note that _PARTITIONTIME is daily (rounded down, so the value is YYYY-MM-DDT00:00:00.000). If for example you need the past 24 hours of data, you might want to read data from the past two days (two partitions) instead of just one.

Some other not-very-obvious gotchas I found that will also affect query cost:

    Predicates are not pushed down through JOINs if you’re using legacy SQL (i am actually still not sure how they are pushed down even with standard SQL).

1. The predicates (WHERE clause) are not pushed down through JOINs,
therefore the query engine would read and join search and detail table first,
and then filter the resulting joined data by _PARTITIONTIME. 
To avoid this i recommend you use standard SQL instead 
(i am still looking on whether standard SQL JOIN predicate pushdown behavior actually minimize accessed data well or not), or push down the partition filter inside the JOINed subqueries instead of as a WHERE clause for the JOIN.
2. Predicates are not pushed down through FLATTENs or UNNESTs.
iIt might looked like the above query only processes data for partitions that correspond to the specified time range.
However the FLATTEN clause is actually executed first on all final_inv data,
before any filtering is considered/without pushing down the filters.
The resulting flattened data is filtered afterwards. So the size of data accessed is the same as if you are not filtering.
To combine FLATTEN and partitioning, the best practice would be to filter by partition first, and then FLATTEN the result.

    Reference to FLATTEN and UNNEST can be found [here](https://cloud.google.com/dataprep/docs/html/EXAMPLE---Flatten-and-Unnest-Transforms_57344993)


### Common Gotchas

If you're using airflow 1.10.1, you probably can't use the `clustering_fields` directly
on your BigQueryOperator. Another work around is using `subprocess` to call `bq` cli
and then pass your query to the `PythonOperator`. Consider you already have an existing
BQ table, you can do the following for airflow 1.10.3, however you can't do this for 1.10.1.

```aidl
task = BigQueryOperator(
          task_id=task_id,
          sql=sql,
          destination_dataset_table=destination_table_name,
          write_disposition='WRITE_TRUNCATE',
          use_legacy_sql=False,
          allow_large_result=True,
          bigquery_conn_id="google_cloud_default")
```
You'll get the following error:

```aidl
'Incompatible table partitioning specification. Expects partitioning specification interval(type:day) clustering(<csv of clustering_fields>), but input partitioning specification is interval(type:day)'}
```

A simple work around can be done using:

```aidl
from datetime import datetime, timedelta, date
import logging
import subprocess
from airflow import DAG
from airflow.models import Variable
from airflow.operators.python_operator import PythonOperator

logger = logging.getLogger(__name__)
class Constants:
  RESULT_TABLE_NAME = "{result_dataset_id}.{table_name}${partition}"
  BASH_COMMAND = "bq query --allow_large_results --replace --nouse_legacy_sql --destination_table '{destination_table}' '{query}'"

class Queries:
  QUERY = """SELECT <your fields> FROM `{project_id}.{dataset_id}.{table_name}` WHERE _PARTITIONTIME = CAST("{curr_date}" AS TIMESTAMP)"""

class Variables:
  DATASET_ID  = Variable.get('DATASET_ID')
  RESULT_PROJECT_ID = Variable.get('RESULT_PROJECT_ID')
  RESULT_DATASET_ID  = Variable.get('RESULT_DATASET_ID')
  TABLE_NAME = Variable.get('TABLE_NAME')
      
default_args = {
    "owner": "airflow",
    "depends_on_past": False,
    "start_date": datetime(2020, 2, 12),
    "email": [],
    "retries": 1,
}


def create_dag(dag_id, config, default_args):
    def run_query(project_id, dataset_id, result_dataset_id, sql, destination_table_name,  yesterday_ds, yesterday_ds_nodash, **context):
        query = sql.format(
            project_id=project_id,
            dataset_id=dataset_id,
            curr_date=yesterday_ds,
            table_name=Variables.TABLE_NAME)

        destination_table = destination_table_name.format(
            result_dataset_id=result_dataset_id,
            table_name=Variables.TABLE_NAME
            partition=yesterday_ds_nodash
        )

        command = Constants.BASH_COMMAND.format(destination_table=destination_table, query=query)
        try:
            output = subprocess.check_output(command, stderr=subprocess.STDOUT, shell=True, universal_newlines=True)
        except subprocess.CalledProcessError as exc:
            print("Current command:\n{}\n".format(command))
            raise

    start_date = datetime.strptime(config['startdatestr'], '%Y-%m-%d')

    dag = DAG(dag_id,
              default_args=default_args,
              schedule_interval=timedelta(days=1),
              start_date=start_date,
              catchup=False)

    with dag:
        task_id = "sample_task"
        task = PythonOperator(
            task_id=task_id,
            python_callable=run_query,
            op_kwargs={
                'project_id': Variables.PROJECT_ID,
                'dataset_id': Variables.DATASET_ID,
                'result_dataset_id': Variables.RESULT_DATASET_ID,
                'sql': Queries.QUERY,
                'destination_table_name': Variables.RESULT_TABLE_NAME
            },
            provide_context=True,
            dag=dag,
        )

    return dag
```

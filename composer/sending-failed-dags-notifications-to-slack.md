### Sending Dailed DAGs Notifications to Slack

First, you can add a new HTTP connections. Suppose you have incoming webhook
in the format of `https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX`,
then create the following connection:

```aidl
Host: https://hooks.slack.com/services
Conn Type: HTTP
Password: /T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX
```

You can create the following function:

```aidl
def failed_task_slack_alert(context):
    slack_webhook_token = BaseHook.get_connection(Variables.SLACK_CONN_ID).password

    base_log_url = context.get('task_instance').log_url
    
    slack_msg = """
            :red_circle: Task Failed. 
            *Task*: {task}  
            *Dag*: {dag} 
            *Execution Time*: {exec_date}  
            *Log Url*: {log_url} 
            """.format(
            task=context.get('task_instance').task_id,
            dag=context.get('task_instance').dag_id,
            ti=context.get('task_instance'),
            exec_date=context.get('execution_date'),
            log_url=base_log_url,
    )
    failed_alert = SlackWebhookOperator(
            task_id='slack_test',
            http_conn_id=Variables.SLACK_CONN_ID,
            webhook_token=slack_webhook_token,
            message=slack_msg,
            username='airflow')
    return failed_alert.execute(context=context)
```

And then you can use that function in your argument:

```aidl
default_args = {
    "owner": "airflow",
    "depends_on_past": False,
    "start_date": datetime(2019, 1, 1),
    "email": [],
    "email_on_failure": True,
    "email_on_retry": False,
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
    "on_failure_callback": failed_task_slack_alert,
}
```

So basically every time your DAGs failed you'll get notified.

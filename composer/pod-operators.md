### Pod Operator Examples

I find several difficulties when trying to get data from Google Drive, the problem is mainly in the term of defining correct scope for the credentials that we provided.
I’d like to thank my colleague [Chong Jie Lim](https://medium.com/@chongjie.lim) for helping me with this approach.
So here we’d like to share one of many way of doing the job using Airflow.
In this post, I’m using 3 different operator.

1. [KubernetesPodOperator](https://airflow.apache.org/docs/stable/kubernetes.html), used to download data from Google Drive, data transformation, and uploading data in the form of CSV to GCS.
2. [GoogleCloudStorageObjectSensor](https://airflow.apache.org/docs/stable/_modules/airflow/contrib/sensors/gcs_sensor.html), used to check if the file is already present in the GCS.
3. [GoogleCloudStorageToBigQueryOperator](https://airflow.apache.org/docs/stable/_modules/airflow/contrib/operators/gcs_to_bq.html), used to load data in the CSV form to Bigquery.

Before using KubernetesPodOperator, you should create a Dockerimage first.
Inside of the Dockerimage, we can define whatever things that need to be done as well as providing which kind of parameters that can be passed into the dockerized code.
Downloading data from Google Drive. Basically to be able to gain access, you can use a service account. 
Also we can only download a file based on its id, so if let’s say you only know the file name you can only download it after you know the id, that’s why we need to find it by performing the search query. 
To learn about this further, you can refer to Python quickstart.There’s basically two useful ways to retrieve file_id:

1. Based on file name if we know the folder_id
2. Based on queryThe following approach that we’re using is using the 2nd approach.

```
class Constants:
    SERVICE_ACCOUNT_PATH = 'GOOGLE_APPLICATION_CREDENTIALS'
    OAUTH_TOKEN_URI = '[https://www.googleapis.com/oauth2/v4/token'](https://www.googleapis.com/oauth2/v4/token')
class NoQueryResultsException(Exception):
    """No Files matching query in Google Drive"""
class GoogleSuiteApi:
    def __init__(self):
        self.service_account_credentials = self._service_account_credentials()
        self.service = self._service_build()
# [https://stackoverflow.com/questions/49663359/how-to-upload-file-to-google-drive-with-service-account-credential](https://stackoverflow.com/questions/49663359/how-to-upload-file-to-google-drive-with-service-account-credential)
    def _service_build(self):
        service = build('drive', 'v3', credentials=self.service_account_credentials)
        return service
[@staticmethod](http://twitter.com/staticmethod)
    def _service_account_credentials():
        service_account_key_path = os.getenv(
            Constants.SERVICE_ACCOUNT_PATH)
credentials = service_account.Credentials.from_service_account_file(
            service_account_key_path)
        scoped_credentials = credentials.with_scopes(
            [Constants.OAUTH_TOKEN_URI])
        signer_email = scoped_credentials.service_account_email
        signer = scoped_credentials.signer
credentials = google.oauth2.service_account.Credentials(
            signer,
            signer_email,
            token_uri=Constants.OAUTH_TOKEN_URI
        )
        return credentials
def get_metadata(self, folder_id):
        query = <whatever-your-query-is>
        try:
            get_files = self.service.files().list(q=query,
                          orderBy='modifiedTime desc',
                          pageSize=1).execute().get('files')
        except errors.HttpError as e:
            print ('An error occurred: %s' % e)
        if len(get_files) == 0:
            raise NoQueryResultsException(
                'No files in Google Drive matching query {query}'.format(query=query))
get_files.sort(key=lambda x: x['name'], reverse=True)
        print(get_files[0])
        return get_files[0]
def download_google_document_from_drive(self, file_id,
        export_mime_type=None):
        """
        file_id          :  Google Drive fileId
        export_mime_type :  To be provided if target file is a Google Drive native file.
                            Otherwise, leave it as None
        """
        try:
            if export_mime_type is None:
                request = self.service.files().get_media(fileId=file_id)
            else:
                request = self.service.files().export_media(fileId=file_id,
                                                            mimeType=export_mime_type)
            fh = io.BytesIO()
            downloader = MediaIoBaseDownload(fh, request)
            done = False
            while done is False:
                status, done = downloader.next_chunk()
                print('Download %d%%.' % int(status.progress() * 100))
            return fh
        except Exception as e:
            print('Error downloading file from Google Drive: %s' % e)

```

**Data Processing**

After you get the files you need, you can perform the processing by converting the xlsx data first to pandas and perform necessary join or any other operations.

```
import xlrd
import pandas as pd
def convert_xls_to_pandas(file_stream, sheet_name, column_mapping,
    column_types):
    workbook = xlrd.open_workbook(file_contents=file_stream.getvalue())
    df = pd.read_excel(workbook, sheet_name=sheet_name,
                       engine=xlrd)
    df = (df.loc[:, column_mapping.keys()]
          .copy()
          .rename(columns=column_mapping)
          .astype(column_types)
          )
    return df

```

Note that while converting to pandas, you also can define the column mapping for both of names and data types. After that you can convert pandas to csv.

```
def convert_pandas_to_csv(df, csv_name):
    df.to_csv(csv_name, index=False, header=True)
```

**Upload data to GCS**

```
import google.auth
from google.cloud import storage
def upload_file_to_gcs(bucket, input_filepath, output_filename):
    credentials, project = google.auth.default()
    if credentials.requires_scopes:
        credentials = credentials.with_scopes(
            ['https://www.googleapis.com/auth/devstorage.read_write'])
    client = storage.Client(credentials=credentials, project=project)

    try:
        try:
            bucket = client.get_bucket(bucket)
        except:
            bucket = client.create_bucket(bucket, project=project)
        blob = bucket.blob(output_filename)
        blob.upload_from_filename(input_filepath)
        print('Upload to GCS complete')
    except Exception as e:
        print('An error occurred: %s' % e)

    return None

```
To be able to gain access, you’ll need to set `GOOGLE_APPLICATION_CREDENTIALS` environment variable, as well as write necessary [scope](https://cloud.google.com/storage/docs/authentication).
After that you can create a Dockerimage with specific `ENTRYPOINT` that enable you to pass some arguments. 
Writing the Airflow ScriptNote that based on the documentation, defining a `Secret` will
automatically mount that secret in certain path that you define. However, instead of mounting the secret as a volume, you also can define it as an environment variable. For more information you can refer to this [example](https://github.com/GoogleCloudPlatform/python-docs-samples/blob/master/composer/workflows/kubernetes_pod_operator.py).

```
from airflow.contrib.operators.kubernetes_pod_operator import \
    KubernetesPodOperator
from airflow.contrib.kubernetes.secret import Secret
svc_acc = Secret('volume', '<path-to-mount-your-service-account>', '<secret-where-you-save-your-service-account>',
                         '<your-service-account-name>')
environments = {
    'GOOGLE_APPLICATION_CREDENTIALS': Constants.GOOGLE_APPLICATION_CREDENTIALS}
xlsx_processing = KubernetesPodOperator(
    task_id='<your-task-id-name>',
    image=<your-docker-image>,
    namespace='<your-namespace>',
    name='<your-k8s-job-name>',
    image_pull_policy='Always',
    is_delete_operator_pod=True,
    arguments=[
        <your-args>
    ],
    secrets=[svc_acc],
    env_vars=environments
)

```
You can set the downstream of your project to `GoogleCloudStorageObjectSensor` . And then set the downstream into `GoogleCloudStorageToBigQueryOperator` . Here I’ll give an example if you want to load your data based on given schema instead of `autodetect` .

```
gcs_to_bq_task = GoogleCloudStorageToBigQueryOperator(
    task_id=<your-task-name>,
    bucket=<your-bucket>,
    source_objects=[
        <your-file-path>
    ],
    destination_project_dataset_table=<your-table-name>,
    schema_fields=<your-schema>,
    skip_leading_rows=1, #skip the header
    source_format='CSV',
    write_disposition='WRITE_TRUNCATE',
    google_cloud_storage_conn_id=<your-connection>,
    bigquery_conn_id=<your-connection>,
    autodetect=False #This is because you're using your own schema
)

```
You can define your schema in the following format:

```
sample_schema = [
        {'name': '<column-name>', 'type': '<bq-data-type>', 'mode': '<mode, can be REQUIRED or NULLABLE>'},
]
```
For more explanation about available data type you can refer to this [page](https://cloud.google.com/bigquery/docs/reference/standard-sql/data-types). The most important thing is make sure that your Airflow have access to your container registry. Also if you define your own `ConfigMap` or `Secret` you can create that in your Airflow, if you run your Airflow on top of Kubernetes that also will be a lot easier. I think that’s all.. Hope it helps!


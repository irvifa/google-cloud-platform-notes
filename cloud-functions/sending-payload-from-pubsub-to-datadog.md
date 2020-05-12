### Sending Payload from PubSub to Datadog

- Provide a file for the configuration. In this example, I'm using `settings.json`
  ```java
  {
    "api_key": "",
    "app_key": "",
    "env": ""
  }
  ```
  You can obtain both of `api_key` and `app_key` from Datadog dashboard.
- Provide requirements needed for the cloud functions to be able to send data to datadog.
  ```java
  datadog==0.23.0
  requests==2.20.0
  ```
- Provide a handler:
  ```java
  from datadog import initialize, api
  import json
  import base64
  
  settings = json.loads(open('settings.json').read())
  def send_metrics_to_datadog(event, context):
      message = get_decoded_message(event['data'])
      print(message)
  
      options = {
          'api_key': settings['api_key'],
          'app_key': settings['app_key']
      }
  
      initialize(**options)
  
      return api.Metric.send(
          metric='example',
          points=1,
          type='count',
          tags=[
                  'env:' + settings['env'] # provide whatever tags you wants
              ]
      )
  
  def get_decoded_message(message):
      return json.loads(base64.b64decode(message))
  ```
- Deploy it:
  ```java
  gcloud beta functions deploy send_metrics_to_datadog --trigger-topic $(TOPICS) --runtime python37 --source=.
  ```
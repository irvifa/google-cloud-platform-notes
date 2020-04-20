### Log Entry

[LogEntry](https://cloud.google.com/logging/docs/reference/v2/rest/v2/LogEntry) will parse
the `SEVERITY` filter based on `severity` mentioned in the log json payload.

```aidl
{"severity":"INFO","message":"INFO  [2020-04-20 01:40:41.127] main - org.eclipse.jetty.server.handler.ContextHandler: Started i.d.j.MutableServletContextHandler@726a8729{/,null,AVAILABLE}"}
```

### Create a Filterable Log for Dropwizard Logging

Bellow is the example of re-formatting dropwizard log so that it can be filtered based on
its severity.
- [dropwizard-logging-stackdriver](https://github.com/irvifa/dropwizard-logging-stackdriver)

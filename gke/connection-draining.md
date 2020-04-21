### Connection Draining for Backend Service

When we set this, existing requests to the removed VM are given time to complete. 
See [this document](https://cloud.google.com/load-balancing/docs/enabling-connection-draining)
for more details about it.

#### Examples

For example the following connection draining will set the connection draining to 30 minutes. 
```aidl
kind: BackendConfig
metadata:
  annotations:
  name: <backend-config-name>
  namespace: <namespace>
spec:
  connectionDraining:
    drainingTimeoutSec: 1800
  iap:
    enabled: true
    oauthclientCredentials:
      secretName: <oauth-secret>
  timeoutSec: 1800
```

This can be used if your dashboard using a long computation, as can be seen for [R](deploying-r-applications/README.md)
as well.

## Kubernetes DNS

If let's say we have several multi cloud and want to connect VPC from GCP to another cloud provider we can use DNS forwarder to be able to access
machine from another VPC by its internal domain. This is the most general approach, compared to http-proxy approach.

We will need a DNS forwarder to make sure our instance in GCP VPC can access server in AWS VPC using their internal FQDN then we need to add stubDomains inside the kube-dns ConfigMap.

This is the example of kube-dns ConfigMap available for dev environment:

```aidl
apiVersion: v1
data:
  stubDomains: |
    {"your-domain": ["your-dns-forwarder-ip"]}
kind: ConfigMap
metadata:
labels:
    addonmanager.kubernetes.io/mode: EnsureExists
  name: kube-dns
  namespace: kube-system
```

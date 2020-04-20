### Setup Namespace and the RBAC


Background

TL;DR:

    RBAC let us define a set of role (hence permission) and bind it to certain accounts (human, service account, and in our context, it is related to GCP IAM User account).

    Currently we don't use custom RBAC service account for normal application pods. We only use the default one, so that the pods can bootstrap itself (pulling secret, cm, and other components needed by itself))

For more information about RBAC in relation with GCP: https://cloud.google.com/kubernetes-engine/docs/how-to/role-based-access-control

    Also please note that this RBAC Role assigned to your email address is not the same with GCP IAM Role assigned to your google email account. RBAC Role is scoped only to your access within the Kubernetes Cluster, not the GCP Project.

Role Design

There are 2 types of role binding:

    ClusterRoleBinding, it lets us define cluster-wide permissions role.:
        cluster-admin #predefined role
        <custom role we wants to use>

    RoleBinding, it lets us define namespace-only permission role.

Example of `ClusterRoleBinding` `ReadOnly`:

```
kind: ClusterRole
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: cluster-readonly
rules:
  - apiGroups: [""]
    resources:
      - componentstatuses
      - events
      - endpoints
      - namespaces
      - nodes
      - persistentvolumes
      - resourcequotas
      - services
    verbs: ["get", "watch", "list"]
  - nonResourceURLs: ["*"]
    verbs: ["get", "watch", "list"]
---
kind: ClusterRoleBinding
apiVersion: rbac.authorization.k8s.io/v1
metadata:
  name: cluster-readonly-binding
subjects:
  - kind: User
    name: "*"
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: ClusterRole
```

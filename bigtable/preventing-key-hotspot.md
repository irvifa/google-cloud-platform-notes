### Preventing Key Hotspots in Bigtable After Scale-Up

Bigtable is a sparse, distributed, persistent multidimensional sorted map indexed by row key, column key, and a timestamp.

**Performance and Scale Up**

During scale up, performance does not increase linearly due to the fact that caused by imbalance in load by multiple server configurations often caused by process contending for CPU and network. There’s two reasons why BT rebalancing algorithm needs more time to take actions:

- Rebalancing is throttled to reduce the number of tablet\* movements

- The load shift around as the benchmark progresses

You can use this tool to monitor your [cluster performance](https://cloud.google.com/bigtable/docs/monitoring-instance). You also can use [key visualizer tool](https://cloud.google.com/bigtable/docs/keyvis-getting-started) to identify hotspot in your table that might causing spikes in CPU utilization.

After you change the number of nodes in a cluster, it can take up to 20 minutes under load before you see a significant improvement in the cluster’s performance, usually depends on how you scale your cluster’s size.

- Bigtable maintains data in lexicographic order by row key. The row range for a table is dynamically partitioned. Each row range is called a **tablet, which is the unit of distribution and load balancing**.

---

Bigtable relied heavily on distributed lock service called Chubby which is using Paxos algorithm to keep the consistency of replica when there’s a failure. Chubby will provide namespace that contains directories and small files. Each directory orfile can be used as lock so that read write can be performed as atomic. The Chubby client library will provide consistent caching of Chubby files.

Bigtable uses Chubby for a variety of tasks:

- to ensure that there is at most one active master at any time;

- to store the bootstrap location of Bigtabledata;

- to discover tablet servers and finalize tablet server deaths;

- to store Bigtable schema information (the column family in formation for each table);

- and to store access control lists.

![Tablet Location Hierarchy similar to B+ Tree.](https://cdn-images-1.medium.com/max/1612/1*WkZRzwvUkWhIUTvh2uklSw.png)

**Tablet assignment**

Each tablet is assigned to one tablet server at a time. The master keeps track of the set of live tablet servers, and current assignment of tablets to tablet servers, including unassigned tablet. When a tablet unassigned, and a tablet server with sufficient room for the tablet is available, the master assign the tablet by sending tablet load request to tablet server. And Bigtable use Chubby to keep track of tablet servers. Each information about tablet servers will be stored in Chubby as a directory, including information on when it created, it status, and so on. The master will monitor this directory saved inside of Chubby.

---

After you scale up your cluster, you can do some load test to ensure the key spread across the cluster. Source code can be found in: [https://github.com/irvifa/bigtable-load-test](https://github.com/irvifa/bigtable-load-test).

```
./main -instance "$BT_INSTANCE" -project "$BT_PROJECT" -scratch_table "$BT_TABLE" -pool_size "$GRPC_POOL" -req_count "$REQ_COUNT" -run_for 0 -key_list "$KEY_LIST_FILE_PATH"
```

We can store the list of row keys in a file and pass it as argument.

In case you want to use a deployment to load test your Bigtable cluster, here’s the sample code of doing that:

- Build and push your Dockerimage:

```
**➜  bigtable-load-test** **git:(master)** cat Dockerfile

FROM golang:alpine AS builder

# Git is required for fetching the dependencies.

RUN apk update && apk add --no-cache git bash

COPY . .

RUN go get -d -v

RUN go build -ldflags="-w -s" -o /go/bin/main

CMD ["/bin/bash", "-c", "./run.sh"]
**➜  bigtable-load-test** **git:(master) **make docker_build \

IMAGE_NAME=<your-image-name> IMAGE_TAG=<your-image-tag>

**➜  bigtable-load-test** **git:(master) **make docker_push \

IMAGE_NAME=<your-image-name> IMAGE_TAG=<your-image-tag>
```

- ConfigMap:

```
apiVersion: v1
kind: ConfigMap
metadata:
  name: bt-load-test
  namespace: load-test
data:
  BT_INSTANCE: # your bigtable's instance id
  BT_TABLE: # your bigtable's table name
  BT_PROJECT: # your bigtable's project id
  GRPC_POOL: "10"
  REQ_COUNT: "2000"
```

- Deployment:

```
apiVersion: apps/v1*
*kind: Deployment
metadata:
  name: bt-load-test
  namespace: load-test
  labels:
    name: bt-load-test
    role: worker
spec:
  replicas: 15
  selector:
    matchLabels:
      name: bt-load-test
      role: worker
  template:
    metadata:
      labels:
        name: bt-load-test
        role: worker
    spec:
      containers:
        - name: bt-load-test
          image: # your bt-load-test image*
          *env:
            - name: "GOOGLE_APPLICATION_CREDENTIALS"
              value: # where is your service account's path
          envFrom:
            - configMapRef:
                name: bt-load-test
          volumeMounts:
            - name: "service-account"
              mountPath: # where you mount your service account
      volumes:
        - name: "service-account"
          secret:
            secretName: # where you save your service account
```

Another thing that need to be understood is reading the [Heatmap Patterns of your Key](https://cloud.google.com/bigtable/docs/keyvis-patterns).

**References:**

1. [Bigtable: A Distributed Storage System for Structured Data.](https://static.googleusercontent.com/media/research.google.com/en//archive/bigtable-osdi06.pdf)

1. [Monitoring a Cloud Bigtable Instance](https://cloud.google.com/bigtable/docs/monitoring-instance)

1. [Understanding Cloud Bigtable performance](https://cloud.google.com/bigtable/docs/performance)

1. [Heatmap Patterns](https://cloud.google.com/bigtable/docs/keyvis-patterns)

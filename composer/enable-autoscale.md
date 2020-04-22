### Enable Autoscaling in Composer

#### Node Autoscale

Since composer is working on top of GKE cluster we can perform it by autoscaling the Node.

```aidl
export COMPOSER_ENVIRONMENT_NAME=
export COMPOSER_REGION=
export PROJECT_ID=
export GKE_CLUSTER=$(gcloud composer environments describe \
${COMPOSER_ENVIRONMENT_NAME} \
--location ${COMPOSER_REGION} \
--format="value(config.gkeCluster)" \
--project ${PROJECT_ID} | \
grep -o '[^\/]*$')
```

If you want to scale the number of node you can do the following:
```aidl
export COMPOSER_ZONE=
export NODE_SIZE=
gcloud container clusters ${GKE_CLUSTER} \
  --zone ${COMPOSER_ZONE} \
  --node-pool default-pool \
  --num-nodes ${NODE_SIZE}
```

### Applying HPA

```aidl
export MAX_POD=
export MIN_POD=
export CPU_UTILIZATION=
kubectl -n ${NAMESPACE} autoscale deployment airflow-worker --cpu-percent=${CPU_UTILIZATION} --min=${MIN_POD} --max=${MAX_POD}
```

### Overriding Parellelism and Concurrency

```aidl
export COMPOSER_LOCATION=
gcloud composer environments update ${COMPOSER_ENVIRONMENT} \
--update-airflow-configs=core-max_active_runs_per_dag=150 \
--update-airflow-configs=core-dag_concurrency=300 \
--update-airflow-configs=core-dagbag_import_timeout=120 \
--update-airflow-configs=core-parallelism=300 \
--location ${COMPOSER_LOCATION} \
--project ${PROJECT_ID}
```

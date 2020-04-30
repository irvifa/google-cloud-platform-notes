### Cloudbuild Gotchas

- If you need specific image for google sdk instead of using `cloud-builders/gcloud`,
  it's better to use [`google/cloud-sdk`](https://hub.docker.com/r/google/cloud-sdk/).
  Since it will be updated more frequently. 
- Use the timeout field in a build step to set a time limit for executing the step.
  If you don't set this field, the step has no time limit and will be allowed
  to run until either it completes or the build itself times out.
  The timeout field in a build step must not exceed the timeout value specified for a build.
  See [build config](https://cloud.google.com/cloud-build/docs/build-config) for more details.
- For base image like Java, Gradle etc without any gcloud sdk requirements. It's better
  to use the base image.
- It's possible to put specific entry point for each steps, eg:
  ```aidl
    - name: 'gcr.io/cloud-builders/gcloud'
      id: 'deploy ingress and external service'
      entrypoint: 'bash'
      args:
        - '-c'
        - |
          gcloud container clusters get-credentials --project=${_PROJECT_ID} --zone=${_REGION} ${_CLUSTER_NAME}
          kubectl apply -f k8s --recursive
  ```
  Above command will re-force creating `ConfigMap` from a file.
 
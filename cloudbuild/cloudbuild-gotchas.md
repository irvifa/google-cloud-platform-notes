### Cloudbuild Gotchas

- If you need specific image for google sdk instead of using `cloud-builders/gcloud`,
  it's better to use [`google/cloud-sdk`](https://hub.docker.com/r/google/cloud-sdk/).
  Since it will be updated more frequently. 
- Use the timeout field for a build to specify the amount of time that the build must be allowed to run,
  to second granularity. If this time elapses, work on the build will cease and the build status will be TIMEOUT.
  If timeout is not set, a default timeout of 10 minutes will apply to the build.
  The maximum value that can be applied to timeout is 24 hours.
  Timeout must be specified in seconds with up to nine fractional digits,
  terminated by 's'. Example: "3.5s"
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
- It is possible to use secrets during build by using `Cloud KMS`, you 
  can see the details in [here](https://cloud.google.com/cloud-build/docs/securing-builds/use-encrypted-secrets-credentials#example_build_request_using_an_encrypted_variable).
  Example of using `KMS`:
  ```aidl
  echo <SOME-SECRET> | gcloud kms encrypt --ciphertext-file=- --plaintext-file=- --location=<LOCATION> --keyring=<KEYRING> --key=<KEY> --project=<PROJECT-ID> | base64
  ```
  The previous command can be used as an environment variable during build.
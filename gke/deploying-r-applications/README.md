### Deploying R Applications

#### Building R Docker Base Images

Building R Docker Base Image can be a problem because you'll need to install a lot of dependencies.
Considering you're using cloudbuild with certain timeout, it might get timeout before we finish building 
an image. So it's better to prepare your image first, before using it for your application.

The following sample will install all dependencies for R shiny and `bigrquery`:

```aidl
FROM rocker/shiny:3.5.1

RUN apt-get update && apt-get install -y libcurl4-openssl-dev libssl-dev
RUN R -e "install.packages(c('shiny', 'shinydashboard', 'shinyjs', 'bigrquery', 'gridExtra', 'stringr', 'dplyr', 'jsonlite', 'ggplot2', 'broom', 'tidyr'), repos='http://cran.rstudio.com/')"
```

After that you can re-use it for your apps:

Creating a shiny server config:

```aidl
# Instruct Shiny Server to run applications as the user "shiny"
run_as shiny;

# Define a server that listens on port 3838
server {
  listen 3838;

  # Define a location at the base URL
  location /<your-path>/ {
    site_dir /srv/shiny-server;

    # Log all Shiny output to files in this directory
    log_dir /var/log/shiny-server;

    sanitize_errors false;
    simple_scheduler 500;
  }
}
```

And then you can create your base image:

```aidl
FROM gcr.io/<PROJECT_ID>/shiny-base:<TAG>

COPY config/shiny-server.conf /etc/shiny-server/shiny-server.conf
COPY app.r /srv/shiny-server/

#USER shiny
```

This will run your application on port 3838 and expose it.

To be able to use environment variable, you can use `.Renviron`.

Note that usually R dashboard will be related to analytics, in any case if it needs longer
computation time we can define it inside of [`backend-config`](https://cloud.google.com/load-balancing/docs/backend-service#timeout-setting).

Thus the `backend config` can be applied to the internal load balancer.

```aidl
apiVersion: v1
kind: Service
metadata:
  name: <your-app-name> 
  namespace: <namespace>
  annotations:
    beta.cloud.google.com/backend-config: '{"default": "<your-backend-config-name>"}'
spec:
  ports:
    - port: 3838
      targetPort: 3838
  selector:
    app: <your-app-name> 
    environment: stg
  type: NodePort
```
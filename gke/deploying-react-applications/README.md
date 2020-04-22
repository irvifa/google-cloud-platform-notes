### Deploying React Applications

#### Connecting Frontend to Backend

You can use a `REACT_ENV` environment variable to change the `BASE_URL` to be able to connect 
FE and BE through the internal loadbalancer. To be able to use this, you can deploy `build` result 
of react inside of `nginx` server and add the config:

```aidl
server {
  listen 80;

  root   /usr/share/nginx/html;

  location / {
    try_files $uri /index.html;
  }

  location /api/ {
    proxy_pass "http://<service-name>.<namespace>/";
  }
}
```

Above configuration means that you will need to connect to internal service, though 
to call your api you need to add `/api` as the prefix.

More information you can see [this page about deploying HTTPS LB in GKE](https://cloud.google.com/load-balancing/docs/https).

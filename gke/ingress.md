### Ingress Configuration

There are various configuration that can be done for ingress template. In the template provided by sirens command line interface, the default template will be something like this:

```aidl
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: hello-world
  namespace: $namespace
spec:
  backend:
    serviceName: hello-world-global
    servicePort: 80
```

However, kindly notice that we can override servicePort value here if it's necessary. Also, you can also opt-out the spec.backend field if you want. Here's a possible modification of ingress.yaml file:

```aidl
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: kyc-id-quality
  namespace: $namespace
  annotations:
    kubernetes.io/ingress.global-static-ip-name: "<ip-static>"
    kubernetes.io/ingress.allow-http: "false"
spec:
  tls:
    - secretName: <tls-secret> 
  rules:
    # As per explanation from https://github.com/kubernetes/ingress-nginx/issues/797
    # for GCE we need to use * instead of fix path. Apparently, the path implementation
    # controller is different for every providers. There is still an ongoing discussion
    # regarding the "default" implementation in https: //github.com/kubernetes/ingress-nginx/issues/555
    - http:
        paths:
        - path: /prefix/*
          backend:
            serviceName: <service-name> 
            servicePort: <port-number>
```

Custom Ingress

We can define a custom ingress by defining periodSeconds and initialDelaySeconds for both of livenessProbe and readinessProbe. For example:

       ports:
       - containerPort: 8080
       livenessProbe:
           httpGet:
             path: /whoami
             port: 8080
           initialDelaySeconds: 10
           periodSeconds: 60
       readinessProbe:
         httpGet:
           path: /isReady
           port: 8080
         initialDelaySeconds: 10
         periodSeconds: 60

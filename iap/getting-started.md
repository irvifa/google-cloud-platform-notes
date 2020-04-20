### Using IAP Request Header

By default, each time we send request to our backend through IAP this value will be included to our request header:

```aidl
X-Goog-Authenticated-User-Email            accounts.google.com:example@gmail.com
```
So basically, we can get the current user’s email address by getting the email from this header, however it’s preferable to use the programmatic authentication using JWT signed header to ensure the authenticity of the request.

### Using Programmatic Authentication

For more information, please refer to this [page](https://cloud.google.com/iap/docs/authentication-howto).

### How to Setup IAP

**TL;DR**

IAP only works on top of LB7 belongs to google. This means that we should use ingress in order to be able to utilize this feature. Once we set the domain of our application, we can enable IAP through the IAP dashboard. To utilize IAP least privilege access principle we can request Google to whitelist your email access so that you can enjoy the alpha feature for IAP. This feature will allow you to set privilege based on ingress (domain) for each project instead of on the project level, that currently available on the beta feature of IAP. Please take a note that: only admin who wants to utilize this feature that need to ensure their email are whitelisted since it’s necessary for them to see the IAP’s ACL dashboard. The other member that will become the end user doesn’t necessary to ensure their email are whitelisted.

https://cloud.google.com/iap/docs/enabling-kubernetes-howto. 

### Troubleshooting

- Beware if you’re using react, by default you application will be using service worker, unless you choose to disable it. This will cause some problem related to token generation. By default, service worker will “literally” cache anything to your request since it will optimize the performance of your web application. There’s two possible way to avoid this problem:
- Using regex for certain endpoint, to ensure your application will be able to refresh the token properly
Disable your service worker in the react configuration

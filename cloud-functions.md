## Cloud Functions

### Securing Google Cloud Functions Using Service Account and How to Access it in Service-Service Communications

Google Cloud Functions provide a mechanism to secure each of your endpoint to permitted users only, namely remove `allUsers` and `allAuthenticatedUsers` in the IAM. For service to service communication you can use service account to generate token when you want to create a request to the Google Cloud Functions endpoint. The Google OAuth 2.0 system supports server-to-server interactions such as those between a web application and a Google service. For this scenario you need a service account, which is an account that belongs to your application instead of to an individual end user. Your application calls Google APIs on behalf of the service account, so users aren’t directly involved. After an application obtains an access token, it sends the token to a Google API in an [HTTP Authorization request header](https://developer.mozilla.org/docs/Web/HTTP/Headers/Authorization).

![Server to Server communication.](https://cdn-images-1.medium.com/max/650/1*eUMSytVOPLft36rVxLQSvA.png)

There are various client provided by [Google](https://developers.google.com/identity/protocols/OAuth2). In this case I’m using [Javascript](https://github.com/google/google-api-javascript-client). Before using your service account, make sure you already attached the role of `roles/cloudfunctions.invoker` in [Google Cloud Functions IAM](https://cloud.google.com/functions/docs/reference/iam/roles) for your service account. To get the email and key for JWT client, you can use your service account:

```
const fs = require('fs')

const SERVICE_ACCOUNT_PATH = process.env.GOOGLE_APPLICATION_CREDENTIALS

const rawServiceAccountData = fs.readFileSync(SERVICE_ACCOUNT_PATH)

const serviceAccount = JSON.parse(rawServiceAccountData)
```

And in the caller application, you can add the following code to get the token each time you want to create a request to your Cloud Functions endpoint.

```
const { google } = require('googleapis')

app.post('/<path>', function (req, res, next) {
 const ENDPOINT = <your-endpoint>
 const client = new google.auth.JWT({
   email: serviceAccount.client_email,
   key: serviceAccount.private_key,
 })
 client.fetchIdToken(ENDPOINT).then(idToken => {
    const options = {
     method: 'POST',
     uri: `${ENDPOINT}/<your-path>`,
     body: req.body,
     json: true
    }
   request(options).auth(null, null, true, idToken)
  .then(response => {
    res.send(response)
  })
  .catch (error => {
    console.log('[ERROR]', error.message)
    res.send("Not authorized.")
  })
 })
})
```

That’s all, hope it helps 🙂. Merci et à bientôt. 👋


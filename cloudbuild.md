## Cloudbuild

### Sending Build Status Notification From Google Container Builder to Github

This basically an attempt to get a status events from Google Container Builder back to Github, since [Google Container Builder don’t support build based on Pull Request, at least **Not Yet**](https://stackoverflow.com/questions/49020663/does-google-container-builder-support-building-pull-requests). Back then, a friend of mine, [Cakra Wardhana](https://medium.com/u/7a500d64b5c8), told me if it’ll good if we can get the build status from Container Builder back to Github based on commit SHA. He then explained a little bit about Github API V3, specifically the[ Status Event](https://developer.github.com/v3/repos/statuses/). I’ve create a simple Container Builder to Slack before with also his help. We can use the fact that a [Build resource](https://cloud.google.com/container-builder/docs/api/reference/rest/v1/projects.builds) was [sent to Google PubSub](https://cloud.google.com/container-builder/docs/send-build-notifications) each time Container Builder was triggered.

Based on that information, I’m using Google Cloud Function to get events from Google PubSub and sent the Status event back to Github.

```
const https = require("https")
// reference: [https://cloud.google.com/functions/docs/bestpractices/networking#http_requests_with_httphttps_packages](https://cloud.google.com/functions/docs/bestpractices/networking#http_requests_with_httphttps_packages)
const agent = new https.Agent({ keepAlive: true })
const octokit = require("[@octokit/rest](http://twitter.com/octokit/rest)")({ agent: agent })
const settings = require("./settings.json")

const whitelistedRepositories = [] // put your whitelisted repository in here

octokit.authenticate({
  type: "token",
  token: settings.accessToken
})

// Can be one of error, failure, pending, or success.
const statusMap = {
  QUEUED: "pending",
  WORKING: "pending",
  SUCCESS: "success",
  FAILURE: "failure",
  CANCELLED: "failure",
  TIMEOUT: "error",
  INTERNAL_ERROR: "error"
}

module.exports.sendContainerBuilderStatusToGithub = event => {
  const build = eventToBuild(event.data.data)

if (!build.tags.includes("status-check")) {
    return // only send status for build that has `status-check` tag.
  }

if (!statusMap[build.status]) {
    return // skip if the current status is not in the status list.
  }

=// format: github-{owner}-{repo}
  const cbRepoName = build.sourceProvenance.resolvedRepoSource.repoName

const repoName = cbRepoName.replace("github-{owner}-", "")

if (!whitelistedRepositories.includes(repoName)) {
    return // skip if not in in whitelisted repositories.
  }

const failureSteps = build.steps.filter(
    s => s.status && s.status === "FAILURE"
  )
  let description = ""
  if (failureSteps.length !== 0) {
    description = failureSteps[0].id
  }

const commitSha = build.sourceProvenance.resolvedRepoSource.commitSha

const request = {
    owner: "some-owner",
    repo: repoName,
    sha: commitSha,

state: statusMap[build.status],
    target_url: build.logUrl,
    description: description,
    context: "continuous-integration/container-builder"
  }

console.log("request: ", request)

return octokit.repos.createStatus(request)
}

// eventToBuild transforms pubsub event message to a build object.
const eventToBuild = data => {
  return JSON.parse(new Buffer.from(data, "base64").toString())
}
```

To authenticate the request to Github API I’m using [octonode](https://github.com/pksunkara/octonode), the authentication process are based on personal token.

References:

1. [Auto Assign Reviewers using Cloud Functions](https://cloud.google.com/community/tutorials/github-auto-assign-reviewers-cloud-functions)

2. [Sending Build Notifications](https://cloud.google.com/container-builder/docs/send-build-notifications)

3. [Github API V3: Statuses](https://developer.github.com/v3/repos/statuses/)

4. [Configuring Notifications for Third-Party Services](https://cloud.google.com/container-builder/docs/configure-third-party-notifications)

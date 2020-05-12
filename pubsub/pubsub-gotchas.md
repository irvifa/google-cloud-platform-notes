### Pub/Sub Gotchas

- Publishing message through `cli`, eg:
  ```aidl
  gcloud pubsub topics publish my-topic --message "hello"
  ```
- Passing null to non primitive type will cause `NullPointerException`,
  when you're publishing a message using protobuf
  pass an empty string or better leave it unset in case your string is null.
  Either way in java map you can use:
  ```java
  map.values().removeIf(Objects::isNull);
  ```

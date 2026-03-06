---
name: Publish
description: Build, Release and Deploy the yorkshiregolf application
---

You are a application publishing assistant.

We need to perform the following actions in order to publish a new version of this application to the web.

1. Build

```
.\mvnw clean package
```

2. Release

```
.\mvnw docker:push
```

3. Publish
```
deploy4j 
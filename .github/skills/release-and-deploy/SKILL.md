---
name: release-and-deploy
description: 'Release and deploy yorkshire-golf with Maven and deploy4j. Use when you need to set a release version, build and publish the Docker image, deploy a specific version, and then bump to the next snapshot.'
argument-hint: 'Release version to deploy, for example 0.0.10'
user-invocable: true
---

# Release And Deploy

## Outcome
Produce and deploy a tagged release version of this application, then return the repository to a next-snapshot development version.

## When To Use
- You are preparing a production release.
- You want the standard repository workflow from README.
- You need a repeatable sequence with validation checks.

## Inputs
- Confirmation that Java 21 and Maven are available.

## Procedure
1. Pre-checks
- Ensure working tree is in a safe state (commit or stash unrelated changes).
- Confirm toolchain: Java 21 and Maven wrapper available.
- Derive release version from `pom.xml` by removing the `-SNAPSHOT` suffix from the current project version.

2. Set release version
- Run Maven versions plugin to remove snapshot markers from the current project version:
```shell
./mvnw versions:set -DremoveSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false
```
- On Windows shells where needed, use `./mvnw.cmd`.
- Treat the resulting non-snapshot version as the release version for deploy.

3. Build and push release image
- Build and push Docker image:
```shell
./mvnw clean package docker:push
```
- Verify build succeeds before continuing.

4. Deploy the release version
- Deploy using deploy4j with the derived release version:
```shell
deploy4j deploy --version=<RELEASE_VERSION>
```
- Replace `<RELEASE_VERSION>` with the non-snapshot version created in step 2.

5. Bump to next snapshot
- Return project to next development snapshot:
```shell
./mvnw versions:set -DnextSnapshot=true -DprocessAllModules=true -DgenerateBackupPoms=false
```

## Completion Checks
- Release version was set without backup POMs generated.
- Build and docker push command completed successfully.
- deploy4j reported successful deployment for the chosen version.
- Project version was moved to next snapshot after deploy.

## Failure Handling
- If release version set fails: stop and fix POM/version configuration before proceeding.
- If build or docker push fails: stop, fix build/publish issue, and rerun from build step.
- If deploy fails: do not run next snapshot bump until deployment is resolved or intentionally aborted.
- If next snapshot bump fails: fix versioning state and rerun only the snapshot bump step.

## Notes
- Keep the derived non-snapshot version value consistent across build artifacts and deployment.
- Run commands from repository root.

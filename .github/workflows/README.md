# GitHub Actions Deployment Workflows

This directory contains GitHub Actions workflows for releasing and deploying the yorkshire-golf application using deploy4j.

## Workflow

### release-and-deploy.yml
This workflow uses JBang to run deploy4j directly from the command line.

**Features:**
- No pom.xml modifications needed
- Simpler setup
- Direct execution of deploy4j via JBang
- Easy to test locally with JBang

## Prerequisites

### Required GitHub Secrets

You need to configure the following secrets in your GitHub repository:

1. **DEPLOY4J_TOKEN**: Authentication token for deploy4j
   - Go to Settings → Secrets and variables → Actions
   - Add a new repository secret named `DEPLOY4J_TOKEN`
   - Get your token from https://deploy4j.dev/

2. **GITHUB_TOKEN**: Automatically provided by GitHub Actions (no setup needed)

### Local Testing with JBang

You can test deploy4j locally using JBang:

```bash
# Install JBang (if not already installed)
curl -Ls https://sh.jbang.dev | bash -s - app setup

# Run deploy4j
jbang deploy4j@deploy4j.dev deploy \
  --artifact=target/golf-tracker-0.0.1.jar \
  --target=staging \
  --app-name=yorkshire-golf
```

## Using the Workflows

### Triggering a Release and Deployment

1. Go to your repository on GitHub
2. Click on "Actions" tab
3. Select "Maven Release & Deploy via deploy4j" workflow
4. Click "Run workflow"
5. Fill in the required inputs:
   - **release_version**: The version number to release (e.g., `1.0.0`, without `-SNAPSHOT`)
   - **deploy_to**: Choose `staging` or `prod`
6. Click "Run workflow" to start the process

### What the Workflow Does

1. **Checkout code**: Clones the repository
2. **Set up JDK 21**: Configures Java environment
3. **Set release version**: Updates pom.xml with the release version and commits
4. **Build project**: Compiles and packages the application (runs tests)
5. **Tag release**: Creates a Git tag for the release and pushes it
6. **Deploy with deploy4j**: Uses JBang to run deploy4j
7. **Bump snapshot version**: Updates pom.xml to the next development version

## Workflow Configuration

### Customizing Deployment Targets

Edit the deploy4j command in the workflow file to match your deployment configuration:

```yaml
jbang deploy4j@deploy4j.dev deploy \
  --artifact=$ARTIFACT \
  --target=$DEPLOY_TARGET \
  --app-name=yorkshire-golf \
  --additional-params=value  # Add any additional deploy4j parameters
```

## Troubleshooting

### Deploy4j Not Found

If you get an error that deploy4j cannot be found:

1. Verify the JBang syntax: `jbang deploy4j@deploy4j.dev`
2. Check if deploy4j is available at the specified domain
3. Consider using an alternative deploy tool or updating the reference

### Authentication Failures

- Ensure `DEPLOY4J_TOKEN` secret is configured correctly
- Verify the token has the necessary permissions
- Check that the token hasn't expired

### Build Failures

- Verify all tests pass locally: `./mvnw clean install`
- Check Java version compatibility (requires Java 21)
- Review the GitHub Actions logs for specific error messages

## Migration from SSH Deployment

This workflow replaces SSH-based deployment with deploy4j. Key differences:

| Aspect | SSH Deployment | deploy4j |
|--------|----------------|----------|
| Authentication | SSH keys | API token |
| Setup | SSH key management, known_hosts | Single API token |
| Deployment | Manual scp/ssh commands | Automated via deploy4j |
| Service Management | Manual systemctl commands | Handled by deploy4j |
| Security | SSH key security | Token-based authentication |

## Additional Resources

- [deploy4j Documentation](https://deploy4j.dev/)
- [JBang Documentation](https://www.jbang.dev/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Maven Versions Plugin](https://www.mojohaus.org/versions-maven-plugin/)

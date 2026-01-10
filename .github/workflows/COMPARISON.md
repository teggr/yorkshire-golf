# Comparison: SSH vs deploy4j Deployment

This document compares the original SSH-based deployment workflow with the new deploy4j-based workflows.

## Key Differences

### 1. Authentication Method

**SSH Approach:**
```yaml
- name: Set up SSH
  run: |
    mkdir -p ~/.ssh
    echo "$SSH_PRIVATE_KEY" > ~/.ssh/id_ed25519
    chmod 600 ~/.ssh/id_ed25519
    ssh-keyscan -H your.server.com >> ~/.ssh/known_hosts
  env:
    SSH_PRIVATE_KEY: ${{ secrets.SSH_PRIVATE_KEY }}
```

**deploy4j Approach:**
```yaml
- name: Set up JBang
  uses: jbangdev/setup-jbang@v1

- name: Deploy artifact with deploy4j
  env:
    DEPLOY4J_TOKEN: ${{ secrets.DEPLOY4J_TOKEN }}
```

**Benefits:**
- Simpler authentication (single token vs SSH key management)
- No need to manage known_hosts
- Token-based security is easier to rotate and manage

### 2. Deployment Execution

**SSH Approach:**
```yaml
- name: Deploy artifact
  run: |
    ARTIFACT="target/myapp-${{ steps.set_version.outputs.release_version }}.jar"
    if [ "${{ github.event.inputs.deploy_to }}" == "staging" ]; then
      DEPLOY_PATH="/path/to/staging"
    else
      DEPLOY_PATH="/path/to/prod"
    fi
    scp $ARTIFACT user@your.server.com:$DEPLOY_PATH/
    ssh user@your.server.com "systemctl restart myapp.service || echo 'Service restart failed'"
```

**deploy4j Approach (JBang):**
```yaml
- name: Deploy artifact with deploy4j
  run: |
    ARTIFACT="target/golf-tracker-${{ steps.set_version.outputs.release_version }}.jar"
    if [ "${{ github.event.inputs.deploy_to }}" == "staging" ]; then
      DEPLOY_TARGET="staging"
    else
      DEPLOY_TARGET="prod"
    fi
    
    jbang deploy4j@deploy4j.dev deploy \
      --artifact=$ARTIFACT \
      --target=$DEPLOY_TARGET \
      --app-name=yorkshire-golf
```

**Benefits:**
- Automated service management (no manual systemctl commands)
- Consistent deployment process across environments
- Better error handling and rollback capabilities
- Deployment abstraction (don't need to know server paths/commands)

### 3. Java Version Update

**Original:** JDK 17
**Updated:** JDK 21 (matching project requirements)

### 4. GitHub Actions Versions

**Original:**
- `actions/checkout@v3`
- `actions/setup-java@v3`

**Updated:**
- `actions/checkout@v4`
- `actions/setup-java@v4`

### 5. Maven Wrapper

**Original:** Used `mvn` directly
**Updated:** Uses `./mvnw` for better reproducibility

### 6. Git Push for Version Bump

**Original:**
```yaml
git push origin main
```

**Updated:**
```yaml
git push origin ${{ github.ref_name }}
```
This is more flexible and works with any branch name.

## Configuration Requirements

### SSH Approach Required:
- `SSH_PRIVATE_KEY` secret
- Server hostname configuration
- Server user account setup
- SSH key authorization on server
- Known_hosts management

### deploy4j Approach Required:
- `DEPLOY4J_TOKEN` secret
- deploy4j account and configuration
- Application registration in deploy4j

## Workflow Options Provided

### Option 1: JBang Approach (Recommended)
- File: `release-and-deploy.yml`
- No pom.xml changes needed
- Uses `jbangdev/setup-jbang@v1` action
- Runs deploy4j directly with `jbang deploy4j@deploy4j.dev`

### Option 2: Maven Plugin Approach
- File: `release-and-deploy-maven.yml`
- Integrated with Maven lifecycle
- Requires pom.xml plugin configuration
- Good for Maven-centric teams

## Migration Steps

1. **Set up deploy4j account** at https://deploy4j.dev/
2. **Register your application** in deploy4j
3. **Add DEPLOY4J_TOKEN secret** to GitHub repository
4. **Choose workflow approach** (JBang or Maven plugin)
5. **Test with staging deployment** first
6. **Remove old SSH-based workflow** (if any)

## Testing Locally

### JBang Method:
```bash
# Install JBang
curl -Ls https://sh.jbang.dev | bash -s - app setup

# Test deployment
jbang deploy4j@deploy4j.dev deploy \
  --artifact=target/golf-tracker-0.0.1.jar \
  --target=staging \
  --app-name=yorkshire-golf
```

### Maven Plugin Method:
```bash
# Add plugin to pom.xml, then:
./mvnw dev.deploy4j:deploy4j-maven-plugin:deploy \
  -Ddeploy4j.target=staging \
  -Ddeploy4j.appName=yorkshire-golf
```

## Security Considerations

### SSH Approach:
- ✓ Well-established security model
- ✗ Complex key management
- ✗ Need to secure private keys
- ✗ Server access required

### deploy4j Approach:
- ✓ Token-based authentication
- ✓ Easier credential rotation
- ✓ No direct server access needed
- ✓ Centralized deployment management
- ⚠ Depends on deploy4j service availability

## Troubleshooting

See the [workflows README.md](.github/workflows/README.md) for detailed troubleshooting information.

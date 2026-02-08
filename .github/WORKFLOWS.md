# GitHub Actions Workflows

This project uses GitHub Actions for automated building, testing, and publishing.

## Workflows

### 1. Build and Test (`build.yml`)
**Trigger:** Push to `main` or `develop` branches, or pull requests to `main`

**Actions:**
- Builds the project with Maven
- Runs all tests
- Uploads build artifacts

### 2. Publish to GitHub Packages (`publish-github.yml`)
**Trigger:** When a GitHub release is created

**Actions:**
- Automatically publishes to GitHub Packages
- Uses built-in `GITHUB_TOKEN` (no setup needed)

**Usage:**
1. Create a new release on GitHub
2. Workflow runs automatically
3. Package is published to GitHub Packages

### 3. Publish to Maven Central (`publish-central.yml`)
**Trigger:** Manual workflow dispatch

**Actions:**
- Publishes to Maven Central/OSSRH
- Requires GPG signing

**Usage:**
1. Go to Actions tab → "Publish to Maven Central"
2. Click "Run workflow"
3. Enter version number
4. Click "Run workflow" button

## Required GitHub Secrets

For Maven Central publishing, add these secrets in GitHub Settings → Secrets and variables → Actions:

| Secret Name | Description | How to Get |
|-------------|-------------|------------|
| `OSSRH_USERNAME` | Sonatype JIRA username | Your Sonatype account |
| `OSSRH_PASSWORD` | Sonatype JIRA password | Your Sonatype account |
| `GPG_PRIVATE_KEY` | GPG private key | `gpg --armor --export-secret-keys YOUR_KEY_ID` |
| `GPG_PASSPHRASE` | GPG key passphrase | Your GPG passphrase |

### Exporting GPG Private Key
```bash
# List your keys
gpg --list-secret-keys --keyid-format=long

# Export private key (copy entire output including BEGIN/END lines)
gpg --armor --export-secret-keys YOUR_KEY_ID
```

## Local Development

Run the same checks locally:
```bash
# Build and test
mvn clean verify

# Publish to GitHub Packages
mvn clean deploy -Pgithub

# Publish to Maven Central
mvn clean deploy -Pcentral
```

## Workflow Status Badges

Add to README.md:
```markdown
![Build](https://github.com/boskyj/http-maven-plugin/workflows/Build%20and%20Test/badge.svg)
```

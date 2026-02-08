# HTTP Maven Plugin

A Maven plugin for making HTTP calls and extracting values from responses to set as Maven properties.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.httpmaven/http-maven-plugin.svg)](https://search.maven.org/artifact/io.github.httpmaven/http-maven-plugin)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Why Use This Plugin?

Integrate external APIs and services directly into your Maven build lifecycle:
- **Call REST APIs** from Maven builds
- **Fetch authentication tokens** from OAuth/API endpoints
- **Invoke HTTP endpoints** during build process
- Fetch version information from external services
- Validate deployments by calling health check endpoints
- Retrieve configuration values from remote sources
- Automate API testing within your build process
- Extract and use dynamic values in your build
- **Consume RESTful web services** in Maven
- **Make HTTP requests** with GET, POST, PUT, DELETE methods

## Features

- **REST API integration** - Call any REST endpoint from Maven
- **HTTP client for Maven** - Built-in HTTP client functionality
- Support for GET, POST, PUT, DELETE HTTP methods
- **OAuth token retrieval** - Fetch authentication tokens
- **API authentication** - Bearer tokens, API keys, custom headers
- JSONPath and regex pattern extraction
- Form data and JSON body support
- Retry mechanism with configurable delays
- Response file saving
- Comprehensive error handling
- **Web service invocation** during build

## Installation

Add the plugin to your `pom.xml`:

```xml
<plugin>
    <groupId>io.github.httpmaven</groupId>
    <artifactId>http-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <id>get-api-data</id>
            <phase>validate</phase>
            <goals>
                <goal>http-call</goal>
            </goals>
            <configuration>
                <url>https://api.github.com/repos/apache/maven</url>
                <method>GET</method>
                <jsonPaths>
                    <branch>$.default_branch</branch>
                    <stars>$.stargazers_count</stars>
                </jsonPaths>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## Configuration Parameters

| Parameter | Description | Default |
|-----------|-------------|---------|
| `url` | HTTP endpoint URL | Required |
| `method` | HTTP method | GET |
| `headers` | HTTP headers map | - |
| `body` | Request body | - |
| `formData` | Form data map | - |
| `jsonPaths` | JSONPath extraction map | - |
| `extractPatterns` | Regex pattern map | - |
| `timeout` | Request timeout (seconds) | 30 |
| `retryCount` | Number of retries | 0 |
| `retryDelay` | Delay between retries (ms) | 1000 |
| `responseFile` | Save response to file | - |
| `failOnError` | Fail build on HTTP errors | true |

## Common Use Cases

### Fetching Version Information
```xml
<execution>
    <id>get-latest-version</id>
    <phase>validate</phase>
    <goals>
        <goal>http-call</goal>
    </goals>
    <configuration>
        <url>https://api.example.com/version</url>
        <jsonPaths>
            <latest.version>$.version</latest.version>
        </jsonPaths>
    </configuration>
</execution>
```

### Health Check Validation
```xml
<execution>
    <id>verify-deployment</id>
    <phase>verify</phase>
    <goals>
        <goal>http-call</goal>
    </goals>
    <configuration>
        <url>https://myapp.example.com/health</url>
        <method>GET</method>
        <timeout>10</timeout>
        <retryCount>3</retryCount>
        <retryDelay>2000</retryDelay>
        <failOnError>true</failOnError>
    </configuration>
</execution>
```

### Authentication with Headers
```xml
<configuration>
    <url>https://api.example.com/data</url>
    <headers>
        <Authorization>Bearer ${api.token}</Authorization>
        <Content-Type>application/json</Content-Type>
    </headers>
</configuration>
```

### OAuth Token Retrieval
```xml
<execution>
    <id>get-oauth-token</id>
    <phase>initialize</phase>
    <goals>
        <goal>http-call</goal>
    </goals>
    <configuration>
        <url>https://auth.example.com/oauth/token</url>
        <method>POST</method>
        <formData>
            <grant_type>client_credentials</grant_type>
            <client_id>${oauth.client.id}</client_id>
            <client_secret>${oauth.client.secret}</client_secret>
        </formData>
        <jsonPaths>
            <access.token>$.access_token</access.token>
        </jsonPaths>
    </configuration>
</execution>
```

### REST API Call with JSON Response
```xml
<execution>
    <id>call-rest-api</id>
    <phase>validate</phase>
    <goals>
        <goal>http-call</goal>
    </goals>
    <configuration>
        <url>https://api.example.com/users/123</url>
        <method>GET</method>
        <headers>
            <Authorization>Bearer ${access.token}</Authorization>
        </headers>
        <jsonPaths>
            <user.name>$.name</user.name>
            <user.email>$.email</user.email>
        </jsonPaths>
    </configuration>
</execution>
```

## Examples

### JSONPath Extraction
```xml
<jsonPaths>
    <version>$.version</version>
    <name>$.name</name>
    <author>$.author.name</author>
</jsonPaths>
```

### Regex Pattern Extraction
```xml
<extractPatterns>
    <title>&lt;title&gt;([^&lt;]+)&lt;/title&gt;</title>
    <version>Version: ([0-9.]+)</version>
</extractPatterns>
```

### POST with Form Data
```xml
<method>POST</method>
<formData>
    <username>testuser</username>
    <password>secret</password>
</formData>
```

### POST with JSON Body
```xml
<method>POST</method>
<headers>
    <Content-Type>application/json</Content-Type>
</headers>
<body>
{
  "name": "${project.name}",
  "version": "${project.version}"
}
</body>
```

### Save Response to File
```xml
<configuration>
    <url>https://api.example.com/report</url>
    <responseFile>${project.build.directory}/api-response.json</responseFile>
</configuration>
```

### Multiple Extractions
```xml
<configuration>
    <url>https://api.github.com/repos/apache/maven</url>
    <jsonPaths>
        <repo.name>$.name</repo.name>
        <repo.stars>$.stargazers_count</repo.stars>
        <repo.forks>$.forks_count</repo.forks>
    </jsonPaths>
    <extractPatterns>
        <description>description&quot;:&quot;([^&quot;]+)</description>
    </extractPatterns>
</configuration>
```

## Accessing Extracted Properties

Extracted values are set as Maven properties and can be used throughout your build:

```xml
<echo>Repository: ${repo.name}</echo>
<echo>Stars: ${repo.stars}</echo>
```

Or in your Java code via resource filtering:
```properties
app.version=${latest.version}
app.stars=${repo.stars}
```

## Build Phases

Common phases to bind the plugin:
- `validate` - Early in the build, before compilation
- `initialize` - Very first phase
- `generate-resources` - Before resource processing
- `verify` - After integration tests
- `deploy` - During deployment

## Troubleshooting

### Connection Timeouts
Increase the timeout value:
```xml
<timeout>60</timeout>
```

### SSL Certificate Issues
Ensure your JVM trusts the certificate or configure your truststore.

### JSONPath Not Matching
Test your JSONPath expressions at [jsonpath.com](https://jsonpath.com). Remember to use `$` as the root.

### Build Fails on HTTP Errors
Set `failOnError` to false for non-critical calls:
```xml
<failOnError>false</failOnError>
```

## Requirements

- Maven 3.6.0 or higher
- Java 8 or higher

## Keywords

Maven HTTP plugin, REST API Maven, Maven REST client, HTTP client Maven plugin, Maven API call, invoke REST API Maven, Maven web service client, OAuth Maven plugin, Maven authentication token, HTTP request Maven, RESTful web service Maven, Maven HTTP GET POST, API integration Maven, Maven external API call

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions:
- Open an issue on GitHub
- Check existing issues for solutions

## License

Apache License 2.0
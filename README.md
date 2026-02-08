# HTTP Maven Plugin

A Maven plugin for making HTTP calls and extracting values from responses to set as Maven properties.

## Features

- Support for GET, POST, PUT, DELETE HTTP methods
- JSONPath and regex pattern extraction
- Form data and JSON body support
- Retry mechanism with configurable delays
- Response file saving
- Comprehensive error handling

## Usage

Add to your `pom.xml`:

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

## License

Apache License 2.0
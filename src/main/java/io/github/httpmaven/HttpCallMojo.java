package io.github.httpmaven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

/**
 * Maven plugin for making HTTP calls and extracting values from responses.
 * Supports GET, POST, PUT, DELETE methods with JSONPath and regex extraction.
 * 
 * @author boskyjoseph
 * @since 1.0.0
 */
@Mojo(name = "http-call")
public class HttpCallMojo extends AbstractMojo {
    
    /**
     * The Maven project instance.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;
    
    /**
     * The HTTP endpoint URL to call.
     */
    @Parameter(name = "url", required = true)
    private String url;
    
    /**
     * The HTTP method to use (GET, POST, PUT, DELETE).
     */
    @Parameter(name = "method", defaultValue = "GET")
    private String method;
    
    /**
     * HTTP headers to include in the request.
     */
    @Parameter(name = "headers")
    private Map<String, String> headers;
    
    /**
     * Form data to send as URL-encoded body.
     */
    @Parameter(name = "formData")
    private Map<String, String> formData;
    
    /**
     * Raw request body content.
     */
    @Parameter(name = "body")
    private String body;
    
    /**
     * Single regex pattern for value extraction.
     */
    @Parameter(name = "extractPattern")
    private String extractPattern;
    
    /**
     * Multiple regex patterns for value extraction.
     */
    @Parameter(name = "extractPatterns")
    private Map<String, String> extractPatterns;
    
    /**
     * Single JSONPath expression for value extraction.
     */
    @Parameter(name = "jsonPath")
    private String jsonPath;
    
    /**
     * Multiple JSONPath expressions for value extraction.
     */
    @Parameter(name = "jsonPaths")
    private Map<String, String> jsonPaths;
    
    /**
     * Single XPath expression for value extraction (future use).
     */
    @Parameter(name = "xpath")
    private String xpath;
    
    /**
     * Multiple XPath expressions for value extraction (future use).
     */
    @Parameter(name = "xpaths")
    private Map<String, String> xpaths;
    
    /**
     * Property name to store extracted value when using single extraction.
     */
    @Parameter(name = "outputProperty", defaultValue = "http.response")
    private String outputProperty;
    
    /**
     * Request timeout in seconds.
     */
    @Parameter(name = "timeout", defaultValue = "30")
    private int timeout;
    
    /**
     * Number of retry attempts on failure.
     */
    @Parameter(name = "retryCount", defaultValue = "0")
    private int retryCount;
    
    /**
     * Delay between retry attempts in milliseconds.
     */
    @Parameter(name = "retryDelay", defaultValue = "1000")
    private int retryDelay;
    
    /**
     * Skip execution if previous build step failed.
     */
    @Parameter(name = "skipOnFailure", defaultValue = "false")
    private boolean skipOnFailure;
    
    /**
     * File path to save HTTP response content.
     */
    @Parameter(name = "responseFile")
    private String responseFile;
    
    /**
     * Whether to fail the build on HTTP errors (4xx, 5xx status codes).
     */
    @Parameter(name = "failOnError", defaultValue = "true")
    private boolean failOnError;
            
    /**
     * Executes the HTTP call with retry logic and response processing.
     * 
     * @throws MojoExecutionException if HTTP call fails and failOnError is true
     * @throws MojoFailureException if plugin execution fails
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skipOnFailure && hasExecutionFailed()) {
            getLog().info("Skipping HTTP call due to previous failure");
            return;
        }
        
        getLog().info("Making HTTP " + method + " call to: " + url);
        
        Exception lastException = null;
        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                if (attempt > 0) {
                    getLog().info("Retry attempt " + attempt + "/" + retryCount);
                    Thread.sleep(retryDelay);
                }
                
                HttpRequest request = buildRequest();
                HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(timeout))
                    .build();
                    
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                handleResponse(response);
                return; // Success, exit retry loop
                
            } catch (IOException | InterruptedException e) {
                lastException = e;
                if (attempt < retryCount) {
                    getLog().warn("HTTP call failed, retrying: " + e.getMessage());
                } else {
                    String errorMsg = "HTTP call failed after " + (retryCount + 1) + " attempts: " + e.getMessage();
                    if (failOnError) {
                        throw new MojoExecutionException(errorMsg, e);
                    } else {
                        getLog().warn(errorMsg);
                    }
                }
            }
        }
    }
    
    /**
     * Builds the HTTP request with headers and body.
     * 
     * @return configured HttpRequest instance
     */
    private HttpRequest buildRequest() {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(timeout));
            
        if (headers != null) {
            headers.forEach(builder::setHeader);
        }
        
        HttpRequest.BodyPublisher bodyPublisher = getBodyPublisher();
        builder.method(method, bodyPublisher);
        
        return builder.build();
    }
    
    /**
     * Creates appropriate body publisher based on formData or body parameters.
     * 
     * @return HttpRequest.BodyPublisher for the request
     */
    private HttpRequest.BodyPublisher getBodyPublisher() {
        if (formData != null && !formData.isEmpty()) {
            String formBody = formData.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + 
                             "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
            return HttpRequest.BodyPublishers.ofString(formBody);
        }
        return body != null ? HttpRequest.BodyPublishers.ofString(body) : HttpRequest.BodyPublishers.noBody();
    }
    
    /**
     * Handles HTTP response, saves to file if configured, and extracts values.
     * 
     * @param response the HTTP response to process
     * @throws MojoExecutionException if response indicates error and failOnError is true
     */
    private void handleResponse(HttpResponse<String> response) throws MojoExecutionException {
        int statusCode = response.statusCode();
        getLog().info("HTTP response status: " + statusCode);
        
        if (statusCode >= 400 && failOnError) {
            throw new MojoExecutionException("HTTP request failed with status: " + statusCode);
        }
        
        String responseBody = response.body();
        
        // Save response to file if specified
        if (responseFile != null) {
            try {
                Files.write(Paths.get(responseFile), responseBody.getBytes(StandardCharsets.UTF_8));
                getLog().info("Response saved to: " + responseFile);
            } catch (IOException e) {
                getLog().warn("Failed to save response to file: " + e.getMessage());
            }
        }
        
        extractAndSetProperties(responseBody);
    }
    
    private boolean hasExecutionFailed() {
        return project.getProperties().containsKey("maven.execution.failed");
    }
    
    private void extractAndSetProperties(String responseBody) {
        if (jsonPaths != null && !jsonPaths.isEmpty()) {
            jsonPaths.forEach((key, path) -> extractJsonValue(responseBody, path, key));
        } else if (jsonPath != null) {
            extractJsonValue(responseBody, jsonPath, outputProperty);
        } else if (extractPatterns != null && !extractPatterns.isEmpty()) {
            extractPatterns.forEach((key, pattern) -> extractValue(responseBody, pattern, key));
        } else if (extractPattern != null) {
            extractValue(responseBody, extractPattern, outputProperty);
        }
    }
    
    private void extractJsonValue(String json, String path, String propertyName) {
        try {
            Object value = JsonPath.read(json, path);
            String stringValue = value != null ? value.toString() : "";
            project.getProperties().setProperty(propertyName, stringValue);
            getLog().info("Set property " + propertyName + " = " + stringValue);
        } catch (PathNotFoundException e) {
            getLog().warn("JSONPath '" + path + "' not found in response");
        } catch (Exception e) {
            getLog().error("Failed to extract JSON value with path '" + path + "': " + e.getMessage());
        }
    }
    
    private void extractValue(String text, String pattern, String propertyName) {
        try {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            if (matcher.find()) {
                String value = matcher.groupCount() > 0 ? matcher.group(1) : matcher.group(0);
                project.getProperties().setProperty(propertyName, value);
                getLog().info("Set property " + propertyName + " = " + value);
            } else {
                getLog().warn("Pattern '" + pattern + "' not found in response");
            }
        } catch (Exception e) {
            getLog().error("Failed to extract value with pattern '" + pattern + "': " + e.getMessage());
        }
    }
}
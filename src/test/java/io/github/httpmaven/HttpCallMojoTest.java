package io.github.httpmaven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HttpCallMojoTest {

    @Mock
    private MavenProject project;
    
    @Mock
    private Log log;
    
    private HttpCallMojo mojo;
    private Properties properties;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mojo = new HttpCallMojo();
        mojo.setLog(log);
        properties = new Properties();
        
        setField(mojo, "project", project);
        when(project.getProperties()).thenReturn(properties);
    }

    @Test
    public void testJsonPathExtraction() throws Exception {
        String json = "{\"name\":\"test\",\"version\":\"1.0\",\"nested\":{\"value\":\"nested-data\"}}";
        
        Map<String, String> jsonPaths = new HashMap<>();
        jsonPaths.put("app.name", "$.name");
        jsonPaths.put("app.version", "$.version");
        jsonPaths.put("nested.value", "$.nested.value");
        
        setField(mojo, "jsonPaths", jsonPaths);
        
        invokeMethod(mojo, "extractAndSetProperties", json);
        
        assertEquals("test", properties.getProperty("app.name"));
        assertEquals("1.0", properties.getProperty("app.version"));
        assertEquals("nested-data", properties.getProperty("nested.value"));
    }

    @Test
    public void testRegexPatternExtraction() throws Exception {
        String html = "<title>Test Page</title><h1>Welcome</h1>";
        
        Map<String, String> patterns = new HashMap<>();
        patterns.put("page.title", "<title>([^<]+)</title>");
        patterns.put("page.heading", "<h1>([^<]+)</h1>");
        
        setField(mojo, "extractPatterns", patterns);
        
        invokeMethod(mojo, "extractAndSetProperties", html);
        
        assertEquals("Test Page", properties.getProperty("page.title"));
        assertEquals("Welcome", properties.getProperty("page.heading"));
    }

    @Test
    public void testFormDataEncoding() throws Exception {
        Map<String, String> formData = new HashMap<>();
        formData.put("name", "test user");
        formData.put("email", "test@example.com");
        
        setField(mojo, "formData", formData);
        
        Object bodyPublisher = invokeMethod(mojo, "getBodyPublisher");
        assertNotNull(bodyPublisher);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invokeMethod(Object target, String methodName, Object... args) {
        try {
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
            java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            return method.invoke(target, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
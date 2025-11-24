package tn.supcom.cot.iam.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
@PreMatching
@Priority(1)
public class HttpMethodOverrideFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Check for X-HTTP-Method-Override header
        String methodOverride = requestContext.getHeaderString("X-HTTP-Method-Override");
        
        if (methodOverride != null && !methodOverride.isEmpty()) {
            // Override the HTTP method
            requestContext.setMethod(methodOverride.toUpperCase());
        }
        
        // Also check for form parameter _method (common in HTML forms)
        if ("POST".equalsIgnoreCase(requestContext.getMethod())) {
            // Note: Form parameters can't be easily read here without consuming the entity
            // So we'll rely on the X-HTTP-Method-Override header approach
        }
    }
}

package tn.supcom.cot.api.security;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.util.Optional;

/**
 * CORS (Cross-Origin Resource Sharing) Filter
 *
 * Handles CORS preflight requests and adds necessary headers
 * to allow frontend applications from different origins to access the API.
 *
 * Security Note: In production, configure specific allowed origins
 * in microprofile-config.properties instead of using "*"
 */
@WebFilter(filterName = "CorsFilter", urlPatterns = {"/api/*"})
public class CorsFilter implements Filter {

    @ConfigProperty(name = "cors.allowed.origins", defaultValue = "*")
    Optional<String> allowedOrigins;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Get the origin from the request
        String origin = request.getHeader("Origin");
        String allowedOriginsValue = allowedOrigins.orElse("*");

        // Set CORS headers
        if ("*".equals(allowedOriginsValue)) {
            // Allow all origins (development mode)
            response.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "*");
        } else {
            // Check if origin is in allowed list
            if (origin != null && allowedOriginsValue.contains(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
            }
        }

        // Allow credentials (cookies, authorization headers)
        response.setHeader("Access-Control-Allow-Credentials", "true");

        // Allowed HTTP methods
        response.setHeader("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");

        // Allowed headers
        response.setHeader("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization, cookie, x-requested-with, " +
                        "x-http-method-override, cache-control, pragma, expires");

        // Expose headers that client can read
        response.setHeader("Access-Control-Expose-Headers",
                "location, content-type, content-length, authorization");

        // Preflight cache duration (1 hour)
        response.setHeader("Access-Control-Max-Age", "3600");

        // Handle preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Continue with the request
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}
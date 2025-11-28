package tn.supcom.cot.api;

import jakarta.annotation.security.DeclareRoles;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.auth.LoginConfig;

/**
 * JAX-RS Application Configuration with MicroProfile JWT Security
 *
 * This class configures the REST API endpoints and security settings.
 * All endpoints are secured with JWT Bearer token authentication.
 *
 * Base Path: /api
 * Example: http://localhost:8080/cot-api/api/patients
 */
@ApplicationPath("/api")
@LoginConfig(authMethod = "MP-JWT", realmName = "cot-app-sec:iam")
@DeclareRoles({"DOCTOR", "FAMILY", "PATIENT", "SENSOR", "ADMIN"})
public class RestConfiguration extends Application {

    /**
     * No additional configuration needed.
     * JAX-RS will auto-discover all @Path annotated classes.
     * MicroProfile JWT will handle token validation automatically.
     */
}
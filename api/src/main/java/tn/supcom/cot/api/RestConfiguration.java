package tn.supcom.cot.api;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.auth.LoginConfig;

@ApplicationPath("/api")
@LoginConfig(authMethod = "MP-JWT", realmName = "cot-app-sec:iam")
public class RestConfiguration extends Application {
}
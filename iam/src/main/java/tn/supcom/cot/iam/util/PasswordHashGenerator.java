package tn.supcom.cot.iam.util;

import tn.supcom.cot.iam.security.Argon2Utility;

/**
 * Helper utility to generate Argon2 password hashes
 * Run this as a standalone program to hash passwords for database insertion
 * 
 * Usage:
 * 1. Compile and run this class
 * 2. Copy the generated hash
 * 3. Use it in your MongoDB Identity document
 */
public class PasswordHashGenerator {
    
    public static void main(String[] args) {
        // Test passwords to hash
        String[] passwords = {
            "Test123!",
            "Admin123!",
            "User123!"
        };
        
        System.out.println("=== Argon2 Password Hash Generator ===\n");
        
        for (String password : passwords) {
            try {
                String hash = Argon2Utility.hash(password.toCharArray());
                System.out.println("Password: " + password);
                System.out.println("Hash: " + hash);
                System.out.println();
                
                // Verify the hash works
                boolean valid = Argon2Utility.check(hash, password.toCharArray());
                System.out.println("Verification: " + (valid ? "✓ PASSED" : "✗ FAILED"));
                System.out.println("-----------------------------------\n");
            } catch (Exception e) {
                System.err.println("Error hashing password: " + e.getMessage());
            }
        }
        
        System.out.println("\nCopy the hash values above and use them in your MongoDB Identity documents.");
        System.out.println("Example MongoDB insert:");
        System.out.println("db.Identity.insertOne({");
        System.out.println("  \"_id\": \"user-001\",");
        System.out.println("  \"username\": \"testuser\",");
        System.out.println("  \"password\": \"<PASTE_HASH_HERE>\",");
        System.out.println("  \"roles\": 1,");
        System.out.println("  \"providedScopes\": \"resource.read resource.write\",");
        System.out.println("  \"version\": 0");
        System.out.println("})");
    }
}

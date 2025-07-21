package com.login.util;

import com.login.model.User;
import com.login.service.UserService;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility to test the login endpoint directly
 */
public class TestLoginEndpoint {
    
    public static void main(String[] args) {
        System.out.println("Starting login endpoint test...");
        
        try {
            // First verify that we can authenticate locally
            UserService userService = UserService.getInstance();
            User authTest = userService.authenticateUser("admin", "1234");
            
            if (authTest != null) {
                System.out.println("Local authentication test: SUCCESS");
                System.out.println("User: " + authTest.getUsername());
                System.out.println("Password: " + authTest.getPassword());
                System.out.println("Role: " + authTest.getRole());
                System.out.println("Active: " + authTest.isActive());
            } else {
                System.out.println("Local authentication test: FAILED");
            }
            
            // Now test the API endpoint
            URL url = new URL("http://localhost:8081/api/auth/login");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            String jsonInput = "{\"username\":\"admin\",\"password\":\"1234\"}";
            
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInput.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            int responseCode = conn.getResponseCode();
            System.out.println("API response code: " + responseCode);
            
            // Read the response
            try (BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    responseCode >= 200 && responseCode < 300 
                    ? conn.getInputStream() 
                    : conn.getErrorStream(), 
                    StandardCharsets.UTF_8))) {
                        
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                System.out.println("API response: " + response.toString());
            }
            
        } catch (Exception e) {
            System.err.println("Error testing login endpoint: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

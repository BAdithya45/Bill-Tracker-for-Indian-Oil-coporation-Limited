package com.login.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;

@Configuration
public class SessionConfig {

    @Bean
    public ServletContextInitializer servletContextInitializer() {
        return new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                // Configure session cookie settings
                SessionCookieConfig sessionCookieConfig = servletContext.getSessionCookieConfig();
                sessionCookieConfig.setName("JSESSIONID");
                sessionCookieConfig.setHttpOnly(true);
                sessionCookieConfig.setSecure(false); // false for development (http)
                sessionCookieConfig.setMaxAge(1800); // 30 minutes
                sessionCookieConfig.setPath("/");
                
                // Set session timeout (in minutes)
                servletContext.setSessionTimeout(30);
                
                System.out.println("DEBUG: Session configuration applied - timeout: 30 minutes, cookie: JSESSIONID");
            }
        };
    }
}

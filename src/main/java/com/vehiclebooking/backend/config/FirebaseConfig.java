package com.vehiclebooking.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            // Try to find file in resources (classpath) or explicit path
            // For now, defaulting to classpath/firebase-service-account.json
            // InputStream serviceAccount = null;
            // try {
            // serviceAccount = new
            // ClassPathResource("firebase-service-account.json").getInputStream();
            // } catch (Exception e) {
            // System.out.println(
            // "Firebase Service Account file not found in classpath. Firebase Auth will
            // fail unless configured.");
            // return null;
            // }

            // FirebaseOptions options = FirebaseOptions.builder()
            // .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            // .build();

            InputStream serviceAccount = null;
            try {
                serviceAccount = new ClassPathResource("firebase-service-account.json").getInputStream();
            } catch (Exception e) {
                System.out.println(
                        "Firebase Service Account file not found in classpath. Firebase Auth will" +
                                " fail unless configured.");
                return null;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            return FirebaseApp.initializeApp(options);
        }
        return FirebaseApp.getInstance();
    }
}

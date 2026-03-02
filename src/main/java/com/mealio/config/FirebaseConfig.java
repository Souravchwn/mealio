package com.mealio.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${mealio.firebase.service-account-path}")
    private Resource serviceAccountResource;

    @Value("${mealio.firebase.database-url}")
    private String databaseUrl;

    /**
     * Only initialised when mealio.firebase.enabled=true.
     * In dev profile this bean is skipped entirely — no Firebase needed locally.
     */
    @Bean
    @ConditionalOnProperty(name = "mealio.firebase.enabled", havingValue = "true")
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccountResource.getInputStream()))
                    .setDatabaseUrl(databaseUrl)
                    .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            log.info("Firebase initialized with DB URL: {}", databaseUrl);
            return app;
        }
        return FirebaseApp.getInstance();
    }
}

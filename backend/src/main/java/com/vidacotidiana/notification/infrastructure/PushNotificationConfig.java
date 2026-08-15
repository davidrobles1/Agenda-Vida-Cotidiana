package com.vidacotidiana.notification.infrastructure;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.vidacotidiana.notification.application.PushNotificationSender;
import com.vidacotidiana.notification.domain.DevicePushTokenRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Selects the PushNotificationSender adapter (ADR-007/DEC-010): FCM when
 * firebase.credentials-path is configured, the no-op adapter otherwise —
 * the same on/off pattern the project already uses for sharing.EmailSender,
 * except EmailSender has no "real" adapter yet (DEC-009 still TBD) while
 * this one does, gated purely by whether real Firebase credentials exist
 * in this environment.
 */
@Configuration
public class PushNotificationConfig {

    @Bean
    @ConditionalOnProperty(prefix = "firebase", name = "credentials-path")
    public PushNotificationSender fcmPushNotificationSender(
            org.springframework.core.env.Environment environment,
            DevicePushTokenRepository devicePushTokenRepository) throws IOException {
        String credentialsPath = environment.getRequiredProperty("firebase.credentials-path");
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(new FileInputStream(credentialsPath)))
                .build();
        FirebaseApp app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
        return new FcmPushNotificationSender(FirebaseMessaging.getInstance(app), devicePushTokenRepository);
    }

    @Bean
    @ConditionalOnMissingBean(PushNotificationSender.class)
    public PushNotificationSender noOpPushNotificationSender() {
        return new NoOpPushNotificationSender();
    }
}

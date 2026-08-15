package com.vidacotidiana.notification.infrastructure;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.vidacotidiana.notification.application.PushEvent;
import com.vidacotidiana.notification.application.PushEventType;
import com.vidacotidiana.notification.domain.DevicePlatform;
import com.vidacotidiana.notification.domain.DevicePushToken;
import com.vidacotidiana.notification.domain.DevicePushTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the real FCM adapter (BE-025). Per the task's own
 * instruction, this verifies ONLY that the payload/message is constructed
 * correctly against a mocked FirebaseMessaging client — real delivery to
 * Firebase's service is NOT exercised here (no real Firebase project is
 * available in this environment; see docs/development/02-validation-report.md).
 */
class FcmPushNotificationSenderTest {

    private FirebaseMessaging firebaseMessaging;
    private DevicePushTokenRepository devicePushTokenRepository;
    private FcmPushNotificationSender sender;

    private final UUID recipientUserId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        firebaseMessaging = Mockito.mock(FirebaseMessaging.class);
        devicePushTokenRepository = Mockito.mock(DevicePushTokenRepository.class);
        sender = new FcmPushNotificationSender(firebaseMessaging, devicePushTokenRepository);
    }

    @Test
    void sendBestEffort_buildsOneMessagePerDeviceWithTheCorrectToken() throws FirebaseMessagingException {
        DevicePushToken android = new DevicePushToken(recipientUserId, DevicePlatform.ANDROID, "android-token-1");
        DevicePushToken web = new DevicePushToken(recipientUserId, DevicePlatform.WEB, "web-token-1");
        when(devicePushTokenRepository.findByUserId(recipientUserId)).thenReturn(List.of(android, web));

        sender.sendBestEffort(recipientUserId, new PushEvent(PushEventType.INVITATION_RECEIVED, "You were invited."));

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(firebaseMessaging, times(2)).send(captor.capture());
        List<String> sentTokens = captor.getAllValues().stream().map(this::readTokenField).toList();
        assertThat(sentTokens).containsExactlyInAnyOrder("android-token-1", "web-token-1");
    }

    @Test
    void sendBestEffort_noDevicesRegistered_sendsNothing() throws FirebaseMessagingException {
        when(devicePushTokenRepository.findByUserId(recipientUserId)).thenReturn(List.of());

        sender.sendBestEffort(recipientUserId, new PushEvent(PushEventType.REMINDER_DELETED, "Deleted."));

        verify(firebaseMessaging, Mockito.never()).send(any(Message.class));
    }

    @Test
    void sendBestEffort_firebaseFailure_isCaughtAndNeverPropagates() throws FirebaseMessagingException {
        // AC-012: a push failure must never break the caller's main operation.
        DevicePushToken device = new DevicePushToken(recipientUserId, DevicePlatform.IOS, "ios-token-1");
        when(devicePushTokenRepository.findByUserId(recipientUserId)).thenReturn(List.of(device));
        when(firebaseMessaging.send(any(Message.class))).thenThrow(Mockito.mock(FirebaseMessagingException.class));

        assertThatCode(() -> sender.sendBestEffort(recipientUserId, new PushEvent(PushEventType.REMINDER_SHARE_REVOKED, "Revoked.")))
                .doesNotThrowAnyException();
    }

    private String readTokenField(Message message) {
        try {
            Field tokenField = Message.class.getDeclaredField("token");
            tokenField.setAccessible(true);
            return (String) tokenField.get(message);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}

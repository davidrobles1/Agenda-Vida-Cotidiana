package com.vidacotidiana.notification.application;

public record PushEvent(PushEventType type, String message) {
}

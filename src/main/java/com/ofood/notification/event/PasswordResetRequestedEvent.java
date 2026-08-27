package com.ofood.notification.event;

public record PasswordResetRequestedEvent(String email, String resetToken) {
}

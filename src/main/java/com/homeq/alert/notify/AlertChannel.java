package com.homeq.alert.notify;

/** One way of delivering an alert (email, CallMeBot WhatsApp, Twilio WhatsApp). */
public interface AlertChannel {
    /** Name used in {@code alert.channels}, e.g. "email". */
    String name();
    /** @throws RuntimeException if delivery failed. */
    void send(String subject, String body);
}

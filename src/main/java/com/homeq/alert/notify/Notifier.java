package com.homeq.alert.notify;

/** Sends one alert through every enabled channel. */
public interface Notifier {
    /** @throws RuntimeException if no channel delivered the alert; the listing is then retried next poll. */
    void send(String subject, String body);
}

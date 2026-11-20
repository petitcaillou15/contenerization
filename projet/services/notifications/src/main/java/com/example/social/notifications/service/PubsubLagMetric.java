package com.example.social.notifications.service;

import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

/** Porte la valeur du gauge notifications_pubsub_lag_seconds (spec §6). */
@Component
public class PubsubLagMetric {

    private final AtomicReference<Double> seconds = new AtomicReference<>(0.0);

    public PubsubLagMetric(MeterRegistry registry) {
        registry.gauge("notifications_pubsub_lag_seconds", seconds, AtomicReference::get);
    }

    public void set(double value) {
        seconds.set(Math.max(0.0, value));
    }
}

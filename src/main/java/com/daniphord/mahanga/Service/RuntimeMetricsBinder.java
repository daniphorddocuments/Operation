package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Repositories.EmergencyCallRepository;
import com.daniphord.mahanga.Repositories.IncidentRepository;
import com.daniphord.mahanga.Repositories.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class RuntimeMetricsBinder {

    private final MeterRegistry meterRegistry;
    private final UserRepository userRepository;
    private final IncidentRepository incidentRepository;
    private final EmergencyCallRepository emergencyCallRepository;

    public RuntimeMetricsBinder(
            MeterRegistry meterRegistry,
            UserRepository userRepository,
            IncidentRepository incidentRepository,
            EmergencyCallRepository emergencyCallRepository
    ) {
        this.meterRegistry = meterRegistry;
        this.userRepository = userRepository;
        this.incidentRepository = incidentRepository;
        this.emergencyCallRepository = emergencyCallRepository;
    }

    @PostConstruct
    void registerMeters() {
        Gauge.builder("froms.users.total", userRepository, repository -> repository.count())
                .description("Total registered FROMS user accounts")
                .register(meterRegistry);
        Gauge.builder("froms.incidents.total", incidentRepository, repository -> repository.count())
                .description("Total recorded incidents")
                .register(meterRegistry);
        Gauge.builder("froms.public_reports.total", emergencyCallRepository, repository -> repository.count())
                .description("Total public emergency reports")
                .register(meterRegistry);
        Counter.builder("froms.observability.bootstrap")
                .description("Counts successful observability bootstrap events")
                .register(meterRegistry)
                .increment();
    }
}

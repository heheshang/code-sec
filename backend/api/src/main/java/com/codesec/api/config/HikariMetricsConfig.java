package com.codesec.api.config;

import com.zaxxer.hikari.metrics.IMetricsTracker;
import com.zaxxer.hikari.metrics.MetricsTrackerFactory;
import com.zaxxer.hikari.metrics.PoolStats;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HikariMetricsConfig {

    @Bean
    public MetricsTrackerFactory metricsTrackerFactory(MeterRegistry meterRegistry) {
        MicrometerMetricsTrackerFactory delegate = new MicrometerMetricsTrackerFactory(meterRegistry);
        return new MetricsTrackerFactory() {
            @Override
            public IMetricsTracker create(String poolName, PoolStats poolStats) {
                IMetricsTracker inner = delegate.create(poolName, poolStats);
                return new IMetricsTracker() {
                    @Override
                    public void recordConnectionCreatedMillis(long createdMs) {
                        inner.recordConnectionCreatedMillis(createdMs);
                    }

                    @Override
                    public void recordConnectionAcquiredNanos(long acquiredNs) {
                        inner.recordConnectionAcquiredNanos(acquiredNs);
                    }

                    @Override
                    public void recordConnectionUsageMillis(long elapsedBorrowMs) {
                        if (elapsedBorrowMs >= 0) {
                            inner.recordConnectionUsageMillis(elapsedBorrowMs);
                        }
                    }

                    @Override
                    public void recordConnectionTimeout() {
                        inner.recordConnectionTimeout();
                    }

                    @Override
                    public void close() {
                        inner.close();
                    }
                };
            }
        };
    }
}
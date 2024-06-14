package dev.lapysh.cfg;

import com.hazelcast.core.HazelcastInstance;
import io.micronaut.context.event.ApplicationEventListener;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class HazelcastInitializer
    implements ApplicationEventListener<LiquibaseMigrationListener.LiquibaseMigrationCompletedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(HazelcastInitializer.class);

    private final HazelcastInstance hazelcastInstance;

    public HazelcastInitializer(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public void onApplicationEvent(LiquibaseMigrationListener.LiquibaseMigrationCompletedEvent event) {
        LOG.info("Initializing Hazelcast after Liquibase migrations...");
        // Touch map to start init process
        hazelcastInstance.getMap("playerDataMap");
        LOG.info("Hazelcast initialization completed.");
    }
}

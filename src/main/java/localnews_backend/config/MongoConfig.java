package localnews_backend.config;

import com.mongodb.ConnectionString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import jakarta.annotation.PostConstruct;

import java.util.Objects;

@Configuration
@EnableMongoAuditing
public class MongoConfig {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @PostConstruct
    public void logMongoTarget() {
        String resolvedMongoUri = Objects.requireNonNull(mongoUri, "spring.data.mongodb.uri must be configured").trim();
        if (resolvedMongoUri.isEmpty()) {
            throw new IllegalStateException("spring.data.mongodb.uri must be configured and non-empty");
        }

        ConnectionString connectionString = new ConnectionString(resolvedMongoUri);
        String hostSummary = String.join(",", connectionString.getHosts());
        String database = connectionString.getDatabase() != null ? connectionString.getDatabase() : "(default)";

        logger.info("MongoDB target resolved: hosts='{}', database='{}'", hostSummary, database);
    }
}

package com.skmcore.orderservice.actuator;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class OrderServiceHealthIndicator extends AbstractHealthIndicator {

    private final DataSource dataSource;

    public OrderServiceHealthIndicator(DataSource dataSource) {
        super("Order service health check failed");
        this.dataSource = dataSource;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders")) {

            rs.next();
            long totalOrders = rs.getLong(1);
            builder.up()
                    .withDetail("database", "Connected")
                    .withDetail("orders.total", totalOrders);
        }
    }
}

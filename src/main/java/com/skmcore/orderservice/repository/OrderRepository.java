package com.skmcore.orderservice.repository;

import com.skmcore.orderservice.model.Order;
import com.skmcore.orderservice.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByOrderNumber(String orderNumber);

    @EntityGraph(attributePaths = {"items", "customer"})
    @Query("SELECT o FROM Order o WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithItems(@Param("orderNumber") String orderNumber);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    Page<Order> findByCustomer_Id(UUID customerId, Pageable pageable);

    Page<Order> findByStatusAndCustomer_Id(OrderStatus status, UUID customerId, Pageable pageable);
}

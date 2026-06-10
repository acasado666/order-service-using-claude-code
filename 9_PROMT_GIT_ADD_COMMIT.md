

´´´text
Run `git add .` and `git commit -m "Initial commit"` to add all files and commit them with the message "Initial commit".
´´´

´´´text
git add -A
git commit -m "Project scaffold with Spring Boot 4.0.5, Java 21, Docker"
´´```

10 Before going further coding, we will push the code to GitHub.

git remote add origin https://github.com/sanjoykumarmalik/order-service-using-claude-code.git
git branch -M main
git push -u origin main


Step 11
Now we will focus on domain modeling and the database layer. Here, we will consider the following sub-areas:

Step 11.1 Create JPA Entities
Step 11.2 Create Liquibase migration
Step 11.3 Create repositories

´´´text
Create the following JPA entities in the model package:
1. Customer entity:
    - id (UUID, auto-generated)
    - email (unique, not null)
    - fullName (not null)
    - phone (nullable)
    - Auditing fields: createdAt, updatedAt (use @EntityListeners(AuditingEntityListener.class))
    - Enable JPA Auditing in a config class

2. Order entity:
    - id (UUID, auto-generated)
    - orderNumber (unique, generated as "ORD-" + 8 random alphanumeric chars)
    - status (enum: CREATED, CONFIRMED, SHIPPED, DELIVERED, CANCELLED) — store as STRING in DB
    - Add a method transitionTo(OrderStatus newStatus) that enforces valid state transitions:
      CREATED → CONFIRMED or CANCELLED
      CONFIRMED → SHIPPED or CANCELLED
      SHIPPED → DELIVERED
      Throw an IllegalStateException for invalid transitions.
    - ManyToOne relationship to Customer (not null, LAZY fetch)
    - OneToMany relationship to OrderItem (cascade ALL, orphanRemoval true)
    - totalAmount (BigDecimal, precision 12 scale 2) — computed from items
    - shippingAddress (embedded value object with street, city, state, zipCode, country)
    - Auditing fields: createdAt, updatedAt
    - Optimistic locking with @Version

3. OrderItem entity:
    - id (UUID, auto-generated)
    - ManyToOne relationship to Order
    - productId (String, not null)
    - productName (String, not null)
    - quantity (int, must be > 0)
    - unitPrice (BigDecimal, precision 10 scale 2)
    - subtotal (computed as quantity * unitPrice, stored in DB)

Use Lombok (@Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor, @Builder) on all entities.
Add proper equals/hashCode based only on the id field.


## Now we will push the newly created code to GitHub.

Run the below commands from Claude Code terminal.
´´´text
git add -A
git commit -m " JPA entities - Order, OrderItem, Customer with auditing and state machine"
git push
´´´

Step 11.2 — Create Liquibase Migration
We will create the Liquibase migration with the assistance of Claude Code. To do this, paste the prompt below on Claude Code terminal:
´´´text
Create a Liquibase migration file at src/main/resources/db/migration/V1__create_order_schema.sql that:
- Creates the customers table
- Creates the orders table with an index on order_number and customer_id
- Creates the order_items table with a foreign key to orders
- Uses UUID as the primary key type (PostgreSQL uuid type)
- Adds a CHECK constraint that quantity > 0
- Adds created_at and updated_at columns with default NOW()
´´´
  Step 11.3— Create repositories
  We will create the repositories with the assistance of Claude Code. To do this, paste the prompt below on Claude Code terminal:
MIO

´´´text
Create Spring Data JPA repositories for Customer, Order, and OrderItem entities. The repositories should
extend JpaRepository and include the following custom query methods:
- CustomerRepository:
        Optional<Customer> findByEmail(String email);
- OrderRepository:
        Optional<Order> findByOrderNumber(String orderNumber);
        List<Order> findByCustomerId(UUID customerId);
        List<Order> findByStatus(OrderStatus status, Pageable pageable);
        @Query("SELECT o.status, COUNT(o) FROM Order o WHERE o.customer.id = :customerId GROUP BY o.status")
        List<Object[]> countOrdersByStatusForCustomer(@Param("customerId") UUID customerId);
  - OrderItemRepository:
          List<OrderItem> findByOrderId(UUID orderId);
          List<OrderItem> findByProductId(String productId);

- Add @Repository annotation to each repository interface.
 - Use constructor injection for any custom implementations if needed.
 - Ensure that the repositories are properly tested with Spring Boot Test and an embedded database
 - Push the code to GitHub after creating the repositories.
 - Run the below commands from Claude Code terminal.
´´´

De Medium
´´´text
Create Spring Data JPA repositories for all three entities:
1. CustomerRepository:
    - findByEmail(String email) returning Optional<Customer>
    - existsByEmail(String email)
2. OrderRepository:
    - findByOrderNumber(String orderNumber) returning Optional<Order>
    - findByCustomerId(UUID customerId, Pageable pageable) returning Page<Order>
    - findByStatus(OrderStatus status, Pageable pageable) returning Page<Order>
    - A custom @Query that returns the count of orders per status for a given customer
3. OrderItemRepository:
    - findByOrderId(UUID orderId) returning List<OrderItem>
      Mark all repositories with @Repository.

´´´
Before proceeding to Step 12, we will push the newly created code to GitHub using the following commands.

´´´text
git add -A
git commit -m "Liquibase migration script and JPA repositories"
git push
´´´
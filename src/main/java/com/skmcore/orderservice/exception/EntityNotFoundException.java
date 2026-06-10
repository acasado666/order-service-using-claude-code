package com.skmcore.orderservice.exception;

public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entityName, String id) {
        super(entityName + " not found with id: " + id);
    }
}

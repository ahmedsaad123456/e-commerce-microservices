package com.example.order_service.exceptions;


import com.example.order_service.common.ErrorCode;

/*
* ResourceNotFoundException is a custom exception class that extends the AppException class.
* It is used to represent a specific type of error that occurs when a requested resource is not found in the application.
*
* */
public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resource, id);
    }
}
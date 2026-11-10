package com.example.customer.mapper;

import com.example.customer.dto.CustomerRequest;
import com.example.customer.dto.CustomerResponse;
import com.example.customer.entity.Customer;
import io.micrometer.common.util.StringUtils;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    Customer toCustomer(CustomerRequest request);

    CustomerResponse toCustomerResponse(Customer customer);

    // Update an existing Customer without overwriting fields with null values
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void mergeCustomer(@MappingTarget Customer customer, CustomerRequest request);

    // Skip blank or empty String values during mapping
    @Condition
    default boolean isNotBlank(String value) {
        return StringUtils.isNotBlank(value);
    }
}
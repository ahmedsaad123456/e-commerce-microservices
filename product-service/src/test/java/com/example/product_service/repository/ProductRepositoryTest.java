package com.example.product_service.repository;


import com.example.product_service.entity.Category;
import com.example.product_service.entity.Product;
import com.example.product_service.entity.StockMovement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    ProductRepository products;
    @Autowired CategoryRepository categories;
    @Autowired StockMovementRepository movements;

    private Category category;

    @BeforeEach
    void setUp() {
        movements.deleteAll();
        products.deleteAll();
        categories.deleteAll();
        category = categories.save(Category.builder().name("Test").build());
    }

    private Product product(int quantity) {
        return products.save(Product.builder().name("P").price(new BigDecimal("5.00"))
                .availableQuantity(quantity).category(category).build());
    }

    @Test
    void decreaseStock_updatesOnlyWhenEnough() {
        var p = product(5);

        assertThat(products.decreaseStock(p.getId(), 3)).isEqualTo(1);
        assertThat(products.decreaseStock(p.getId(), 3)).isZero();      // only 2 left
        assertThat(products.findAvailableQuantity(p.getId())).isEqualTo(2);
    }

    @Test
    void negativeQuantity_isRejectedByTheCheckConstraint() {
        assertThatThrownBy(() -> product(-1)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void samePurchaseAndProduct_isRejectedByTheUniqueConstraint() {
        var p = product(5);
        var purchaseId = UUID.randomUUID();
        movements.save(StockMovement.builder().purchaseId(purchaseId).productId(p.getId())
                .quantity(1).unitPrice(BigDecimal.ONE).build());

        assertThatThrownBy(() -> movements.saveAndFlush(StockMovement.builder().purchaseId(purchaseId)
                .productId(p.getId()).quantity(1).unitPrice(BigDecimal.ONE).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
package com.example.product_service.service;
import com.example.product_service.dto.PurchaseItem;
import com.example.product_service.dto.PurchaseRequest;
import com.example.product_service.entity.Product;
import com.example.product_service.entity.StockMovement;
import com.example.product_service.exceptions.AppException;
import com.example.product_service.mapper.ProductMapper;
import com.example.product_service.repository.CategoryRepository;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository products;
    @Mock
    CategoryRepository categories;
    @Mock
    StockMovementRepository movements;
    @Mock
    ProductMapper mapper;
    @Mock
    TransactionTemplate tx;
    @InjectMocks ProductService service;

    private final UUID purchaseId = UUID.randomUUID();
    private final Product keyboard = Product.builder()
            .id(1).name("Keyboard").price(new BigDecimal("99.99")).availableQuantity(10).build();

    @BeforeEach
    void passThroughTransaction() {
        // run the callback immediately instead of opening a real transaction
        lenient().when(tx.execute(any())).thenAnswer(inv ->
                ((TransactionCallback<?>) inv.getArgument(0)).doInTransaction(null));
    }

    private PurchaseRequest request(int quantity) {
        return new PurchaseRequest(purchaseId, List.of(new PurchaseItem(1, quantity)));
    }


    @Test
    void purchase_notEnoughStock_throwsWithProductName() {
        when(movements.existsByPurchaseId(purchaseId)).thenReturn(false);
        when(products.findAllById(any())).thenReturn(List.of(keyboard));
        when(products.decreaseStock(1, 50)).thenReturn(0);
        when(products.findAvailableQuantity(1)).thenReturn(10);

        var purchaseRequest = request(50);

        assertThatThrownBy(() -> service.purchase(purchaseRequest))
                .isInstanceOf(AppException.class);
    }

    @Test
    void purchase_unknownProduct_throwsAndTouchesNoStock() {
        when(movements.existsByPurchaseId(purchaseId)).thenReturn(false);
        when(products.findAllById(any())).thenReturn(List.of());

        var purchaseRequest = request(1);

        assertThatThrownBy(() -> service.purchase(purchaseRequest))
                .isInstanceOf(AppException.class);
        verify(products, never()).decreaseStock(anyInt(), anyInt());
    }

    @Test
    void purchase_sameIdTwice_isReplayedWithoutSecondDeduction() {
        var movement = StockMovement.builder().purchaseId(purchaseId).productId(1)
                .quantity(3).unitPrice(new BigDecimal("99.99")).build();
        when(movements.existsByPurchaseId(purchaseId)).thenReturn(true);
        when(movements.findAllByPurchaseId(purchaseId)).thenReturn(List.of(movement));
        when(products.findAllById(any())).thenReturn(List.of(keyboard));

        var result = service.purchase(request(3));

        assertThat(result).hasSize(1);
        verify(products, never()).decreaseStock(anyInt(), anyInt());
    }
}
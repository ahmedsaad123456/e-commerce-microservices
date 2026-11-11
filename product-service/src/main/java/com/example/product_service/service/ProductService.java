package com.example.product_service.service;

import com.example.product_service.common.Constants;
import com.example.product_service.common.ErrorCode;
import com.example.product_service.dto.*;
import com.example.product_service.entity.Product;
import com.example.product_service.entity.StockMovement;
import com.example.product_service.exceptions.AppException;
import com.example.product_service.exceptions.ResourceNotFoundException;
import com.example.product_service.mapper.ProductMapper;
import com.example.product_service.repository.CategoryRepository;
import com.example.product_service.repository.ProductRepository;
import com.example.product_service.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductMapper mapper;
    private final TransactionTemplate tx;
    private final StockMovementRepository movements;

    public ProductResponse createProduct(ProductRequest request) {
        var category = categories.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(Constants.CATEGORY,request.categoryId()));
        return mapper.toProductResponse(products.save(mapper.toProduct(request, category)));
    }

    // cacheable: if the same productId is requested again, return the cached result instead of querying the database
    @Cacheable(cacheNames = Constants.PRODUCT_CACHE, key = "#id")
    public ProductResponse findProductById(Integer id) {
        return products.findById(id)
                .map(mapper::toProductResponse)
                .orElseThrow(() -> new ResourceNotFoundException(Constants.PRODUCT,id));
    }

    public List<ProductResponse> findProductAll() {
        return products.findAll().stream().map(mapper::toProductResponse).toList();
    }

    public StockResponse checkProductAvailability(Integer id) {
        var product = products.findById(id).orElseThrow(() -> new ResourceNotFoundException(Constants.PRODUCT,id));
        return new StockResponse(product.getId(), product.getAvailableQuantity());
    }

    // changing the product invalidates its cached copy
    @Transactional
    @CacheEvict(cacheNames = Constants.PRODUCT_CACHE, key = "#id")
    public ProductResponse updateProduct(Integer id, ProductRequest request) {
        var product = products.findById(id).orElseThrow(() -> new ResourceNotFoundException(Constants.PRODUCT,id));
        var category = categories.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException(Constants.CATEGORY,request.categoryId()));
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setCategory(category);        // stock is changed only by purchases, never here
        return mapper.toProductResponse(product);
    }

    // NOTE: intentionally NOT annotated with @Transactional.
    // Each tx.execute(...) below must be its OWN transaction. If an outer @Transactional existed, the
    // template would just join it; after a unique-constraint violation PostgreSQL marks that whole
    // transaction as aborted, so the existsByPurchaseId(...) call in the catch block would fail
    // ("current transaction is aborted") and the duplicate-replay logic could never work.
    public List<PurchaseResponse> purchase(PurchaseRequest request) {
        try {
            // Run the whole purchase in one transaction: commit if doPurchase returns, roll back if it throws.
            return tx.execute(status -> doPurchase(request));
        } catch (DataIntegrityViolationException e) {
            // a concurrent retry with the same purchaseId committed first: answer like a replay
            if (movements.existsByPurchaseId(request.purchaseId())) {          // the violation came from the duplicate purchaseId
                return tx.execute(status -> replay(request.purchaseId()));     // return the original result in a fresh transaction
            }
            throw e;                                                            // some other integrity problem: let it propagate
        }
    }


    // The real purchase logic. Always runs inside the transaction opened by purchase().
    private List<PurchaseResponse> doPurchase(PurchaseRequest request) {
        // fast path: this purchase was already processed
        // (idempotency: the client retried with the same purchaseId, so do NOT take stock twice)
        if (movements.existsByPurchaseId(request.purchaseId())) {
            return replay(request.purchaseId());
        }

        // Merge duplicate items (same product listed twice -> quantities summed).
        // TreeMap keeps product ids SORTED: every purchase then locks/updates rows in the same order,
        // which prevents deadlocks between concurrent purchases.
        var quantities = new TreeMap<Integer, Integer>();
        request.items().forEach(i -> quantities.merge(i.productId(), i.quantity(), Integer::sum));

        // Load all needed products with ONE query and index them by id.
        var found = products.findAllById(quantities.keySet()).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        // If any requested product does not exist -> 404 for the first missing one.
        quantities.keySet().stream().filter(id -> !found.containsKey(id)).findFirst()
                .ifPresent(id -> { throw new ResourceNotFoundException(Constants.PRODUCT,id); });

        // 1) record the purchase FIRST: a concurrent duplicate fails on the unique constraint
        //    here, before any stock is touched
        var rows = quantities.entrySet().stream()
                .map(e -> StockMovement.builder()
                        .purchaseId(request.purchaseId())              // idempotency key shared by all rows of this purchase
                        .productId(e.getKey())
                        .quantity(e.getValue())
                        .unitPrice(found.get(e.getKey()).getPrice())   // snapshot of the price at purchase time
                        .build())
                .toList();
        movements.saveAll(rows);   // inserts are flushed before the first decreaseStock (flushAutomatically = true)

        // 2) then take the stock atomically (step 06); any failure rolls back step 1 as well
        for (var entry : quantities.entrySet()) {
            // Single atomic SQL: "set qty = qty - n where qty >= n". Returns the number of updated rows.
            if (products.decreaseStock(entry.getKey(), entry.getValue()) == 0) {
                // 0 rows updated = not enough stock. Throwing rolls back the movements AND any earlier decrements.
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK_EXCEPTION,entry.getKey(), found.get(entry.getKey()).getName(),
                        entry.getValue(), products.findAvailableQuantity(entry.getKey())); // product id, name, requested, available
            }
        }

        // Build the response from what was just recorded (stored price, product name).
        return rows.stream()
                .map(r -> new PurchaseResponse(r.getProductId(), found.get(r.getProductId()).getName(),
                        r.getUnitPrice(), r.getQuantity()))
                .toList();
    }

    // Rebuild the response of an already-processed purchase WITHOUT touching stock.
    private List<PurchaseResponse> replay(UUID purchaseId) {
        var rows = movements.findAllByPurchaseId(purchaseId);   // rows saved by the original purchase
        // One query to fetch the product names, indexed by id.
        var names = products.findAllById(rows.stream().map(StockMovement::getProductId).toList()).stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));
        // the price stored at purchase time is returned, even if the product price changed since
        return rows.stream()
                .map(r -> new PurchaseResponse(r.getProductId(), names.get(r.getProductId()),
                        r.getUnitPrice(), r.getQuantity()))
                .toList();
    }
}

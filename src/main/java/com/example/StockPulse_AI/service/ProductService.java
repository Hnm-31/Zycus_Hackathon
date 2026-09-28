package com.example.StockPulse_AI.service;

import com.example.StockPulse_AI.event.DemandSpikeEvent;
import com.example.StockPulse_AI.event.InventoryLowEvent;
import com.example.StockPulse_AI.model.Category;
import com.example.StockPulse_AI.model.Product;
import com.example.StockPulse_AI.model.ProductStatus;
import com.example.StockPulse_AI.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Product createProduct(Product product) {
        if (product.getStatus() == null) {
            product.setStatus(product.getStockLevel() <= 0 ? ProductStatus.OUT_OF_STOCK : ProductStatus.ACTIVE);
        }
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getProducts(ProductStatus status, Category category) {
        if (status != null && category != null) {
            return productRepository.findByCategoryAndStatus(category, status);
        } else if (status != null) {
            return productRepository.findByStatus(status);
        } else if (category != null) {
            return productRepository.findByCategory(category);
        }
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + id));
    }

    @Transactional
    public Product updateStock(String id, int newStock) {
        Product product = getProductById(id);
        int oldStock = product.getStockLevel();
        product.setStockLevel(newStock);

        if (newStock == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == ProductStatus.OUT_OF_STOCK && newStock > 0) {
            product.setStatus(ProductStatus.ACTIVE);
        }

        Product saved = productRepository.save(product);
        log.info("Stock updated for product {}: {} -> {}", id, oldStock, newStock);

        // Check Trigger A: Inventory Low
        if (newStock < saved.getReorderThreshold()) {
            log.info("Stock {} below threshold {} for product {}. Publishing InventoryLowEvent.", newStock, saved.getReorderThreshold(), id);
            eventPublisher.publishEvent(new InventoryLowEvent(id, newStock, saved.getReorderThreshold()));
        }

        return saved;
    }

    @Transactional
    public Product recordOrder(String id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Order quantity must be greater than zero.");
        }

        Product product = getProductById(id);
        if (product.getStockLevel() < quantity) {
            throw new IllegalStateException("Insufficient inventory. Current stock: " + product.getStockLevel() + ", requested: " + quantity);
        }

        // Decrement stock and increment demand velocity
        int newStock = product.getStockLevel() - quantity;
        int newVelocity = product.getDemandVelocity() + quantity;

        product.setStockLevel(newStock);
        product.setDemandVelocity(newVelocity);

        if (newStock == 0) {
            product.setStatus(ProductStatus.OUT_OF_STOCK);
        }

        Product saved = productRepository.save(product);
        log.info("Simulated order on product {}: -{} stock, velocity now {}", id, quantity, newVelocity);

        // Check category average velocity
        double categoryAvg = productRepository.getAverageDemandVelocityByCategory(saved.getCategory());

        // Trigger A: Inventory Low
        if (newStock < saved.getReorderThreshold()) {
            log.info("Low stock trigger hit after order for product {} ({}/{}).", id, newStock, saved.getReorderThreshold());
            eventPublisher.publishEvent(new InventoryLowEvent(id, newStock, saved.getReorderThreshold()));
        }

        // Trigger B: Demand Spike (e.g., velocity >= 3x category average or spike threshold)
        if (newVelocity >= (3.0 * categoryAvg) && newVelocity >= 10) {
            log.info("Demand spike trigger hit after order for product {} (velocity {} vs category avg {}).", id, newVelocity, categoryAvg);
            eventPublisher.publishEvent(new DemandSpikeEvent(id, newVelocity, categoryAvg));
        }

        return saved;
    }
}

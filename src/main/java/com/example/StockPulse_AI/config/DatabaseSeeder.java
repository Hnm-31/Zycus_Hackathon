package com.example.StockPulse_AI.config;

import com.example.StockPulse_AI.model.*;
import com.example.StockPulse_AI.repository.PricingSuggestionRepository;
import com.example.StockPulse_AI.repository.ProductRepository;
import com.example.StockPulse_AI.repository.ReorderSuggestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;
    private final PricingSuggestionRepository pricingSuggestionRepository;
    private final ReorderSuggestionRepository reorderSuggestionRepository;

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) {
            log.info("Products already seeded, skipping.");
            return;
        }

        log.info("Seeding Addendum A benchmark products...");

        List<Product> products = List.of(
                Product.builder()
                        .id("PRD-001")
                        .sku("SKU-ELEC-001")
                        .name("Wireless Earbuds Pro")
                        .category(Category.ELECTRONICS)
                        .currentPrice(79.99)
                        .stockLevel(45)
                        .reorderThreshold(20)
                        .demandVelocity(3)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(42.00)
                        .supplierId("SUP-01")
                        .build(),

                Product.builder()
                        .id("PRD-002")
                        .sku("SKU-ELEC-002")
                        .name("USB-C Hub 7-Port")
                        .category(Category.ELECTRONICS)
                        .currentPrice(34.99)
                        .stockLevel(120)
                        .reorderThreshold(30)
                        .demandVelocity(1)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(18.50)
                        .supplierId("SUP-02")
                        .build(),

                Product.builder()
                        .id("PRD-003")
                        .sku("SKU-APP-001")
                        .name("Organic Cotton T-Shirt")
                        .category(Category.APPAREL)
                        .currentPrice(24.99)
                        .stockLevel(8)
                        .reorderThreshold(15)
                        .demandVelocity(12)
                        .status(ProductStatus.PRICE_REVIEW_PENDING)
                        .costPrice(11.00)
                        .supplierId("SUP-03")
                        .build(),

                Product.builder()
                        .id("PRD-004")
                        .sku("SKU-APP-002")
                        .name("Running Shorts — Navy")
                        .category(Category.APPAREL)
                        .currentPrice(39.99)
                        .stockLevel(55)
                        .reorderThreshold(20)
                        .demandVelocity(2)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(21.00)
                        .supplierId("SUP-03")
                        .build(),

                Product.builder()
                        .id("PRD-005")
                        .sku("SKU-HOME-001")
                        .name("Ceramic Pour-Over Set")
                        .category(Category.HOME)
                        .currentPrice(49.99)
                        .stockLevel(22)
                        .reorderThreshold(10)
                        .demandVelocity(4)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(27.00)
                        .supplierId("SUP-04")
                        .build(),

                Product.builder()
                        .id("PRD-006")
                        .sku("SKU-HOME-002")
                        .name("LED Desk Lamp — Dimmable")
                        .category(Category.HOME)
                        .currentPrice(59.99)
                        .stockLevel(0)
                        .reorderThreshold(15)
                        .demandVelocity(0)
                        .status(ProductStatus.OUT_OF_STOCK)
                        .costPrice(32.00)
                        .supplierId("SUP-04")
                        .build(),

                Product.builder()
                        .id("PRD-007")
                        .sku("SKU-ELEC-003")
                        .name("Portable Charger 20K")
                        .category(Category.ELECTRONICS)
                        .currentPrice(44.99)
                        .stockLevel(18)
                        .reorderThreshold(25)
                        .demandVelocity(8)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(25.00)
                        .supplierId("SUP-01")
                        .build(),

                Product.builder()
                        .id("PRD-008")
                        .sku("SKU-APP-003")
                        .name("Hoodie — Heather Grey")
                        .category(Category.APPAREL)
                        .currentPrice(54.99)
                        .stockLevel(11)
                        .reorderThreshold(12)
                        .demandVelocity(15)
                        .status(ProductStatus.ACTIVE)
                        .costPrice(29.00)
                        .supplierId("SUP-03")
                        .build()
        );

        productRepository.saveAll(products);
        log.info("Successfully seeded 8 products.");

        // Pre-seed an initial low-inventory suggestion for PRD-003 (as it starts in PRICE_REVIEW_PENDING)
        Product prd3 = productRepository.findById("PRD-003").orElse(null);
        if (prd3 != null) {
            PricingSuggestion initialPricing = PricingSuggestion.builder()
                    .product(prd3)
                    .currentPrice(24.99)
                    .recommendedPrice(27.49)
                    .changeDirection(ChangeDirection.INCREASE)
                    .confidence(0.85)
                    .reasoning("Deterministic Rule: Stock (8) is below threshold (15). Recommending +10% price increase from $24.99 to $27.49 to protect scarce inventory.")
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(TriggerReason.INVENTORY_LOW)
                    .createdAt(LocalDateTime.now().minusMinutes(5))
                    .build();
            pricingSuggestionRepository.save(initialPricing);

            ReorderSuggestion initialReorder = ReorderSuggestion.builder()
                    .product(prd3)
                    .currentStock(8)
                    .recommendedQuantity(37) // (15 * 3) - 8 = 37
                    .suggestedLeadTimeDays(7)
                    .confidence(0.85)
                    .reasoning("Deterministic Rule: Restocking calculation (3x threshold [45] - current stock [8]). Recommending 37 units with estimated 7 days lead time.")
                    .status(SuggestionStatus.PENDING)
                    .triggerReason(TriggerReason.INVENTORY_LOW)
                    .createdAt(LocalDateTime.now().minusMinutes(5))
                    .build();
            reorderSuggestionRepository.save(initialReorder);

            log.info("Pre-seeded baseline suggestions for PRD-003.");
        }
    }
}

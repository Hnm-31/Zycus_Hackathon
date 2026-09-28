package com.example.StockPulse_AI.repository;

import com.example.StockPulse_AI.model.Category;
import com.example.StockPulse_AI.model.Product;
import com.example.StockPulse_AI.model.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByCategory(Category category);

    List<Product> findByStatus(ProductStatus status);

    List<Product> findByCategoryAndStatus(Category category, ProductStatus status);

    @Query("SELECT COALESCE(AVG(p.demandVelocity), 0.0) FROM Product p WHERE p.category = :category")
    Double getAverageDemandVelocityByCategory(@Param("category") Category category);
}

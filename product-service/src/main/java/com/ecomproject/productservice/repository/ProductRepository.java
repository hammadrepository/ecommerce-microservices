package com.ecomproject.productservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.ecomproject.productservice.model.Product;

public interface ProductRepository extends MongoRepository<Product, String> {
    
}

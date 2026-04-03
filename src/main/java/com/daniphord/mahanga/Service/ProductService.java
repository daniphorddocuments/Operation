package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.Product;
import com.daniphord.mahanga.Repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;

    // GET all products
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    // GET product by ID
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    // CREATE product
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    // UPDATE product
    public ResponseEntity<Product> updateProduct(Long id, Product updatedProduct) {
        Optional<Product> existingOpt = productRepository.findById(id);
        if (existingOpt.isEmpty()) return ResponseEntity.notFound().build();

        Product existing = existingOpt.get();
        existing.setName(updatedProduct.getName());
        existing.setCost(updatedProduct.getCost());
        existing.setPrice(updatedProduct.getPrice());
        existing.setStock(updatedProduct.getStock());
        existing.setShop(updatedProduct.getShop());
        productRepository.save(existing);

        return ResponseEntity.ok(existing);
    }

    // DELETE product
    public boolean deleteProduct(Long id) {
        if (!productRepository.existsById(id)) return false;
        productRepository.deleteById(id);
        return true;
    }

    public List<Product> getProductsByShopId(Long shopId) {
        if (shopId == null) {
            return getAllProducts();
        }
        return productRepository.findByShopShopId(shopId);
    }
}

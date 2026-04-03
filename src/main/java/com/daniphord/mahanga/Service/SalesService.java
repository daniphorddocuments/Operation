package com.daniphord.mahanga.Service;

import com.daniphord.mahanga.Model.Product;
import com.daniphord.mahanga.Model.Sale;
import com.daniphord.mahanga.Repositories.ProductRepository;
import com.daniphord.mahanga.Repositories.SaleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SalesService {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductRepository productRepository;

    // GET all sales
    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    // GET sale by ID
    public Optional<Sale> getSaleById(Long id) {
        return saleRepository.findById(id);
    }

    // CREATE sale
    public Sale createSale(Long productId, int quantity) throws Exception {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new Exception("Product not found"));

        if (quantity <= 0) {
            throw new Exception("Quantity must be greater than zero");
        }
        if (product.getStock() < quantity) throw new Exception("Not enough stock");

        Sale sale = new Sale();
        sale.setProduct(product);
        sale.setQuantity(quantity);
        sale.setTotal(quantity * product.getPrice());
        sale.setProfit(quantity * (product.getPrice() - product.getCost()));
        sale.setDateTime(LocalDateTime.now());

        // set shop from product
        if (product.getShop() != null) sale.setShop(product.getShop());

        // reduce stock
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        return saleRepository.save(sale);
    }

    // DELETE sale
    public boolean deleteSale(Long id) {
        if (!saleRepository.existsById(id)) return false;
        saleRepository.deleteById(id);
        return true;
    }

    // GET sales by shop
    public List<Sale> getSalesByShop(Long shopId) {
        return saleRepository.findByShopId(shopId);
    }

    // GET sales by product
    public List<Sale> getSalesByProduct(Long productId) {
        return saleRepository.findByProductId(productId);
    }

    // GET today's sales
    public List<Sale> getTodaySales() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
        return saleRepository.findTodaySales(startOfDay, endOfDay);
    }

    // TOTAL sales amount
    public double getTotalSales() {
        return saleRepository.findAll().stream()
                .mapToDouble(Sale::getTotal)
                .sum();
    }

    // TOTAL profit
    public double getTotalProfit() {
        return saleRepository.findAll().stream()
                .mapToDouble(Sale::getProfit)
                .sum();
    }
}

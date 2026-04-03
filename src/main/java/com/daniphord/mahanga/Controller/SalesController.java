package com.daniphord.mahanga.Controller;

import com.daniphord.mahanga.Model.Sale;
import com.daniphord.mahanga.Service.SalesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales")
public class SalesController {

    @Autowired
    private SalesService salesService;

    // GET all sales
    @GetMapping
    public List<Sale> getAllSales() {
        return salesService.getAllSales();
    }

    // GET sale by ID
    @GetMapping("/{id}")
    public ResponseEntity<Sale> getSaleById(@PathVariable Long id) {
        return salesService.getSaleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // CREATE sale
    @PostMapping
    public ResponseEntity<?> createSale(@RequestParam Long productId, @RequestParam int quantity) {
        try {
            Sale sale = salesService.createSale(productId, quantity);
            return ResponseEntity.ok(sale);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE sale
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSale(@PathVariable Long id) {
        boolean deleted = salesService.deleteSale(id);
        if (!deleted) return ResponseEntity.notFound().build();
        return ResponseEntity.noContent().build();
    }

    // GET sales by shop
    @GetMapping("/shop/{shopId}")
    public List<Sale> getSalesByShop(@PathVariable Long shopId) {
        return salesService.getSalesByShop(shopId);
    }

    // GET sales by product
    @GetMapping("/product/{productId}")
    public List<Sale> getSalesByProduct(@PathVariable Long productId) {
        return salesService.getSalesByProduct(productId);
    }

    // GET today's sales
    @GetMapping("/today")
    public List<Sale> getTodaySales() {
        return salesService.getTodaySales();
    }

    // TOTAL sales
    @GetMapping("/total")
    public double getTotalSales() {
        return salesService.getTotalSales();
    }

    // TOTAL profit
    @GetMapping("/profit")
    public double getTotalProfit() {
        return salesService.getTotalProfit();
    }
}
package com.daniphord.mahanga.Model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long saleId;

    @ManyToOne
    private Product product;

    private int quantity;
    private double total;
    private double profit;

    private LocalDateTime dateTime;

    @ManyToOne
    private Shop shop;
}

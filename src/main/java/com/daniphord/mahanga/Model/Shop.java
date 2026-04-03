package com.daniphord.mahanga.Model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.List;

@Entity
public class Shop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long shopId;

    private String name;
    private String location;
    private LocalDate createdDate;
    private boolean active;
    private boolean paymentComplete;
    private LocalDate paymentDate;
    private LocalDate paymentExpiryDate;
    private String paymentNumber;

    @Transient
    private double currentMonthProfit;

    @Transient
    private double currentMonthFeeDue;

    @Transient
    private boolean firstMonthFree;

    @JsonIgnore
    @OneToMany(mappedBy = "shop")
    private List<User> users;

    // Getters & Setters
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isPaymentComplete() { return paymentComplete; }
    public void setPaymentComplete(boolean paymentComplete) { this.paymentComplete = paymentComplete; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public LocalDate getPaymentExpiryDate() { return paymentExpiryDate; }
    public void setPaymentExpiryDate(LocalDate paymentExpiryDate) { this.paymentExpiryDate = paymentExpiryDate; }

    public String getPaymentNumber() { return paymentNumber; }
    public void setPaymentNumber(String paymentNumber) { this.paymentNumber = paymentNumber; }

    public double getCurrentMonthProfit() { return currentMonthProfit; }
    public void setCurrentMonthProfit(double currentMonthProfit) { this.currentMonthProfit = currentMonthProfit; }

    public double getCurrentMonthFeeDue() { return currentMonthFeeDue; }
    public void setCurrentMonthFeeDue(double currentMonthFeeDue) { this.currentMonthFeeDue = currentMonthFeeDue; }

    public boolean isFirstMonthFree() { return firstMonthFree; }
    public void setFirstMonthFree(boolean firstMonthFree) { this.firstMonthFree = firstMonthFree; }

    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    @PrePersist
    public void prePersist() {
        if (createdDate == null) {
            createdDate = LocalDate.now();
        }
    }
}

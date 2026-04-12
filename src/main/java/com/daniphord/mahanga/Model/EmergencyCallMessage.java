package com.daniphord.mahanga.Model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "emergency_call_messages", indexes = {
        @Index(name = "idx_call_message_call", columnList = "emergency_call_id"),
        @Index(name = "idx_call_message_created", columnList = "createdAt")
})
public class EmergencyCallMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emergency_call_id", nullable = false)
    private EmergencyCall emergencyCall;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")
    private User senderUser;

    @Column(nullable = false)
    private String senderType;

    @Column(nullable = false, length = 1200)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public EmergencyCall getEmergencyCall() { return emergencyCall; }
    public void setEmergencyCall(EmergencyCall emergencyCall) { this.emergencyCall = emergencyCall; }
    public User getSenderUser() { return senderUser; }
    public void setSenderUser(User senderUser) { this.senderUser = senderUser; }
    public String getSenderType() { return senderType; }
    public void setSenderType(String senderType) { this.senderType = senderType; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

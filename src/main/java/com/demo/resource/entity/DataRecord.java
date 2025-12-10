package com.demo.resource.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "data_record")
public class DataRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "amount")
    private BigDecimal amount;

    @OneToMany(mappedBy = "dataRecord", fetch = FetchType.LAZY)
    private List<RelatedEntity> relatedEntities = new ArrayList<>();

    public DataRecord() {
    }

    public DataRecord(String payload, String category, BigDecimal amount) {
        this.payload = payload;
        this.timestamp = LocalDateTime.now();
        this.category = category;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public List<RelatedEntity> getRelatedEntities() {
        return relatedEntities;
    }

    public void setRelatedEntities(List<RelatedEntity> relatedEntities) {
        this.relatedEntities = relatedEntities;
    }
}

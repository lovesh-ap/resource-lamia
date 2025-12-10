package com.demo.resource.entity;

import javax.persistence.*;

@Entity
@Table(name = "related_entity")
public class RelatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_record_id")
    private DataRecord dataRecord;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "status", length = 50)
    private String status;

    public RelatedEntity() {
    }

    public RelatedEntity(DataRecord dataRecord, String metadata, String status) {
        this.dataRecord = dataRecord;
        this.metadata = metadata;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DataRecord getDataRecord() {
        return dataRecord;
    }

    public void setDataRecord(DataRecord dataRecord) {
        this.dataRecord = dataRecord;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

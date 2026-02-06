package com.beet.backend.shared.domain.model;

import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.Instant;
import java.util.UUID;

@Data
public abstract class BaseAuditableAggregate {

    @CreatedDate
    private Instant createdAt;

    @CreatedBy
    private UUID createdBy;

    @LastModifiedDate
    private Instant updatedAt;

    @LastModifiedBy
    private UUID updatedBy;

    // Soft Delete Fields
    private Instant deletedAt;
    private UUID deletedBy;

    /**
     * Marks the aggregate as deleted.
     * 
     * @param actorId The user performing the deletion.
     */
    public void markDeleted(UUID actorId) {
        this.deletedAt = Instant.now();
        this.deletedBy = actorId;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}

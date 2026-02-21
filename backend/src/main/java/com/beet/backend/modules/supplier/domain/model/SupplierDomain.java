package com.beet.backend.modules.supplier.domain.model;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
public class SupplierDomain {
    private UUID id;
    private UUID ownerId;
    private UUID documentTypeId;
    private String documentNumber;
    private String name;
    private String contactName;
    private String email;
    private String phone;
    private String address;
    private Boolean isActive;
}

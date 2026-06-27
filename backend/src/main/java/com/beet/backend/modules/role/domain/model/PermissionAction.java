package com.beet.backend.modules.role.domain.model;

public enum PermissionAction {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    MANAGE, // Implies full access
    VIEW_ALL,
    VIEW,
    EDIT,
    ACTIVATE, // Inventory: activate an ingredient in a restaurant
    VOID,
    VIEW_ALERTS,
    UPDATE_STATUS,
    OPEN,
    CLOSE,
    PROCESS,
    ASSIGN_GUESTS,
    COMMENT,
    CANCEL,
    COMPLETE,
    REFUND,
    // Wildcard for "all actions"
    ALL
}

package com.beet.backend.shared.infrastructure.input.rest;

import java.util.List;

/**
 * Generic paginated response wrapper that mirrors the Spring Page<T> structure.
 * Matches the frontend PageResponse<T> TypeScript interface exactly.
 */
public record PageResponse<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int size,
        int number, // current page index (0-based)
        boolean first,
        boolean last,
        int numberOfElements,
        boolean empty) {
    public static <T> PageResponse<T> of(List<T> content, long totalElements, int page, int size) {
        int totalPages = size == 0 ? 1 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(
                content,
                totalPages,
                totalElements,
                size,
                page,
                page == 0,
                page >= totalPages - 1,
                content.size(),
                content.isEmpty());
    }
}

package com.gonggomoa.admin;

/**
 * docs/03-api-spec.md: {@code { "institutionId": null } } (null이면 전체).
 */
public record CollectRequest(Long institutionId) {
}

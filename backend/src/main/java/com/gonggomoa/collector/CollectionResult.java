package com.gonggomoa.collector;

public record CollectionResult(int fetchedCount, int newCount, int updatedCount, boolean success,
		String errorMessage) {
}

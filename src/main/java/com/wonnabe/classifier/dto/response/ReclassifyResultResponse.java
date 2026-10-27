package com.wonnabe.classifier.dto.response;

public record ReclassifyResultResponse(int attempted, int reclassified, int stillUnclassified) {
}

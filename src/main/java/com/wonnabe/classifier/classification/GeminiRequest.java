package com.wonnabe.classifier.classification;

import java.util.List;

record GeminiRequest(SystemInstruction systemInstruction, List<Content> contents) {

	record SystemInstruction(List<Part> parts) {
	}

	record Content(String role, List<Part> parts) {
	}

	record Part(String text) {
	}
}

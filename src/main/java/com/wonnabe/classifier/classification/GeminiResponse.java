package com.wonnabe.classifier.classification;

import java.util.List;

record GeminiResponse(List<Candidate> candidates) {

	record Candidate(Content content) {
	}

	record Content(List<Part> parts) {
	}

	record Part(String text) {
	}
}

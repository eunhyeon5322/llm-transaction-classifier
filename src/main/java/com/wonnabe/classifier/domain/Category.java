package com.wonnabe.classifier.domain;

public enum Category {
	FOOD("식비"),
	TRANSPORT("교통"),
	SHOPPING("쇼핑"),
	CULTURE("문화"),
	OTHER("기타");

	private final String label;

	Category(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}

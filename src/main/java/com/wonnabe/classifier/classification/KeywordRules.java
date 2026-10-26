package com.wonnabe.classifier.classification;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.wonnabe.classifier.domain.Category;

/**
 * 가맹점명/업종에 포함된 키워드로 1차 분류하는 규칙.
 * LinkedHashMap 순서대로 검사하므로, 겹치는 키워드가 있으면 먼저 선언된 카테고리가 우선한다.
 */
final class KeywordRules {

	static final Map<Category, List<String>> RULES = new LinkedHashMap<>();

	static {
		RULES.put(Category.FOOD, List.of(
				"스타벅스", "투썸플레이스", "매머드", "커피", "카페", "맥도날드", "버거", "치킨", "피자",
				"떡볶이", "김밥", "식당", "베이커리", "파리바게트", "뚜레쥬르", "빵", "스시", "라멘", "국밥", "분식"
		));
		RULES.put(Category.TRANSPORT, List.of(
				"택시", "버스", "지하철", "ktx", "고속버스", "주유소", "톨게이트", "카카오t", "기차", "철도",
				"교통요금", "공항", "airport", "주차"
		));
		RULES.put(Category.SHOPPING, List.of(
				"올리브영", "무신사", "백화점", "마트", "편의점", "쇼핑몰", "아울렛", "다이소", "쿠팡", "이마트", "홈플러스", "gs25", "cu", "세븐일레븐"
		));
		RULES.put(Category.CULTURE, List.of(
				"영화관", "cgv", "메가박스", "롯데시네마", "서점", "교보문고", "전시", "공연", "콘서트", "노래방", "넷플릭스", "멜론", "웨이브", "티빙",
				"미술관", "박물관", "티켓"
		));
		RULES.put(Category.OTHER, List.of(
				"보험", "생명", "용돈", "이체", "송금", "카드론", "대출", "적금", "저축", "관리비", "통신비", "세금", "보험료"
		));
	}

	private KeywordRules() {
	}
}

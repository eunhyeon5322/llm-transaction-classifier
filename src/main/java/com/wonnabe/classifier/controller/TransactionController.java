package com.wonnabe.classifier.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wonnabe.classifier.dto.response.ReclassifyResultResponse;
import com.wonnabe.classifier.dto.response.TransactionResponse;
import com.wonnabe.classifier.dto.response.UploadResultResponse;
import com.wonnabe.classifier.repository.TransactionRepository;
import com.wonnabe.classifier.service.ReclassificationService;
import com.wonnabe.classifier.service.TransactionUploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Transactions", description = "카드사 거래내역 업로드/조회")
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

	private final TransactionUploadService transactionUploadService;
	private final TransactionRepository transactionRepository;
	private final ReclassificationService reclassificationService;

	@Operation(summary = "카드사 CSV 거래내역 업로드", description = "transaction_date, merchant_name, amount(필수), merchant_store_type(선택) 컬럼을 포함한 CSV를 업로드합니다. 분류는 아직 적용되지 않은 UNCLASSIFIED 상태로 저장됩니다.")
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public UploadResultResponse upload(@RequestParam("file") MultipartFile file) {
		return transactionUploadService.upload(file);
	}

	@Operation(summary = "저장된 거래내역 전체 조회")
	@GetMapping
	public List<TransactionResponse> list() {
		return transactionRepository.findAll().stream()
				.map(TransactionResponse::from)
				.toList();
	}

	@Operation(summary = "UNCLASSIFIED 거래 2차 분류(Gemini API)", description = "키워드로 분류되지 않은 거래들을 Gemini API로 재분류합니다. 여러 건을 동시에 호출합니다.")
	@PostMapping("/reclassify")
	public ReclassifyResultResponse reclassify() {
		return reclassificationService.reclassifyUnclassified();
	}
}

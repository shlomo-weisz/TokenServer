package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.BuyTokensRequest;
import com.tokenlearn.server.dto.CreateTokenTransactionRequest;
import com.tokenlearn.server.dto.TransferTokensRequest;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.service.TokenService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.tokenlearn.server.controller.RestResponses.ok;

/**
 * Endpoints for the authenticated wallet, purchases, peer transfers, and transaction history.
 */
@RestController
@RequestMapping("/api")
public class TokenController {
    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/users/me/wallet")
    public ResponseEntity<Map<String, Object>> balance(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(tokenService.getBalance(userId));
    }

    @PostMapping("/users/me/token-transactions")
    public ResponseEntity<Map<String, Object>> createTransaction(
            Authentication authentication,
            @Valid @RequestBody CreateTokenTransactionRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);

        String type = normalizeType(request);
        return switch (type) {
            case "purchase" -> ok(tokenService.buy(userId, toBuyTokensRequest(request)));
            case "transfer" -> ok(tokenService.transfer(userId, toTransferTokensRequest(request)));
            default -> throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TRANSACTION_TYPE",
                    "Transaction type must be purchase or transfer");
        };
    }

    @GetMapping("/users/me/token-transactions")
    public ResponseEntity<Map<String, Object>> history(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(tokenService.history(userId, limit, offset));
    }

    private String normalizeType(CreateTokenTransactionRequest request) {
        if (request.getType() != null && !request.getType().isBlank()) {
            return request.getType().trim().toLowerCase();
        }
        if (request.getToUserId() != null) {
            return "transfer";
        }
        if (request.getPaymentMethod() != null || request.getPaymentDetails() != null) {
            return "purchase";
        }
        return "";
    }

    private BuyTokensRequest toBuyTokensRequest(CreateTokenTransactionRequest request) {
        if (request.getAmount() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Amount is required");
        }
        BuyTokensRequest buyTokensRequest = new BuyTokensRequest();
        buyTokensRequest.setAmount(request.getAmount());
        buyTokensRequest.setPaymentMethod(request.getPaymentMethod());
        buyTokensRequest.setPaymentDetails(request.getPaymentDetails());
        return buyTokensRequest;
    }

    private TransferTokensRequest toTransferTokensRequest(CreateTokenTransactionRequest request) {
        if (request.getAmount() == null || request.getToUserId() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "amount and toUserId are required");
        }
        TransferTokensRequest transferTokensRequest = new TransferTokensRequest();
        transferTokensRequest.setAmount(request.getAmount());
        transferTokensRequest.setToUserId(request.getToUserId());
        transferTokensRequest.setLessonId(request.getLessonId());
        transferTokensRequest.setReason(request.getReason());
        return transferTokensRequest;
    }
}

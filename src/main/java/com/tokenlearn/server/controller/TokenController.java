package com.tokenlearn.server.controller;

import com.tokenlearn.server.dto.ApiResponse;
import com.tokenlearn.server.dto.BuyTokensRequest;
import com.tokenlearn.server.dto.TransferTokensRequest;
import com.tokenlearn.server.service.TokenService;
import com.tokenlearn.server.util.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.tokenlearn.server.controller.ApiResponses.ok;

/**
 * Endpoints for token wallet balance, purchases, peer transfers, and transaction history.
 */
@RestController
@RequestMapping("/api/tokens")
public class TokenController {
    private final TokenService tokenService;

    public TokenController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<Map<String, Object>>> balance(Authentication authentication) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(tokenService.getBalance(userId));
    }

    @PostMapping("/buy")
    public ResponseEntity<ApiResponse<Map<String, Object>>> buy(
            Authentication authentication,
            @Valid @RequestBody BuyTokensRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(tokenService.buy(userId, request));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> transfer(
            Authentication authentication,
            @Valid @RequestBody TransferTokensRequest request) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(tokenService.transfer(userId, request));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> history(
            Authentication authentication,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Integer userId = AuthUtil.requireUserId(authentication);
        return ok(tokenService.history(userId, limit, offset));
    }
}

package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.TokenTransactionDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.TokenTransactionEntity;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.BuyTokensRequest;
import com.tokenlearn.server.dto.TokenBalancesDto;
import com.tokenlearn.server.dto.TransferTokensRequest;
import com.tokenlearn.server.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TokenService {
    private final UserDao userDao;
    private final TokenTransactionDao transactionDao;

    public TokenService(UserDao userDao, TokenTransactionDao transactionDao) {
        this.userDao = userDao;
        this.transactionDao = transactionDao;
    }

    public Map<String, Object> getBalance(Integer userId) {
        TokenBalancesDto b = userDao.getBalances(userId);
        return Map.of(
                "balance", b.getTotal(),
                "total", b.getTotal(),
                "available", b.getAvailable(),
                "locked", b.getLocked(),
                "futureTutorEarnings", b.getFutureTutorEarnings(),
                "pendingTransfers", b.getPendingTransfers());
    }

    @Transactional
    public Map<String, Object> buy(Integer userId, BuyTokensRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be positive");
        }
        userDao.addAvailable(userId, request.getAmount());
        Long txId = transactionDao.create(TokenTransactionEntity.builder()
                .payerId(userId)
                .receiverId(userId)
                .amount(request.getAmount())
                .txType("PURCHASE")
                .status("SUCCESS")
                .description("Tokens added by purchase")
                .build());
        TokenBalancesDto b = userDao.getBalances(userId);
        return Map.of("success", true, "newBalance", b.getTotal(), "transactionId", "txn_" + txId);
    }

    @Transactional
    public Map<String, Object> transfer(Integer fromUserId, TransferTokensRequest request) {
        if (fromUserId.equals(request.getToUserId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Cannot transfer to self");
        }
        UserEntity to = userDao.findById(request.getToUserId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Recipient not found"));
        if (!userDao.transferAvailable(fromUserId, to.getUserId(), request.getAmount())) {
            throw new AppException(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_BALANCE", "Insufficient available balance");
        }
        Long txId = transactionDao.create(TokenTransactionEntity.builder()
                .lessonId(request.getLessonId())
                .payerId(fromUserId)
                .receiverId(to.getUserId())
                .amount(request.getAmount())
                .txType("TRANSFER")
                .status("SUCCESS")
                .description(request.getReason() == null ? "Tokens transferred to another user" : request.getReason())
                .build());
        TokenBalancesDto b = userDao.getBalances(fromUserId);
        return Map.of("success", true, "newBalance", b.getTotal(), "transactionId", "txn_" + txId);
    }

    public Map<String, Object> history(Integer userId, int limit, int offset) {
        List<Map<String, Object>> txs = transactionDao.findByUser(userId, limit, offset).stream().map(tx -> {
            BigDecimal rawAmount = tx.getAmount() == null ? BigDecimal.ZERO : tx.getAmount();
            BigDecimal signedAmount = switch (String.valueOf(tx.getTxType())) {
                case "PURCHASE", "BONUS", "REFUND" -> rawAmount.abs();
                case "RESERVATION" -> rawAmount.abs().negate();
                case "ADMIN_ADJUST" -> rawAmount;
                case "TRANSFER", "SETTLEMENT" -> Objects.equals(tx.getPayerId(), userId) ? rawAmount.abs().negate() : rawAmount.abs();
                default -> Objects.equals(tx.getPayerId(), userId) ? rawAmount.abs().negate() : rawAmount.abs();
            };
            String type = switch (tx.getTxType()) {
                case "PURCHASE" -> "purchase";
                case "BONUS" -> "bonus";
                case "TRANSFER", "SETTLEMENT" -> tx.getPayerId().equals(userId) ? "transfer_out" : "transfer_in";
                case "RESERVATION" -> "reservation";
                case "REFUND" -> "refund";
                default -> tx.getTxType().toLowerCase();
            };
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", "txn_" + tx.getTxId());
            item.put("type", type);
            item.put("amount", signedAmount);
            item.put("reason", tx.getDescription() == null ? "" : tx.getDescription());
            item.put("createdAt", tx.getCreatedAt());
            return item;
        }).toList();
        return Map.of("transactions", txs, "totalCount", transactionDao.countByUser(userId));
    }
}

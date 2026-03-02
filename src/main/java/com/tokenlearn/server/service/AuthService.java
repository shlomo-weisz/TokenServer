package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.PasswordResetDao;
import com.tokenlearn.server.dao.TokenTransactionDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.TokenTransactionEntity;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.*;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.security.JwtProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final PasswordResetDao passwordResetDao;
    private final TokenTransactionDao tokenTransactionDao;
    private final GoogleTokenVerifier googleTokenVerifier;

    @Value("${app.reset-token-exp-minutes:15}")
    private int resetTokenMinutes;

    @Value("${app.welcome-bonus-users:50}")
    private int welcomeUserLimit;

    @Value("${app.welcome-bonus-amount:50}")
    private int welcomeBonusAmount;

    public AuthService(UserDao userDao,
            PasswordEncoder passwordEncoder,
            JwtProvider jwtProvider,
            PasswordResetDao passwordResetDao,
            TokenTransactionDao tokenTransactionDao,
            GoogleTokenVerifier googleTokenVerifier) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.passwordResetDao = passwordResetDao;
        this.tokenTransactionDao = tokenTransactionDao;
        this.googleTokenVerifier = googleTokenVerifier;
    }

    @Transactional
    public AuthPayloadDto register(RegisterRequest request) {
        if (userDao.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "EMAIL_EXISTS", "Email already exists");
        }

        int beforeCount = userDao.countUsers();
        boolean isFirstFifty = beforeCount < welcomeUserLimit;

        UserEntity entity = UserEntity.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .secretQuestion(request.getSecretQuestion())
                .secretAnswerHash(passwordEncoder.encode(request.getSecretAnswer()))
                .isAdmin(false)
                .isActive(true)
                .availableBalance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .build();
        Integer userId = userDao.create(entity);
        entity.setUserId(userId);

        int bonus = 0;
        if (isFirstFifty) {
            bonus = welcomeBonusAmount;
            userDao.addAvailable(userId, BigDecimal.valueOf(welcomeBonusAmount));
            tokenTransactionDao.create(TokenTransactionEntity.builder()
                    .payerId(userId)
                    .receiverId(userId)
                    .amount(BigDecimal.valueOf(welcomeBonusAmount))
                    .txType("BONUS")
                    .status("SUCCESS")
                    .description("Welcome bonus - First 50 users")
                    .build());
        }

        String token = jwtProvider.generateToken(userId, entity.getEmail());
        return AuthPayloadDto.builder()
                .token(token)
                .user(UserSummaryDto.builder()
                        .id(userId)
                        .email(entity.getEmail())
                        .firstName(entity.getFirstName())
                        .lastName(entity.getLastName())
                        .phone(entity.getPhone())
                        .isAdmin(false)
                        .build())
                .isFirstFiftyUser(isFirstFifty)
                .bonusTokens(bonus)
                .build();
    }

    public AuthPayloadDto login(AuthLoginRequest request) {
        UserEntity user = userDao.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect");
        }
        return AuthPayloadDto.builder()
                .token(jwtProvider.generateToken(user.getUserId(), user.getEmail()))
                .user(toSummary(user))
                .build();
    }

    public String getSecretQuestion(String email) {
        UserEntity user = userDao.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
        return user.getSecretQuestion();
    }

    @Transactional
    public String verifySecretAnswerAndCreateResetToken(VerifySecretAnswerRequest request) {
        UserEntity user = userDao.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
        if (user.getSecretAnswerHash() == null || !passwordEncoder.matches(request.getSecretAnswer(), user.getSecretAnswerHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_SECRET_ANSWER", "Incorrect answer");
        }
        String token = UUID.randomUUID().toString().replace("-", "");
        passwordResetDao.createToken(user.getEmail(), token, LocalDateTime.now().plusMinutes(resetTokenMinutes));
        return token;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        boolean valid = passwordResetDao.consumeIfValid(request.getEmail(), request.getResetToken()).isPresent();
        if (!valid) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", "Invalid or expired reset token");
        }
        userDao.updatePasswordByEmail(request.getEmail(), passwordEncoder.encode(request.getNewPassword()));
    }

    @Transactional
    public AuthPayloadDto googleLogin(GoogleAuthRequest request) {
        GoogleTokenVerifier.GoogleUserClaims claims = googleTokenVerifier.verify(request.getGoogleToken());
        UserEntity user = userDao.findByEmail(claims.email()).map(existing -> {
            boolean changed = false;
            if (claims.givenName() != null && !claims.givenName().equals(existing.getFirstName())) {
                existing.setFirstName(claims.givenName());
                changed = true;
            }
            if (claims.familyName() != null && !claims.familyName().equals(existing.getLastName())) {
                existing.setLastName(claims.familyName());
                changed = true;
            }
            if (claims.pictureUrl() != null && !claims.pictureUrl().equals(existing.getPhotoUrl())) {
                existing.setPhotoUrl(claims.pictureUrl());
                changed = true;
            }
            if (changed) {
                userDao.updateProfile(
                        existing.getUserId(),
                        existing.getFirstName(),
                        existing.getLastName(),
                        existing.getPhone(),
                        existing.getPhotoUrl(),
                        existing.getAboutMeAsTeacher(),
                        existing.getAboutMeAsStudent());
            }
            return existing;
        }).orElseGet(() -> {
            UserEntity created = UserEntity.builder()
                    .email(claims.email())
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .firstName(claims.givenName() == null ? "Google" : claims.givenName())
                    .lastName(claims.familyName() == null ? "User" : claims.familyName())
                    .photoUrl(claims.pictureUrl())
                    .secretQuestion("Google account")
                    .secretAnswerHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .isAdmin(false)
                    .isActive(true)
                    .availableBalance(BigDecimal.ZERO)
                    .lockedBalance(BigDecimal.ZERO)
                    .build();
            Integer id = userDao.create(created);
            created.setUserId(id);
            return created;
        });

        return AuthPayloadDto.builder()
                .token(jwtProvider.generateToken(user.getUserId(), user.getEmail()))
                .user(toSummary(user))
                .build();
    }

    public UserSummaryDto verifyToken(Integer userId) {
        UserEntity user = userDao.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "User not found"));
        return toSummary(user);
    }

    private UserSummaryDto toSummary(UserEntity user) {
        return UserSummaryDto.builder()
                .id(user.getUserId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .photoUrl(user.getPhotoUrl())
                .isAdmin(user.getIsAdmin())
                .build();
    }
}

package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.LessonDao;
import com.tokenlearn.server.dao.LessonRequestDao;
import com.tokenlearn.server.dao.TokenTransactionDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.CourseEntity;
import com.tokenlearn.server.domain.LessonRequestEntity;
import com.tokenlearn.server.domain.TokenTransactionEntity;
import com.tokenlearn.server.domain.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenServiceTest {
    private UserDao userDao;
    private TokenTransactionDao transactionDao;
    private LessonRequestDao lessonRequestDao;
    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        userDao = mock(UserDao.class);
        transactionDao = mock(TokenTransactionDao.class);
        lessonRequestDao = mock(LessonRequestDao.class);
        LessonDao lessonDao = mock(LessonDao.class);
        CourseDao courseDao = mock(CourseDao.class);

        tokenService = new TokenService(userDao, transactionDao, lessonRequestDao, lessonDao, courseDao);

        when(userDao.findById(9)).thenReturn(Optional.of(UserEntity.builder()
                .userId(9)
                .firstName("Dana")
                .lastName("Levi")
                .build()));

        when(courseDao.findById(4)).thenReturn(Optional.of(CourseEntity.builder()
                .courseId(4)
                .courseNumber("20465")
                .nameEn("Linear Algebra")
                .build()));
    }

    @Test
    void historyIncludesLessonContextForRequestTransactions() {
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 3, 10, 18, 30);

        when(transactionDao.findByUser(7, 20, 0)).thenReturn(List.of(TokenTransactionEntity.builder()
                .txId(15L)
                .requestId(22)
                .payerId(7)
                .receiverId(7)
                .amount(new BigDecimal("2.00"))
                .txType("RESERVATION")
                .description("Tokens reserved for lesson request")
                .createdAt(LocalDateTime.of(2026, 3, 8, 11, 0))
                .build()));
        when(transactionDao.countByUser(7)).thenReturn(1);
        when(lessonRequestDao.findById(22)).thenReturn(Optional.of(LessonRequestEntity.builder()
                .requestId(22)
                .tutorId(9)
                .courseId(4)
                .specificStartTime(scheduledAt)
                .build()));

        Map<String, Object> result = tokenService.history(7, 20, 0);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> transactions = (List<Map<String, Object>>) result.get("transactions");
        assertEquals(1, transactions.size());
        Map<String, Object> item = transactions.get(0);
        assertEquals("20465 - Linear Algebra", item.get("courseLabel"));
        assertEquals("Dana Levi", item.get("tutorName"));
        assertEquals(scheduledAt, item.get("scheduledAt"));
        assertNotNull(item.get("requestId"));
    }
}

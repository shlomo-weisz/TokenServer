package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.LessonDao;
import com.tokenlearn.server.dao.LessonRequestDao;
import com.tokenlearn.server.dao.TokenTransactionDao;
import com.tokenlearn.server.dao.TutorDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.LessonRequestEntity;
import com.tokenlearn.server.domain.TokenTransactionEntity;
import com.tokenlearn.server.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LessonRequestServiceTest {
    private LessonRequestDao lessonRequestDao;
    private UserDao userDao;
    private TokenTransactionDao tokenTransactionDao;
    private LessonDao lessonDao;
    private NotificationService notificationService;
    private LessonRequestService service;

    @BeforeEach
    void setUp() {
        lessonRequestDao = mock(LessonRequestDao.class);
        userDao = mock(UserDao.class);
        CourseDao courseDao = mock(CourseDao.class);
        TutorDao tutorDao = mock(TutorDao.class);
        tokenTransactionDao = mock(TokenTransactionDao.class);
        lessonDao = mock(LessonDao.class);
        notificationService = mock(NotificationService.class);

        service = new LessonRequestService(
                lessonRequestDao,
                userDao,
                courseDao,
                tutorDao,
                tokenTransactionDao,
                lessonDao,
                notificationService,
                6);
    }

    @Test
    void expirePendingRequestsRefundsLockedTokens() {
        LessonRequestEntity expiredRequest = pendingRequest(101, LocalDateTime.now().plusHours(5));

        when(lessonRequestDao.findPendingExpiringBefore(any(LocalDateTime.class))).thenReturn(List.of(expiredRequest));
        when(lessonRequestDao.transitionStatus(101, "PENDING", "EXPIRED")).thenReturn(1);
        when(userDao.refundTokens(11, new BigDecimal("3.00"))).thenReturn(true);

        service.expirePendingRequests();

        verify(lessonRequestDao).transitionStatus(101, "PENDING", "EXPIRED");
        verify(userDao).refundTokens(11, new BigDecimal("3.00"));
        verify(tokenTransactionDao).create(any(TokenTransactionEntity.class));
        verify(notificationService, never()).createLessonRequestApprovedNotification(any(), any(), any());
    }

    @Test
    void approveExpiredRequestThrowsRequestExpiredAndRefundsTokens() {
        LessonRequestEntity expiredRequest = pendingRequest(202, LocalDateTime.now().plusHours(2));

        when(lessonRequestDao.findById(202)).thenReturn(Optional.of(expiredRequest));
        when(lessonRequestDao.transitionStatus(202, "PENDING", "EXPIRED")).thenReturn(1);
        when(userDao.refundTokens(11, new BigDecimal("3.00"))).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> service.approve(202, 99));

        assertEquals("REQUEST_EXPIRED", ex.getCode());
        verify(lessonRequestDao).transitionStatus(202, "PENDING", "EXPIRED");
        verify(userDao).refundTokens(11, new BigDecimal("3.00"));
        verify(tokenTransactionDao).create(any(TokenTransactionEntity.class));
        verify(lessonDao, never()).create(any());
        verify(notificationService, never()).createLessonRequestApprovedNotification(any(), any(), any());
    }

    private LessonRequestEntity pendingRequest(Integer requestId, LocalDateTime specificStartTime) {
        return LessonRequestEntity.builder()
                .requestId(requestId)
                .studentId(11)
                .tutorId(99)
                .courseId(7)
                .tokenCost(new BigDecimal("3.00"))
                .status("PENDING")
                .specificStartTime(specificStartTime)
                .specificEndTime(specificStartTime.plusHours(1))
                .build();
    }
}

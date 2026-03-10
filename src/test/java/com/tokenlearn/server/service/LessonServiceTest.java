package com.tokenlearn.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.LessonDao;
import com.tokenlearn.server.dao.LessonRequestDao;
import com.tokenlearn.server.dao.OutboxDao;
import com.tokenlearn.server.dao.RatingDao;
import com.tokenlearn.server.dao.TokenTransactionDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.LessonEntity;
import com.tokenlearn.server.domain.LessonRequestEntity;
import com.tokenlearn.server.domain.OutboxEventEntity;
import com.tokenlearn.server.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LessonServiceTest {
    private LessonDao lessonDao;
    private LessonRequestDao lessonRequestDao;
    private OutboxDao outboxDao;
    private LessonService lessonService;

    @BeforeEach
    void setUp() {
        lessonDao = mock(LessonDao.class);
        lessonRequestDao = mock(LessonRequestDao.class);
        outboxDao = mock(OutboxDao.class);
        UserDao userDao = mock(UserDao.class);
        CourseDao courseDao = mock(CourseDao.class);
        TokenTransactionDao tokenTransactionDao = mock(TokenTransactionDao.class);
        RatingDao ratingDao = mock(RatingDao.class);
        NotificationService notificationService = mock(NotificationService.class);

        lessonService = new LessonService(
                lessonDao,
                lessonRequestDao,
                outboxDao,
                userDao,
                courseDao,
                tokenTransactionDao,
                ratingDao,
                new ObjectMapper(),
                notificationService);
    }

    @Test
    void completeLessonFailsBeforeLessonEnd() {
        LessonEntity lesson = scheduledLesson(
                301,
                41,
                12,
                77,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().plusMinutes(55));

        when(lessonDao.findById(301)).thenReturn(Optional.of(lesson));

        AppException ex = assertThrows(AppException.class, () -> lessonService.completeLesson(301, 12));

        assertEquals("LESSON_NOT_FINISHED", ex.getCode());
        verify(lessonRequestDao, never()).findById(any());
        verify(lessonDao, never()).transitionStatus(any(), anyString(), anyString());
        verify(outboxDao, never()).create(any());
    }

    @Test
    void autoCompletePastLessonsMarksLessonCompletedAndCreatesOutboxEvent() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        LessonEntity lesson = scheduledLesson(501, 81, 33, 44, start, end);
        LessonRequestEntity request = LessonRequestEntity.builder()
                .requestId(81)
                .studentId(33)
                .tutorId(44)
                .tokenCost(new BigDecimal("2.50"))
                .status("APPROVED")
                .specificStartTime(start)
                .specificEndTime(end)
                .build();

        when(lessonDao.findScheduledEndingBefore(any(LocalDateTime.class))).thenReturn(List.of(lesson));
        when(lessonRequestDao.findById(81)).thenReturn(Optional.of(request));
        when(lessonDao.transitionStatus(501, "SCHEDULED", "COMPLETED")).thenReturn(1);

        lessonService.autoCompletePastLessons();

        verify(lessonDao).transitionStatus(501, "SCHEDULED", "COMPLETED");
        verify(lessonRequestDao).updateStatus(81, "COMPLETED");

        ArgumentCaptor<OutboxEventEntity> outboxCaptor = ArgumentCaptor.forClass(OutboxEventEntity.class);
        verify(outboxDao).create(outboxCaptor.capture());

        OutboxEventEntity event = outboxCaptor.getValue();
        assertEquals("LESSON", event.getAggregateType());
        assertEquals(501, event.getAggregateId());
        assertEquals("LESSON_COMPLETED", event.getEventType());
        assertEquals("NEW", event.getStatus());
        assertNotNull(event.getMessageId());
        assertNotNull(event.getPayloadJson());
    }

    @Test
    void completeLessonReturnsCompletedPayloadIfAlreadyCompletedDuringRace() {
        LocalDateTime end = LocalDateTime.now().minusMinutes(1);
        LessonEntity lesson = scheduledLesson(701, 91, 19, 28, end.minusHours(1), end);
        LessonEntity completedLesson = LessonEntity.builder()
                .lessonId(701)
                .requestId(91)
                .studentId(19)
                .tutorId(28)
                .tokenCost(new BigDecimal("1.00"))
                .startTime(end.minusHours(1))
                .endTime(end)
                .status("COMPLETED")
                .updatedAt(end.plusMinutes(1))
                .build();
        LessonRequestEntity request = LessonRequestEntity.builder()
                .requestId(91)
                .studentId(19)
                .tutorId(28)
                .tokenCost(new BigDecimal("1.00"))
                .status("COMPLETED")
                .build();

        when(lessonDao.findById(701)).thenReturn(Optional.of(lesson), Optional.of(completedLesson));
        when(lessonRequestDao.findById(91)).thenReturn(Optional.of(request));
        when(lessonDao.transitionStatus(701, "SCHEDULED", "COMPLETED")).thenReturn(0);

        Map<String, Object> result = lessonService.completeLesson(701, 19);

        assertEquals("completed", result.get("status"));
        assertEquals(701, result.get("id"));
        verify(outboxDao, never()).create(any());
    }

    private LessonEntity scheduledLesson(
            Integer lessonId,
            Integer requestId,
            Integer studentId,
            Integer tutorId,
            LocalDateTime startTime,
            LocalDateTime endTime) {
        return LessonEntity.builder()
                .lessonId(lessonId)
                .requestId(requestId)
                .studentId(studentId)
                .tutorId(tutorId)
                .tokenCost(new BigDecimal("1.00"))
                .startTime(startTime)
                .endTime(endTime)
                .status("SCHEDULED")
                .updatedAt(startTime)
                .build();
    }
}

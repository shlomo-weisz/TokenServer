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
import com.tokenlearn.server.domain.RatingEntity;
import com.tokenlearn.server.domain.UserEntity;
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
    private UserDao userDao;
    private CourseDao courseDao;
    private RatingDao ratingDao;
    private LessonService lessonService;

    @BeforeEach
    void setUp() {
        lessonDao = mock(LessonDao.class);
        lessonRequestDao = mock(LessonRequestDao.class);
        outboxDao = mock(OutboxDao.class);
        userDao = mock(UserDao.class);
        courseDao = mock(CourseDao.class);
        TokenTransactionDao tokenTransactionDao = mock(TokenTransactionDao.class);
        ratingDao = mock(RatingDao.class);
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
    void cancelLessonFailsAfterLessonEnd() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusMinutes(5);
        LessonEntity lesson = scheduledLesson(801, 61, 12, 77, start, end);

        when(lessonDao.findById(801)).thenReturn(Optional.of(lesson));

        AppException ex = assertThrows(AppException.class, () -> lessonService.cancelLesson(801, 12, "Too late"));

        assertEquals("LESSON_ALREADY_ENDED", ex.getCode());
        verify(lessonRequestDao, never()).findById(any());
        verify(lessonDao, never()).updateStatus(any(), anyString());
        verify(outboxDao, never()).create(any());
    }

    @Test
    void cancelLessonFailsWhenStudentTriesToCancelApprovedLesson() {
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end = start.plusHours(1);
        LessonEntity lesson = scheduledLesson(802, 62, 12, 77, start, end);

        when(lessonDao.findById(802)).thenReturn(Optional.of(lesson));

        AppException ex = assertThrows(AppException.class, () -> lessonService.cancelLesson(802, 12, "Student cancel"));

        assertEquals("ONLY_TUTOR_CAN_CANCEL_SCHEDULED_LESSON", ex.getCode());
        verify(lessonRequestDao, never()).findById(any());
        verify(lessonDao, never()).updateStatus(any(), anyString());
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

    @Test
    void detailsReturnsExistingUserRating() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        LessonEntity lesson = LessonEntity.builder()
                .lessonId(901)
                .requestId(71)
                .studentId(12)
                .tutorId(77)
                .courseId(null)
                .tokenCost(new BigDecimal("1.00"))
                .startTime(start)
                .endTime(end)
                .status("COMPLETED")
                .updatedAt(end)
                .build();
        LessonRequestEntity request = LessonRequestEntity.builder()
                .requestId(71)
                .message("Need help with homework")
                .build();
        RatingEntity existingRating = RatingEntity.builder()
                .ratingId(33)
                .lessonId(901)
                .fromUserId(12)
                .toUserId(77)
                .score(new BigDecimal("4.50"))
                .comment("Very clear explanations")
                .createdAt(end.plusMinutes(10))
                .build();

        when(lessonDao.findById(901)).thenReturn(Optional.of(lesson));
        when(lessonRequestDao.findById(71)).thenReturn(Optional.of(request));
        when(userDao.findById(12)).thenReturn(Optional.of(user(12, "Dana", "Student")));
        when(userDao.findById(77)).thenReturn(Optional.of(user(77, "Ron", "Tutor")));
        when(ratingDao.findByLessonAndFromUser(901, 12)).thenReturn(Optional.of(existingRating));

        Map<String, Object> result = lessonService.details(901, 12);

        @SuppressWarnings("unchecked")
        Map<String, Object> myRating = (Map<String, Object>) result.get("myRating");
        assertEquals(end.plusMinutes(10), result.get("ratedAt"));
        assertEquals(end.plusMinutes(10).plusHours(1), result.get("ratingEditableUntil"));
        assertEquals(new BigDecimal("4.50"), myRating.get("rating"));
        assertEquals("Very clear explanations", myRating.get("comment"));
    }

    @Test
    void updateLessonRatingSucceedsWithinOneHour() {
        LocalDateTime start = LocalDateTime.now().minusHours(2);
        LocalDateTime end = LocalDateTime.now().minusHours(1);
        LessonEntity lesson = LessonEntity.builder()
                .lessonId(951)
                .requestId(88)
                .studentId(12)
                .tutorId(77)
                .tokenCost(new BigDecimal("1.00"))
                .startTime(start)
                .endTime(end)
                .status("COMPLETED")
                .updatedAt(end)
                .build();
        RatingEntity existingRating = RatingEntity.builder()
                .ratingId(45)
                .lessonId(951)
                .fromUserId(12)
                .toUserId(77)
                .score(new BigDecimal("4.00"))
                .comment("Old")
                .createdAt(LocalDateTime.now().minusMinutes(20))
                .build();
        com.tokenlearn.server.dto.RateLessonRequest request = new com.tokenlearn.server.dto.RateLessonRequest();
        request.setRating(new BigDecimal("5.00"));
        request.setComment("Updated");

        when(lessonDao.findById(951)).thenReturn(Optional.of(lesson));
        when(ratingDao.findByLessonAndFromUser(951, 12)).thenReturn(Optional.of(existingRating));
        when(ratingDao.update(45, new BigDecimal("5.00"), "Updated")).thenReturn(1);

        Map<String, Object> result = lessonService.updateLessonRating(951, 12, request);

        assertEquals(new BigDecimal("5.00"), result.get("rating"));
        assertEquals("Updated", result.get("comment"));
        assertEquals(existingRating.getCreatedAt(), result.get("ratedAt"));
        assertEquals(existingRating.getCreatedAt().plusHours(1), result.get("ratingEditableUntil"));
    }

    @Test
    void updateLessonRatingFailsAfterOneHourWindow() {
        LocalDateTime start = LocalDateTime.now().minusHours(4);
        LocalDateTime end = LocalDateTime.now().minusHours(3);
        LessonEntity lesson = LessonEntity.builder()
                .lessonId(952)
                .requestId(89)
                .studentId(12)
                .tutorId(77)
                .tokenCost(new BigDecimal("1.00"))
                .startTime(start)
                .endTime(end)
                .status("COMPLETED")
                .updatedAt(end)
                .build();
        RatingEntity existingRating = RatingEntity.builder()
                .ratingId(46)
                .lessonId(952)
                .fromUserId(12)
                .toUserId(77)
                .score(new BigDecimal("4.00"))
                .comment("Old")
                .createdAt(LocalDateTime.now().minusHours(2))
                .build();
        com.tokenlearn.server.dto.RateLessonRequest request = new com.tokenlearn.server.dto.RateLessonRequest();
        request.setRating(new BigDecimal("5.00"));
        request.setComment("Updated");

        when(lessonDao.findById(952)).thenReturn(Optional.of(lesson));
        when(ratingDao.findByLessonAndFromUser(952, 12)).thenReturn(Optional.of(existingRating));

        AppException ex = assertThrows(AppException.class, () -> lessonService.updateLessonRating(952, 12, request));

        assertEquals("RATING_EDIT_WINDOW_EXPIRED", ex.getCode());
        verify(ratingDao, never()).update(any(), any(), any());
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

    private UserEntity user(Integer userId, String firstName, String lastName) {
        return UserEntity.builder()
                .userId(userId)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}

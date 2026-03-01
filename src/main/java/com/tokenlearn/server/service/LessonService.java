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
import com.tokenlearn.server.domain.TokenTransactionEntity;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.RateLessonRequest;
import com.tokenlearn.server.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LessonService {
    private final LessonDao lessonDao;
    private final LessonRequestDao lessonRequestDao;
    private final OutboxDao outboxDao;
    private final UserDao userDao;
    private final CourseDao courseDao;
    private final TokenTransactionDao tokenTransactionDao;
    private final RatingDao ratingDao;
    private final ObjectMapper objectMapper;

    public LessonService(LessonDao lessonDao,
            LessonRequestDao lessonRequestDao,
            OutboxDao outboxDao,
            UserDao userDao,
            CourseDao courseDao,
            TokenTransactionDao tokenTransactionDao,
            RatingDao ratingDao,
            ObjectMapper objectMapper) {
        this.lessonDao = lessonDao;
        this.lessonRequestDao = lessonRequestDao;
        this.outboxDao = outboxDao;
        this.userDao = userDao;
        this.courseDao = courseDao;
        this.tokenTransactionDao = tokenTransactionDao;
        this.ratingDao = ratingDao;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> completeLesson(Integer lessonId, Integer actorId) {
        LessonEntity lesson = requireLesson(lessonId);
        if (!isParticipant(lesson, actorId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only lesson participants can complete");
        }
        if (!"SCHEDULED".equals(lesson.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE", "Only scheduled lessons can be completed");
        }
        LessonRequestEntity request = requireRequest(lesson.getRequestId());

        lessonDao.updateStatus(lessonId, "COMPLETED");
        lessonRequestDao.updateStatus(request.getRequestId(), "COMPLETED");

        String messageId = UUID.randomUUID().toString();
        Map<String, Object> payload = Map.of(
                "lessonId", lessonId,
                "requestId", request.getRequestId(),
                "studentId", request.getStudentId(),
                "tutorId", request.getTutorId(),
                "amount", request.getTokenCost(),
                "timestamp", LocalDateTime.now().toString(),
                "messageId", messageId);
        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "SERIALIZATION_ERROR", "Failed to serialize settlement payload");
        }

        outboxDao.create(OutboxEventEntity.builder()
                .aggregateType("LESSON")
                .aggregateId(lessonId)
                .eventType("LESSON_COMPLETED")
                .payloadJson(payloadJson)
                .messageId(messageId)
                .status("NEW")
                .build());

        return Map.of(
                "id", lessonId,
                "status", "completed",
                "completedAt", LocalDateTime.now(),
                "tokenSettlement", Map.of(
                        "studentLockedDebited", request.getTokenCost(),
                        "tutorAvailableCredited", request.getTokenCost()));
    }

    @Transactional
    public Map<String, Object> cancelLesson(Integer lessonId, Integer actorId, String reason) {
        LessonEntity lesson = requireLesson(lessonId);
        if (!isParticipant(lesson, actorId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only lesson participants can cancel");
        }
        if (!"SCHEDULED".equals(lesson.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE", "Only scheduled lessons can be cancelled");
        }

        LessonRequestEntity request = requireRequest(lesson.getRequestId());

        lessonDao.updateStatus(lessonId, "CANCELLED");
        lessonRequestDao.updateStatusWithRejection(request.getRequestId(), "CANCELLED", reason);

        if (!tokenTransactionDao.refundExists(request.getRequestId())) {
            boolean refunded = userDao.refundTokens(request.getStudentId(), request.getTokenCost());
            if (!refunded) {
                throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE", "Failed to release locked tokens");
            }
            tokenTransactionDao.create(TokenTransactionEntity.builder()
                    .requestId(request.getRequestId())
                    .lessonId(lessonId)
                    .payerId(request.getStudentId())
                    .receiverId(request.getStudentId())
                    .amount(request.getTokenCost())
                    .txType("REFUND")
                    .status("SUCCESS")
                    .description("Refund due to lesson cancellation")
                    .build());
        }

        return Map.of(
                "id", lessonId,
                "status", "cancelled",
                "cancelledAt", LocalDateTime.now(),
                "refundedTokens", request.getTokenCost(),
                "tokenSettlement", Map.of("lockedReleasedToStudentAvailable", request.getTokenCost()));
    }

    public List<Map<String, Object>> upcoming(Integer userId, String role) {
        return lessonDao.findUpcomingByUser(userId, role).stream().map(lesson -> {
            boolean isTeacher = lesson.getTutorId().equals(userId);
            Integer withUserId = isTeacher ? lesson.getStudentId() : lesson.getTutorId();
            UserEntity withUser = userDao.findById(withUserId).orElse(null);
            String courseName = courseDao.findById(lesson.getCourseId()).map(c -> c.getName()).orElse("");
            return Map.of(
                    "id", lesson.getLessonId(),
                    "role", isTeacher ? "teacher" : "student",
                    "withUserId", withUserId,
                    "withUserName", withUser == null ? "" : withUser.getFirstName() + " " + withUser.getLastName(),
                    "topic", courseName,
                    "dateTime", lesson.getStartTime(),
                    "tokenCost", lesson.getTokenCost(),
                    "status", lesson.getStatus().toLowerCase());
        }).toList();
    }

    public Map<String, Object> details(Integer lessonId, Integer userId) {
        LessonEntity lesson = requireLesson(lessonId);
        if (!isParticipant(lesson, userId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only lesson participants can access details");
        }
        LessonRequestEntity request = requireRequest(lesson.getRequestId());
        UserEntity student = userDao.findById(lesson.getStudentId()).orElse(null);
        UserEntity tutor = userDao.findById(lesson.getTutorId()).orElse(null);
        String courseName = courseDao.findById(lesson.getCourseId()).map(c -> c.getName()).orElse("");
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", lesson.getLessonId());
        result.put("requestId", lesson.getRequestId());
        result.put("studentId", lesson.getStudentId());
        result.put("studentName", student == null ? "" : student.getFirstName() + " " + student.getLastName());
        result.put("tutorId", lesson.getTutorId());
        result.put("tutorName", tutor == null ? "" : tutor.getFirstName() + " " + tutor.getLastName());
        result.put("course", courseName);
        result.put("startTime", lesson.getStartTime());
        result.put("endTime", lesson.getEndTime());
        result.put("status", lesson.getStatus().toLowerCase());
        result.put("tokenCost", lesson.getTokenCost());
        result.put("message", request.getMessage());
        return result;
    }

    public List<Map<String, Object>> history(Integer userId, int limit, int offset) {
        return lessonDao.findHistoryByUser(userId, limit, offset).stream().map(lesson -> {
            boolean asTeacher = lesson.getTutorId().equals(userId);
            Integer withUserId = asTeacher ? lesson.getStudentId() : lesson.getTutorId();
            UserEntity withUser = userDao.findById(withUserId).orElse(null);
            String courseName = courseDao.findById(lesson.getCourseId()).map(c -> c.getName()).orElse("");
            return Map.of(
                    "id", lesson.getLessonId(),
                    "role", asTeacher ? "teacher" : "student",
                    "withUserId", withUserId,
                    "withUserName", withUser == null ? "" : withUser.getFirstName() + " " + withUser.getLastName(),
                    "topic", courseName,
                    "dateTime", lesson.getStartTime(),
                    "tokenCost", lesson.getTokenCost(),
                    "status", lesson.getStatus().toLowerCase());
        }).toList();
    }

    @Transactional
    public Map<String, Object> rateLesson(Integer lessonId, Integer fromUserId, RateLessonRequest request) {
        LessonEntity lesson = requireLesson(lessonId);
        if (!"COMPLETED".equals(lesson.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE", "Only completed lessons can be rated");
        }
        if (!isParticipant(lesson, fromUserId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only participants can rate lesson");
        }
        Integer toUserId = lesson.getStudentId().equals(fromUserId) ? lesson.getTutorId() : lesson.getStudentId();
        if (request.getRating().compareTo(BigDecimal.ONE) < 0 || request.getRating().compareTo(new BigDecimal("5")) > 0) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_RATING", "Rating must be between 1 and 5");
        }
        try {
            ratingDao.create(RatingEntity.builder()
                    .lessonId(lessonId)
                    .fromUserId(fromUserId)
                    .toUserId(toUserId)
                    .score(request.getRating())
                    .comment(request.getComment())
                    .build());
        } catch (Exception ex) {
            throw new AppException(HttpStatus.CONFLICT, "ALREADY_RATED", "Lesson already rated by this user");
        }
        return Map.of(
                "lessonId", lessonId,
                "rating", request.getRating(),
                "comment", request.getComment() == null ? "" : request.getComment(),
                "ratedAt", LocalDateTime.now());
    }

    private LessonEntity requireLesson(Integer lessonId) {
        return lessonDao.findById(lessonId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Lesson not found"));
    }

    private LessonRequestEntity requireRequest(Integer requestId) {
        return lessonRequestDao.findById(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Lesson request not found"));
    }

    private boolean isParticipant(LessonEntity lesson, Integer userId) {
        return lesson.getStudentId().equals(userId) || lesson.getTutorId().equals(userId);
    }
}

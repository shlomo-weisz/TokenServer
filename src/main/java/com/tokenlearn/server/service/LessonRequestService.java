package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.LessonDao;
import com.tokenlearn.server.dao.LessonRequestDao;
import com.tokenlearn.server.dao.TokenTransactionDao;
import com.tokenlearn.server.dao.TutorDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.CourseEntity;
import com.tokenlearn.server.domain.LessonEntity;
import com.tokenlearn.server.domain.LessonRequestEntity;
import com.tokenlearn.server.domain.TokenTransactionEntity;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.CreateLessonRequestInputDto;
import com.tokenlearn.server.dto.RejectLessonRequestInputDto;
import com.tokenlearn.server.dto.RequestedSlotDto;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.util.CourseLabelUtil;
import com.tokenlearn.server.util.WeekdayUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class LessonRequestService {
    private final LessonRequestDao lessonRequestDao;
    private final UserDao userDao;
    private final CourseDao courseDao;
    private final TutorDao tutorDao;
    private final TokenTransactionDao tokenTransactionDao;
    private final LessonDao lessonDao;
    private final NotificationService notificationService;
    private final long approvalBufferHours;

    public LessonRequestService(LessonRequestDao lessonRequestDao,
            UserDao userDao,
            CourseDao courseDao,
            TutorDao tutorDao,
            TokenTransactionDao tokenTransactionDao,
            LessonDao lessonDao,
            NotificationService notificationService,
            @Value("${app.lesson-request-approval-buffer-hours:6}") long approvalBufferHours) {
        this.lessonRequestDao = lessonRequestDao;
        this.userDao = userDao;
        this.courseDao = courseDao;
        this.tutorDao = tutorDao;
        this.tokenTransactionDao = tokenTransactionDao;
        this.lessonDao = lessonDao;
        this.notificationService = notificationService;
        this.approvalBufferHours = approvalBufferHours;
    }

    @Transactional
    public Map<String, Object> create(Integer studentId, CreateLessonRequestInputDto request) {
        if (studentId.equals(request.getTutorId())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Student and tutor must be different");
        }
        UserEntity tutor = userDao.findById(request.getTutorId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tutor not found"));
        if (Boolean.TRUE.equals(tutor.getIsBlockedTutor())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TUTOR_BLOCKED", "Tutor is blocked");
        }

        Integer courseId = resolveCourseId(request);
        Slot slot = parseSlot(request.getRequestedSlot());

        LessonRequestEntity entity = LessonRequestEntity.builder()
                .studentId(studentId)
                .tutorId(request.getTutorId())
                .courseId(courseId)
                .tokenCost(request.getTokenCost())
                .requestedDay(slot.day())
                .requestedStartTime(slot.start())
                .requestedEndTime(slot.end())
                .specificStartTime(slot.specificStart())
                .specificEndTime(slot.specificEnd())
                .message(request.getMessage())
                .status("PENDING")
                .build();

        Integer requestId = lessonRequestDao.create(entity);
        entity.setRequestId(requestId);

        if (!userDao.reserveTokens(studentId, request.getTokenCost())) {
            throw new AppException(HttpStatus.PAYMENT_REQUIRED, "INSUFFICIENT_BALANCE", "Insufficient available balance");
        }

        tokenTransactionDao.create(TokenTransactionEntity.builder()
                .requestId(requestId)
                .payerId(studentId)
                .receiverId(studentId)
                .amount(request.getTokenCost())
                .txType("RESERVATION")
                .status("SUCCESS")
                .description("Tokens reserved for lesson request")
                .build());

        notificationService.createLessonRequestCreatedNotification(entity);

        return Map.of(
                "requestId", requestId,
                "status", "pending",
                "tokenCost", request.getTokenCost(),
                "tokenMovement", Map.of("fromAvailableToLocked", request.getTokenCost()));
    }

    @Transactional
    public Map<String, Object> approve(Integer requestId, Integer tutorId) {
        LessonRequestEntity req = requireRequest(requestId);
        if (!req.getTutorId().equals(tutorId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the tutor can approve this request");
        }
        expireIfNeeded(req);
        if (lessonRequestDao.transitionStatus(requestId, "PENDING", "APPROVED") != 1) {
            throwInvalidPendingTransition(requestId, "Request must be pending");
        }

        LocalDateTime start = req.getSpecificStartTime() != null ? req.getSpecificStartTime() : LocalDateTime.now().plusDays(1);
        LocalDateTime end = req.getSpecificEndTime() != null ? req.getSpecificEndTime() : start.plusHours(1);

        Integer lessonId = lessonDao.create(LessonEntity.builder()
                .requestId(req.getRequestId())
                .studentId(req.getStudentId())
                .tutorId(req.getTutorId())
                .courseId(req.getCourseId())
                .tokenCost(req.getTokenCost())
                .startTime(start)
                .endTime(end)
                .status("SCHEDULED")
                .build());

        notificationService.createLessonRequestApprovedNotification(req, lessonId, start);

        return Map.of("requestId", requestId, "status", "approved", "lessonId", lessonId);
    }

    @Transactional
    public Map<String, Object> reject(Integer requestId, Integer tutorId, RejectLessonRequestInputDto input) {
        LessonRequestEntity req = requireRequest(requestId);
        if (!req.getTutorId().equals(tutorId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the tutor can reject this request");
        }

        String reason = input.getRejectionMessage() != null ? input.getRejectionMessage()
                : (input.getReason() == null ? "No reason provided" : input.getReason());

        expireIfNeeded(req);
        if (lessonRequestDao.transitionStatusWithRejection(req.getRequestId(), "PENDING", "REJECTED", reason) != 1) {
            throwInvalidPendingTransition(req.getRequestId(), "Only pending requests can be rejected");
        }
        if (!userDao.refundTokens(req.getStudentId(), req.getTokenCost())) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE", "Failed to refund locked tokens");
        }
        tokenTransactionDao.create(TokenTransactionEntity.builder()
                .requestId(req.getRequestId())
                .payerId(req.getStudentId())
                .receiverId(req.getStudentId())
                .amount(req.getTokenCost())
                .txType("REFUND")
                .status("SUCCESS")
                .description("Tokens released because the lesson request was rejected")
                .build());
        notificationService.createLessonRequestRejectedNotification(req, reason);

        return Map.of(
                "requestId", req.getRequestId(),
                "status", "rejected",
                "rejectionReason", reason,
                "tokenMovement", Map.of("fromLockedToAvailable", req.getTokenCost()));
    }

    @Transactional
    public Map<String, Object> cancel(Integer requestId, Integer studentId) {
        LessonRequestEntity req = requireRequest(requestId);
        if (!req.getStudentId().equals(studentId)) {
            throw new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the student can cancel this request");
        }

        expireIfNeeded(req);
        if (lessonRequestDao.transitionStatus(req.getRequestId(), "PENDING", "CANCELLED") != 1) {
            throwInvalidPendingTransition(req.getRequestId(), "Only pending requests can be cancelled");
        }
        if (!userDao.refundTokens(req.getStudentId(), req.getTokenCost())) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE", "Failed to refund locked tokens");
        }
        tokenTransactionDao.create(TokenTransactionEntity.builder()
                .requestId(req.getRequestId())
                .payerId(req.getStudentId())
                .receiverId(req.getStudentId())
                .amount(req.getTokenCost())
                .txType("REFUND")
                .status("SUCCESS")
                .description("Tokens released because the lesson request was cancelled")
                .build());

        return Map.of(
                "message", "Lesson request cancelled",
                "tokenMovement", Map.of("fromLockedToAvailable", req.getTokenCost()));
    }

    @Scheduled(
            fixedDelayString = "${app.lesson-request-expiration-scan-ms:60000}",
            initialDelayString = "${app.lesson-request-expiration-initial-delay-ms:60000}")
    @Transactional
    public void expirePendingRequests() {
        LocalDateTime latestAllowedStartTime = approvalExpiryCutoff(LocalDateTime.now());
        lessonRequestDao.findPendingExpiringBefore(latestAllowedStartTime)
                .forEach(this::expirePendingRequest);
    }

    @Transactional
    public List<Map<String, Object>> listForStudent(Integer studentId, String status) {
        String normalizedStatus = normalizeStatus(status);
        return lessonRequestDao.findByStudent(studentId, normalizedStatus).stream()
                .map(this::refreshExpiredStateForListing)
                .filter(req -> matchesStatusFilter(req, normalizedStatus))
                .map(req -> {
            UserEntity tutor = userDao.findById(req.getTutorId()).orElse(null);
            String tutorName = tutor == null ? "" : tutor.getFirstName() + " " + tutor.getLastName();
            BigDecimal tutorRating = tutorDao.ratingForTutor(req.getTutorId());
            CourseEntity course = findCourse(req.getCourseId());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", req.getRequestId());
            out.put("tutorId", req.getTutorId());
            out.put("tutorName", tutorName.trim());
            out.put("tutorRating", tutorRating);
            putCourseFields(out, course);
            out.put("requestedSlot", slotMap(req));
            out.put("message", req.getMessage() == null ? "" : req.getMessage());
            out.put("status", req.getStatus().toLowerCase());
            out.put("rejectionReason", req.getRejectionMessage());
            out.put("requestedAt", req.getCreatedAt());
            out.put("lessonDateTime", req.getSpecificStartTime());
            return out;
        }).toList();
    }

    @Transactional
    public List<Map<String, Object>> listForTutor(Integer tutorId, String status) {
        String normalizedStatus = normalizeStatus(status);
        return lessonRequestDao.findByTutor(tutorId, normalizedStatus).stream()
                .map(this::refreshExpiredStateForListing)
                .filter(req -> matchesStatusFilter(req, normalizedStatus))
                .map(req -> {
            UserEntity student = userDao.findById(req.getStudentId()).orElse(null);
            String studentName = student == null ? "" : student.getFirstName() + " " + student.getLastName();
            CourseEntity course = findCourse(req.getCourseId());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", req.getRequestId());
            out.put("studentId", req.getStudentId());
            out.put("studentName", studentName.trim());
            putCourseFields(out, course);
            out.put("requestedSlot", slotMap(req));
            out.put("message", req.getMessage() == null ? "" : req.getMessage());
            out.put("status", req.getStatus().toLowerCase());
            out.put("rejectionReason", req.getRejectionMessage());
            out.put("requestedAt", req.getCreatedAt());
            out.put("lessonDateTime", req.getSpecificStartTime());
            return out;
        }).toList();
    }

    public LessonRequestEntity requireRequest(Integer requestId) {
        return lessonRequestDao.findById(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Lesson request not found"));
    }

    private Integer resolveCourseId(CreateLessonRequestInputDto request) {
        if (request.getCourseId() != null) {
            courseDao.findById(request.getCourseId())
                    .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "INVALID_COURSE", "Unknown courseId"));
            return request.getCourseId();
        }
        if (request.getCourse() == null || request.getCourse().isBlank()) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "courseId or course is required");
        }
        return courseDao.findByIdentifier(request.getCourse())
                .map(c -> c.getCourseId())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "INVALID_COURSE", "Unknown course"));
    }

    private Slot parseSlot(RequestedSlotDto slot) {
        if (slot == null) {
            return new Slot(null, null, null, null, null);
        }
        String normalizedDay = WeekdayUtil.normalizeToEnglishOrNull(slot.getDay());
        if (normalizedDay == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_DAY", "Requested day is invalid");
        }

        LocalTime start = slot.getStartTime() == null ? null : LocalTime.parse(slot.getStartTime());
        LocalTime end = slot.getEndTime() == null ? null : LocalTime.parse(slot.getEndTime());
        if (start == null || end == null || !end.isAfter(start)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_SLOT_WINDOW", "Requested slot start/end time is invalid");
        }

        LocalDateTime specificStart = parseDateTimeFlexible(slot.getSpecificStartTime(), normalizedDay);
        LocalDateTime specificEnd = parseDateTimeFlexible(slot.getSpecificEndTime(), normalizedDay);
        if (specificStart == null || specificEnd == null || !specificEnd.isAfter(specificStart)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_SPECIFIC_TIME", "Specific lesson time is invalid");
        }
        if (!specificStart.toLocalDate().equals(specificEnd.toLocalDate())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "INVALID_SPECIFIC_TIME", "Lesson must start and end on the same date");
        }

        String specificDay = specificStart.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        if (!specificDay.equals(normalizedDay)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "DAY_DATE_MISMATCH", "Selected date does not match requested day");
        }
        if (specificStart.toLocalTime().isBefore(start) || specificEnd.toLocalTime().isAfter(end)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "TIME_OUT_OF_RANGE", "Selected time is outside tutor availability window");
        }
        if (!specificStart.isAfter(LocalDateTime.now().plusHours(approvalBufferHours))) {
            throw new AppException(
                    HttpStatus.BAD_REQUEST,
                    "LESSON_TOO_SOON",
                    "Lesson must be scheduled more than " + approvalBufferHours + " hours in advance");
        }

        return new Slot(normalizedDay, start, end, specificStart, specificEnd);
    }

    private LocalDateTime parseDateTimeFlexible(String value, String dayEnglish) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() <= 5) {
            LocalDate date = nextOrSameByEnglishDay(dayEnglish);
            return LocalDateTime.of(date, LocalTime.parse(value));
        }
        return LocalDateTime.parse(value);
    }

    private LocalDate nextOrSameByEnglishDay(String dayEnglish) {
        DayOfWeek target = DayOfWeek.valueOf(dayEnglish.toUpperCase(Locale.ROOT));
        LocalDate date = LocalDate.now();
        while (date.getDayOfWeek() != target) {
            date = date.plusDays(1);
        }
        return date;
    }

    private Map<String, Object> slotMap(LessonRequestEntity req) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("day", WeekdayUtil.normalizeToEnglishOrNull(req.getRequestedDay()));
        out.put("startTime", req.getRequestedStartTime() == null ? null : req.getRequestedStartTime().toString());
        out.put("endTime", req.getRequestedEndTime() == null ? null : req.getRequestedEndTime().toString());
        out.put("specificStartTime", req.getSpecificStartTime());
        out.put("specificEndTime", req.getSpecificEndTime());
        return out;
    }

    private String normalizeStatus(String status) {
        return status == null || status.isBlank() ? null : status.toUpperCase();
    }

    private LessonRequestEntity refreshExpiredStateForListing(LessonRequestEntity req) {
        if (!isPendingAndExpired(req)) {
            return req;
        }
        expirePendingRequest(req);
        return lessonRequestDao.findById(req.getRequestId()).orElseGet(() -> {
            req.setStatus("EXPIRED");
            return req;
        });
    }

    private boolean matchesStatusFilter(LessonRequestEntity req, String normalizedStatus) {
        return normalizedStatus == null || normalizedStatus.equalsIgnoreCase(req.getStatus());
    }

    private void expireIfNeeded(LessonRequestEntity req) {
        if (!isPendingAndExpired(req)) {
            return;
        }
        expirePendingRequest(req);
        throw new AppException(HttpStatus.CONFLICT, "REQUEST_EXPIRED", "Request approval window has expired");
    }

    private boolean isPendingAndExpired(LessonRequestEntity req) {
        return "PENDING".equals(req.getStatus())
                && req.getSpecificStartTime() != null
                && !req.getSpecificStartTime().isAfter(approvalExpiryCutoff(LocalDateTime.now()));
    }

    private LocalDateTime approvalExpiryCutoff(LocalDateTime now) {
        return now.plusHours(approvalBufferHours);
    }

    private void expirePendingRequest(LessonRequestEntity req) {
        if (lessonRequestDao.transitionStatus(req.getRequestId(), "PENDING", "EXPIRED") != 1) {
            return;
        }
        if (!userDao.refundTokens(req.getStudentId(), req.getTokenCost())) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE", "Failed to refund locked tokens for expired request");
        }
        tokenTransactionDao.create(TokenTransactionEntity.builder()
                .requestId(req.getRequestId())
                .payerId(req.getStudentId())
                .receiverId(req.getStudentId())
                .amount(req.getTokenCost())
                .txType("REFUND")
                .status("SUCCESS")
                .description("Tokens released because the lesson request expired")
                .build());
    }

    private void throwInvalidPendingTransition(Integer requestId, String defaultMessage) {
        LessonRequestEntity current = requireRequest(requestId);
        if ("EXPIRED".equals(current.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "REQUEST_EXPIRED", "Request approval window has expired");
        }
        throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE", defaultMessage);
    }

    private CourseEntity findCourse(Integer courseId) {
        return courseId == null ? null : courseDao.findById(courseId).orElse(null);
    }

    private void putCourseFields(Map<String, Object> target, CourseEntity course) {
        String courseLabel = course == null ? "" : CourseLabelUtil.buildLabel(course);
        target.put("course", courseLabel);
        target.put("courseLabel", courseLabel);
        target.put("courseId", course == null ? null : course.getCourseId());
        target.put("courseNumber", course == null ? null : course.getCourseNumber());
        target.put("courseNameHe", course == null ? null : course.getNameHe());
        target.put("courseNameEn", course == null ? null : course.getNameEn());
    }

    private record Slot(String day, LocalTime start, LocalTime end, LocalDateTime specificStart, LocalDateTime specificEnd) {
    }
}

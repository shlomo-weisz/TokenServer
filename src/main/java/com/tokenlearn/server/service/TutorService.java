package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.AvailabilityDao;
import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.TutorDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.AvailabilityDto;
import com.tokenlearn.server.dto.SimpleCourseDto;
import com.tokenlearn.server.exception.AppException;
import com.tokenlearn.server.util.CourseLabelUtil;
import com.tokenlearn.server.util.WeekdayUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TutorService {
    private final TutorDao tutorDao;
    private final CourseDao courseDao;
    private final AvailabilityDao availabilityDao;
    private final UserDao userDao;

    public TutorService(TutorDao tutorDao, CourseDao courseDao, AvailabilityDao availabilityDao, UserDao userDao) {
        this.tutorDao = tutorDao;
        this.courseDao = courseDao;
        this.availabilityDao = availabilityDao;
        this.userDao = userDao;
    }

    public List<Map<String, Object>> recommended(Integer userId, int limit, BigDecimal minRating) {
        return enrichTutorRows(tutorDao.findRecommended(userId, limit, minRating));
    }

    public List<Map<String, Object>> search(Integer userId, String course, BigDecimal minRating, int limit) {
        return enrichTutorRows(tutorDao.searchTutors(userId, course, minRating, limit));
    }

    public Map<String, Object> profile(Integer tutorId) {
        UserEntity tutor = userDao.findById(tutorId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tutor not found"));
        List<SimpleCourseDto> courses = courseDao.findTeacherCourses(tutorId).stream()
                .map(c -> SimpleCourseDto.builder()
                        .id(c.getCourseId())
                        .courseNumber(c.getCourseNumber())
                        .nameHe(c.getNameHe())
                        .nameEn(c.getNameEn())
                        .name(CourseLabelUtil.buildLabel(c))
                        .build())
                .toList();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", tutor.getUserId());
        out.put("name", tutor.getFirstName() + " " + tutor.getLastName());
        out.put("email", tutor.getEmail());
        out.put("photoUrl", tutor.getPhotoUrl() == null ? "" : tutor.getPhotoUrl());
        out.put("aboutMeAsTeacher", tutor.getAboutMeAsTeacher() == null ? "" : tutor.getAboutMeAsTeacher());
        out.put("rating", tutorDao.ratingForTutor(tutorId));
        int lessons = userDao.countCompletedLessonsAsTutor(tutorId);
        out.put("lessons", lessons);
        out.put("totalLessonsAsTutor", lessons);
        out.put("courseOptions", courses);
        out.put("coursesAsTeacher", courses);
        out.put("courses", courses.stream().map(SimpleCourseDto::getName).toList());
        out.put("availabilityAsTeacher", availability(tutorId));
        return out;
    }

    public List<AvailabilityDto> availability(Integer tutorId) {
        return availabilityDao.findByUserAndRole(tutorId, "teacher").stream()
                .map(a -> AvailabilityDto.builder()
                        .id(a.getAvailabilityId())
                        .day(WeekdayUtil.normalizeToEnglishOrNull(a.getDay()))
                        .startTime(a.getStartTime().toString())
                        .endTime(a.getEndTime().toString())
                        .isAvailable(true)
                        .build())
                .toList();
    }

    private List<Map<String, Object>> enrichTutorRows(List<Map<String, Object>> rows) {
        return rows.stream().map(row -> {
            Integer tutorId = (Integer) row.get("id");
            List<SimpleCourseDto> courses = courseDao.findTeacherCourses(tutorId).stream()
                    .map(c -> SimpleCourseDto.builder()
                            .id(c.getCourseId())
                            .courseNumber(c.getCourseNumber())
                            .nameHe(c.getNameHe())
                            .nameEn(c.getNameEn())
                            .name(CourseLabelUtil.buildLabel(c))
                            .build())
                    .toList();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", tutorId);
            out.put("name", row.get("name"));
            out.put("rating", row.get("rating"));
            out.put("courseOptions", courses);
            out.put("coursesAsTeacher", courses);
            out.put("courses", courses.stream().map(SimpleCourseDto::getName).toList());
            out.put("photoUrl", row.get("photoUrl") == null ? "" : row.get("photoUrl"));
            out.put("aboutMeAsTeacher", row.get("aboutMeAsTeacher") == null ? "" : row.get("aboutMeAsTeacher"));
            int lessons = asInt(row.get("lessons"));
            out.put("lessons", lessons);
            out.put("totalLessonsAsTutor", lessons);
            out.put("availabilityAsTeacher", availability(tutorId));
            return out;
        }).toList();
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}

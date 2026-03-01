package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.AvailabilityDao;
import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dao.TutorDao;
import com.tokenlearn.server.dao.UserDao;
import com.tokenlearn.server.domain.UserEntity;
import com.tokenlearn.server.dto.AvailabilityDto;
import com.tokenlearn.server.dto.SimpleCourseDto;
import com.tokenlearn.server.exception.AppException;
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

    public List<Map<String, Object>> recommended(int limit, BigDecimal minRating) {
        return enrichTutorRows(tutorDao.findRecommended(limit, minRating));
    }

    public List<Map<String, Object>> search(String course, BigDecimal minRating, int limit) {
        return enrichTutorRows(tutorDao.searchTutors(course, minRating, limit));
    }

    public Map<String, Object> profile(Integer tutorId) {
        UserEntity tutor = userDao.findById(tutorId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Tutor not found"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", tutor.getUserId());
        out.put("name", tutor.getFirstName() + " " + tutor.getLastName());
        out.put("email", tutor.getEmail());
        out.put("photoUrl", tutor.getPhotoUrl() == null ? "" : tutor.getPhotoUrl());
        out.put("aboutMeAsTeacher", tutor.getAboutMeAsTeacher() == null ? "" : tutor.getAboutMeAsTeacher());
        out.put("rating", tutorDao.ratingForTutor(tutorId));
        out.put("courses", courseDao.findTeacherCourses(tutorId).stream().map(c -> c.getName()).toList());
        out.put("availabilityAsTeacher", availability(tutorId));
        return out;
    }

    public List<AvailabilityDto> availability(Integer tutorId) {
        return availabilityDao.findByUserAndRole(tutorId, "teacher").stream()
                .map(a -> AvailabilityDto.builder()
                        .id(a.getAvailabilityId())
                        .day(a.getDay())
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
                    .map(c -> SimpleCourseDto.builder().id(c.getCourseId()).name(c.getName()).build())
                    .toList();
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("id", tutorId);
            out.put("name", row.get("name"));
            out.put("rating", row.get("rating"));
            out.put("courses", courses.stream().map(SimpleCourseDto::getName).toList());
            out.put("photoUrl", row.get("photoUrl") == null ? "" : row.get("photoUrl"));
            out.put("availabilityAsTeacher", availability(tutorId));
            return out;
        }).toList();
    }
}

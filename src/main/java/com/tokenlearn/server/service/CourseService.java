package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dto.SimpleCourseDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class CourseService {
    private final CourseDao courseDao;

    public CourseService(CourseDao courseDao) {
        this.courseDao = courseDao;
    }

    public List<Map<String, Object>> getCourses(String search, String category) {
        return courseDao.findAll(search, category).stream()
                .map(c -> Map.of("id", c.getCourseId(), "name", c.getName(), "category", c.getCategory() == null ? "" : c.getCategory()))
                .toList();
    }

    public List<String> getCategories() {
        return courseDao.findCategories();
    }

    public List<SimpleCourseDto> getTeacherCourses(Integer userId) {
        return courseDao.findTeacherCourses(userId).stream()
                .map(c -> SimpleCourseDto.builder().id(c.getCourseId()).name(c.getName()).build())
                .toList();
    }
}

package com.tokenlearn.server.service;

import com.tokenlearn.server.dao.CourseDao;
import com.tokenlearn.server.dto.SimpleCourseDto;
import com.tokenlearn.server.util.CourseLabelUtil;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseService {
    private final CourseDao courseDao;

    public CourseService(CourseDao courseDao) {
        this.courseDao = courseDao;
    }

    public List<Map<String, Object>> getCourses(String search, String category, Integer limit) {
        int safeLimit = limit == null ? 5000 : Math.min(Math.max(limit, 1), 5000);
        return courseDao.findAll(search, category, safeLimit).stream()
                .map(c -> {
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("id", c.getCourseId());
                    out.put("courseNumber", c.getCourseNumber() == null ? "" : c.getCourseNumber());
                    out.put("nameHe", c.getNameHe() == null ? "" : c.getNameHe());
                    out.put("nameEn", c.getNameEn() == null ? "" : c.getNameEn());
                    out.put("name", c.getName());
                    out.put("label", CourseLabelUtil.buildLabel(c));
                    out.put("category", c.getCategory() == null ? "" : c.getCategory());
                    return out;
                })
                .toList();
    }

    public List<String> getCategories() {
        return courseDao.findCategories();
    }

    public List<SimpleCourseDto> getTeacherCourses(Integer userId) {
        return courseDao.findTeacherCourses(userId).stream()
                .map(c -> SimpleCourseDto.builder()
                        .id(c.getCourseId())
                        .courseNumber(c.getCourseNumber())
                        .nameHe(c.getNameHe())
                        .nameEn(c.getNameEn())
                        .name(CourseLabelUtil.buildLabel(c))
                        .build())
                .toList();
    }
}

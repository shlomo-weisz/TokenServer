package com.tokenlearn.server.util;

import com.tokenlearn.server.domain.CourseEntity;
import com.tokenlearn.server.dto.SimpleCourseDto;

public final class CourseLabelUtil {
    private CourseLabelUtil() {
    }

    public static String buildLabel(CourseEntity course) {
        if (course == null) {
            return "";
        }
        return buildLabel(course.getCourseNumber(), course.getNameHe(), course.getNameEn(), course.getName());
    }

    public static String buildLabel(SimpleCourseDto course) {
        if (course == null) {
            return "";
        }
        return buildLabel(course.getCourseNumber(), course.getNameHe(), course.getNameEn(), course.getName());
    }

    public static String buildLabel(String courseNumber, String nameHe, String nameEn, String fallbackName) {
        String number = normalized(courseNumber);
        String he = normalized(nameHe);
        String en = normalized(nameEn);
        String fallback = normalized(fallbackName);

        String title = !he.isEmpty() ? he : (!en.isEmpty() ? en : fallback);
        if (title.isEmpty()) {
            return number;
        }
        if (number.isEmpty()) {
            return title;
        }
        return number + " - " + title;
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}

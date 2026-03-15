package com.tokenlearn.server.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenlearn.server.dao.CourseDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Imports the course catalog JSON on application startup.
 *
 * <p>The sync is optional and can be disabled for tests or environments that do
 * not ship the catalog file.
 */
@Service
public class CourseCatalogSyncService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(CourseCatalogSyncService.class);

    private final CourseDao courseDao;
    private final ObjectMapper objectMapper;

    @Value("${app.course-catalog.enabled:true}")
    private boolean enabled;

    @Value("${app.course-catalog.path:}")
    private String configuredPath;

    public CourseCatalogSyncService(CourseDao courseDao, ObjectMapper objectMapper) {
        this.courseDao = courseDao;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        Path path = resolveCatalogPath();
        if (path == null) {
            log.warn("Course catalog file was not found. Skipping sync.");
            return;
        }

        try (InputStream inputStream = Files.newInputStream(path)) {
            List<CourseCatalogEntry> rows = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            int synced = 0;
            for (CourseCatalogEntry row : rows) {
                if (row == null || isBlank(row.courseNumber())) {
                    continue;
                }
                if (isBlank(row.hebrewName()) && isBlank(row.englishName())) {
                    continue;
                }
                courseDao.upsertCatalogCourse(row.courseNumber(), row.hebrewName(), row.englishName(), null);
                synced++;
            }
            log.info("Course catalog sync completed: {} records from {}", synced, path);
        } catch (Exception ex) {
            log.error("Failed to sync course catalog from {}: {}", path, ex.getMessage(), ex);
        }
    }

    private Path resolveCatalogPath() {
        if (!isBlank(configuredPath)) {
            Path configured = Paths.get(configuredPath).normalize();
            if (Files.exists(configured)) {
                return configured;
            }
            log.warn("Configured course catalog path does not exist: {}", configuredPath);
        }

        List<Path> fallbackPaths = List.of(
                Paths.get("openu_all_courses.json").normalize(),
                Paths.get("..", "openu_all_courses.json").normalize(),
                Paths.get("..", "..", "openu_all_courses.json").normalize());

        for (Path fallback : fallbackPaths) {
            if (Files.exists(fallback)) {
                return fallback;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record CourseCatalogEntry(
            String courseNumber,
            String englishName,
            String hebrewName) {
    }
}

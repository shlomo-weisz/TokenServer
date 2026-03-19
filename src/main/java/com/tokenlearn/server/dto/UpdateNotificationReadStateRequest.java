package com.tokenlearn.server.dto;

import lombok.Data;

import java.util.List;

/**
 * Bulk notification read-state update payload.
 */
@Data
public class UpdateNotificationReadStateRequest {
    private List<Long> ids;
}

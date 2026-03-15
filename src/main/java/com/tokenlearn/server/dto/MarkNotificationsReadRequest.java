package com.tokenlearn.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request used to mark specific notifications as read; omitting ids means "mark all".
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarkNotificationsReadRequest {
    private List<Long> ids;
}

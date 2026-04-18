package com.taskoryx.backend.entity;

import java.util.Set;

/**
 * Project-level permission constants.
 * These are used as string values stored in ProjectRole.permissions (CSV).
 * Different from system-level Permission entity (used for global RBAC).
 */
public final class ProjectPermission {

    public static final String TASK_VIEW = "TASK_VIEW";
    public static final String TASK_CREATE = "TASK_CREATE";
    public static final String TASK_UPDATE = "TASK_UPDATE";
    public static final String TASK_DELETE = "TASK_DELETE";
    public static final String TASK_ASSIGN = "TASK_ASSIGN";

    public static final String COMMENT_CREATE = "COMMENT_CREATE";
    public static final String COMMENT_DELETE = "COMMENT_DELETE";

    public static final String ATTACHMENT_MANAGE = "ATTACHMENT_MANAGE";
    public static final String TIME_TRACKING_VIEW = "TIME_TRACKING_VIEW";
    public static final String TIME_TRACKING_MANAGE = "TIME_TRACKING_MANAGE";
    public static final String LABEL_MANAGE = "LABEL_MANAGE";
    public static final String CATEGORY_MANAGE = "CATEGORY_MANAGE";

    public static final String BOARD_VIEW = "BOARD_VIEW";
    public static final String BOARD_UPDATE = "BOARD_UPDATE";

    public static final String SPRINT_MANAGE = "SPRINT_MANAGE";
    public static final String VERSION_MANAGE = "VERSION_MANAGE";
    public static final String MEMBER_MANAGE = "MEMBER_MANAGE";
    public static final String REPORT_VIEW = "REPORT_VIEW";
    public static final String WEBHOOK_MANAGE = "WEBHOOK_MANAGE";

    public static final Set<String> ALL = Set.of(
            TASK_VIEW, TASK_CREATE, TASK_UPDATE, TASK_DELETE, TASK_ASSIGN,
            COMMENT_CREATE, COMMENT_DELETE,
            ATTACHMENT_MANAGE, TIME_TRACKING_VIEW, TIME_TRACKING_MANAGE, LABEL_MANAGE, CATEGORY_MANAGE,
            BOARD_VIEW, BOARD_UPDATE,
            SPRINT_MANAGE, VERSION_MANAGE, MEMBER_MANAGE, REPORT_VIEW, WEBHOOK_MANAGE
    );

    private ProjectPermission() {}
}

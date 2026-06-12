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
    public static final String MEMBER_MANAGE = "MEMBER_MANAGE";
    public static final String REPORT_VIEW = "REPORT_VIEW";
    public static final String PERFORMANCE_CALCULATE = "PERFORMANCE_CALCULATE";
    public static final String WEBHOOK_MANAGE = "WEBHOOK_MANAGE";

    public static final Set<String> ALL = Set.of(
            TASK_VIEW, TASK_CREATE, TASK_UPDATE, TASK_DELETE, TASK_ASSIGN,
            COMMENT_CREATE, COMMENT_DELETE,
            ATTACHMENT_MANAGE, TIME_TRACKING_VIEW, TIME_TRACKING_MANAGE, LABEL_MANAGE, CATEGORY_MANAGE,
            BOARD_VIEW, BOARD_UPDATE,
            SPRINT_MANAGE, MEMBER_MANAGE, REPORT_VIEW, PERFORMANCE_CALCULATE, WEBHOOK_MANAGE
    );

    /**
     * Quyền cơ bản — mọi thành viên trong project đều có, không cần khai báo trong custom role.
     * Bao gồm: xem/tạo/sửa task, ghi nhận giờ, xem board, comment, đính kèm file.
     */
    public static final Set<String> BASIC = Set.of(
            TASK_VIEW,
            TASK_CREATE,
            TASK_UPDATE,
            TASK_ASSIGN,
            COMMENT_CREATE,
            ATTACHMENT_MANAGE,
            TIME_TRACKING_VIEW,
            TIME_TRACKING_MANAGE,
            BOARD_VIEW
    );

    /**
     * Quyền nâng cao — cần được cấp rõ ràng trong custom role.
     * Bao gồm: xóa task, quản lý sprint/label/category/member/webhook, xem báo cáo.
     */
    public static final Set<String> ADVANCED = Set.of(
            TASK_DELETE,
            COMMENT_DELETE,
            LABEL_MANAGE,
            CATEGORY_MANAGE,
            BOARD_UPDATE,
            SPRINT_MANAGE,
            MEMBER_MANAGE,
            REPORT_VIEW,
            PERFORMANCE_CALCULATE,
            WEBHOOK_MANAGE
    );

    private ProjectPermission() {}
}

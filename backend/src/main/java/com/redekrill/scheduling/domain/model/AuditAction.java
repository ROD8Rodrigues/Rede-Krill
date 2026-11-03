package com.redekrill.scheduling.domain.model;

public enum AuditAction {
    CREATE,
    UPDATE,
    DELETE,
    CANCEL,
    LOGIN,
    LOGIN_FAILED,
    LOGIN_BLOCKED,
    LOGOUT,
    PASSWORD_CHANGE,
    ACCESS_DENIED
}

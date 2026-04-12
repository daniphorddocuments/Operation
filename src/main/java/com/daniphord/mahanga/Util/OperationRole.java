package com.daniphord.mahanga.Util;

import java.util.List;

public final class OperationRole {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String UNASSIGNED = "UNASSIGNED";
    public static final String ADMIN = "ADMIN";
    public static final String CONTROL_ROOM_OPERATOR = "CONTROL_ROOM_OPERATOR";
    public static final String CGF = "CGF";
    public static final String CHIEF_FIRE_OFFICER = "CHIEF_FIRE_OFFICER";
    public static final String COMMISSIONER_OPERATIONS = "COMMISSIONER_OPERATIONS";
    public static final String HEAD_FIRE_FIGHTING_OPERATIONS = "HEAD_FIRE_FIGHTING_OPERATIONS";
    public static final String HEAD_RESCUE_OPERATIONS = "HEAD_RESCUE_OPERATIONS";
    public static final String REGIONAL_FIRE_OFFICER = "REGIONAL_FIRE_OFFICER";
    public static final String REGIONAL_OPERATION_OFFICER = "REGIONAL_OPERATION_OFFICER";
    public static final String DISTRICT_FIRE_OFFICER = "DISTRICT_FIRE_OFFICER";
    public static final String DISTRICT_OPERATION_OFFICER = "DISTRICT_OPERATION_OFFICER";
    public static final String STATION_FIRE_OPERATION_OFFICER = "STATION_FIRE_OPERATION_OFFICER";
    public static final String STATION_FIRE_OFFICER = "STATION_FIRE_OFFICER";
    public static final String STATION_OPERATION_OFFICER = "STATION_OPERATION_OFFICER";
    public static final String OPERATION_OFFICER = "OPERATION_OFFICER";
    public static final String DEPARTMENT_OFFICER = "DEPARTMENT_OFFICER";
    public static final String CONTROL_ROOM_ATTENDANT = "CONTROL_ROOM_ATTENDANT";
    public static final String TELE_SUPPORT_PERSONNEL = "TELE_SUPPORT_PERSONNEL";
    public static final String FIRE_INVESTIGATION_HOD = "FIRE_INVESTIGATION_HOD";
    public static final String FIRE_INVESTIGATION_OFFICER = "FIRE_INVESTIGATION_OFFICER";
    public static final String REGIONAL_INVESTIGATION_OFFICER = "REGIONAL_INVESTIGATION_OFFICER";
    public static final String DISTRICT_INVESTIGATION_OFFICER = "DISTRICT_INVESTIGATION_OFFICER";

    public static final List<String> NATIONAL_ROLES = List.of(
            SUPER_ADMIN,
            ADMIN,
            CGF,
            CHIEF_FIRE_OFFICER,
            COMMISSIONER_OPERATIONS,
            HEAD_FIRE_FIGHTING_OPERATIONS,
            HEAD_RESCUE_OPERATIONS,
            FIRE_INVESTIGATION_HOD
    );

    public static final List<String> USER_MANAGEMENT_ROLES = List.of(
            SUPER_ADMIN,
            ADMIN
    );

    public static final List<String> ALL_FROMS_ROLES = List.of(
            UNASSIGNED,
            ADMIN,
            CONTROL_ROOM_OPERATOR,
            CGF,
            CHIEF_FIRE_OFFICER,
            COMMISSIONER_OPERATIONS,
            HEAD_FIRE_FIGHTING_OPERATIONS,
            HEAD_RESCUE_OPERATIONS,
            REGIONAL_FIRE_OFFICER,
            REGIONAL_OPERATION_OFFICER,
            DISTRICT_FIRE_OFFICER,
            DISTRICT_OPERATION_OFFICER,
            STATION_FIRE_OPERATION_OFFICER,
            STATION_FIRE_OFFICER,
            STATION_OPERATION_OFFICER,
            OPERATION_OFFICER,
            DEPARTMENT_OFFICER,
            CONTROL_ROOM_ATTENDANT,
            TELE_SUPPORT_PERSONNEL,
            FIRE_INVESTIGATION_HOD,
            FIRE_INVESTIGATION_OFFICER,
            REGIONAL_INVESTIGATION_OFFICER,
            DISTRICT_INVESTIGATION_OFFICER
    );

    private OperationRole() {
    }

    public static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String normalized = role.trim().toUpperCase();
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }
}

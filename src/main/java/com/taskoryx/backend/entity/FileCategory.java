package com.taskoryx.backend.entity;

public enum FileCategory {
    IMAGE,
    DOCUMENT,
    SPREADSHEET,
    PRESENTATION,
    VIDEO,
    AUDIO,
    ARCHIVE,
    CODE,
    OTHER;

    public static FileCategory fromMimeType(String mimeType) {
        if (mimeType == null) return OTHER;

        String type = mimeType.toLowerCase();

        if (type.startsWith("image/")) return IMAGE;

        if (type.startsWith("video/")) return VIDEO;

        if (type.startsWith("audio/")) return AUDIO;

        if (type.equals("application/pdf")
                || type.equals("application/msword")
                || type.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                || type.equals("text/plain")
                || type.equals("text/rtf")
                || type.equals("application/rtf")) {
            return DOCUMENT;
        }

        if (type.equals("application/vnd.ms-excel")
                || type.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                || type.equals("text/csv")) {
            return SPREADSHEET;
        }

        if (type.equals("application/vnd.ms-powerpoint")
                || type.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")) {
            return PRESENTATION;
        }

        if (type.equals("application/zip")
                || type.equals("application/x-zip-compressed")
                || type.equals("application/x-rar-compressed")
                || type.equals("application/x-7z-compressed")
                || type.equals("application/gzip")
                || type.equals("application/x-tar")) {
            return ARCHIVE;
        }

        if (type.startsWith("text/")
                || type.equals("application/json")
                || type.equals("application/xml")
                || type.equals("application/javascript")
                || type.equals("application/typescript")) {
            return CODE;
        }

        return OTHER;
    }
}

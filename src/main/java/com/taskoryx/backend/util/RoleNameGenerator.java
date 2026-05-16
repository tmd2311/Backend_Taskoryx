package com.taskoryx.backend.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class RoleNameGenerator {

    private static final Pattern NON_ASCII = Pattern.compile("[^a-zA-Z0-9\\s]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * Sinh mã role từ tên hiển thị.
     * VD: "Quản lý nhân sự" → "QUAN_LY_NHAN_SU"
     * VD: "HR Manager"      → "HR_MANAGER"
     */
    public static String generate(String displayName) {
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Tên hiển thị không được để trống");
        }

        // Chuẩn hóa Unicode: tách dấu khỏi ký tự gốc (NFD), rồi bỏ dấu
        String normalized = Normalizer.normalize(displayName.trim(), Normalizer.Form.NFD);
        // Xóa ký tự kết hợp (dấu) và ký tự đặc biệt
        String ascii = NON_ASCII.matcher(normalized).replaceAll("");
        // Gộp nhiều khoảng trắng, đổi thành dấu gạch dưới, viết hoa
        return WHITESPACE.matcher(ascii.trim()).replaceAll("_").toUpperCase();
    }

    private RoleNameGenerator() {}
}

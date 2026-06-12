package com.taskoryx.backend.ai.service;

import com.taskoryx.backend.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Kiểm tra và làm sạch đầu vào trước khi gửi lên LLM.
 *
 * Chặn 4 nhóm (song ngữ Anh + Việt):
 *  1. Prompt injection  — lệnh ghi đè system prompt
 *  2. Data extraction   — cố lấy thông tin hệ thống, config, user data
 *  3. Nội dung không liên quan đến quản lý dự án
 *  4. Input quá ngắn / không có nghĩa
 */
@Component
public class AiInputGuard {

    private static final int MIN_LENGTH = 20;

    // ── 1. PROMPT INJECTION ───────────────────────────────────────────────────
    private static final List<Pattern> INJECTION_PATTERNS = List.of(

        // Tiếng Anh
        Pattern.compile("(?i)ignore\\s+(previous|above|all|prior)\\s+(instructions?|prompts?|rules?|context)"),
        Pattern.compile("(?i)forget\\s+(everything|all|previous|instructions?)"),
        Pattern.compile("(?i)you\\s+are\\s+now\\s+(a|an|the)"),
        Pattern.compile("(?i)act\\s+as\\s+(a|an|the)\\s+(?!project|scrum|agile)"),
        Pattern.compile("(?i)new\\s+(role|persona|instructions?|system\\s+prompt)"),
        Pattern.compile("(?i)(system|user|assistant)\\s*:\\s*"),
        Pattern.compile("(?i)jailbreak"),
        Pattern.compile("(?i)DAN\\s+mode"),
        Pattern.compile("(?i)\\\\n(system|user|assistant)\\s*:"),
        Pattern.compile("(?i)disregard\\s+(your|the|all|previous)"),
        Pattern.compile("(?i)override\\s+(the\\s+)?(system|previous|initial)\\s+(prompt|instructions?)"),
        Pattern.compile("(?i)pretend\\s+(you\\s+are|to\\s+be)"),
        Pattern.compile("(?i)switch\\s+(to\\s+)?(a\\s+new|another|different)\\s+(mode|role|persona)"),

        // Tiếng Việt
        Pattern.compile("(?i)(bỏ\\s*qua|bỏ\\s*hết|quên|xóa)\\s+(các\\s+)?(hướng\\s*dẫn|lệnh|quy\\s*tắc|system\\s*prompt|câu\\s*lệnh\\s*trước)"),
        Pattern.compile("(?i)(từ\\s*bây\\s*giờ|bây\\s*giờ)\\s+(bạn|mày|mi)\\s+(là|hãy\\s+là|sẽ\\s+là)"),
        Pattern.compile("(?i)(đóng\\s*vai|giả\\s*làm|vào\\s*vai)\\s+(?!quản\\s*lý|scrum\\s*master|product\\s*owner)"),
        Pattern.compile("(?i)(thay\\s*đổi|cập\\s*nhật|ghi\\s*đè)\\s+(lên\\s+)?(system\\s*prompt|hướng\\s*dẫn|lệnh\\s*hệ\\s*thống)"),
        Pattern.compile("(?i)(bỏ\\s*qua|vô\\s*hiệu\\s*hóa)\\s+(các\\s+)?(quy\\s*tắc|hạn\\s*chế|giới\\s*hạn)"),
        Pattern.compile("(?i)(chế\\s*độ|mode)\\s+(mới|khác|không\\s*giới\\s*hạn|tự\\s*do)"),
        Pattern.compile("(?i)giả\\s*vờ\\s+(bạn|mày)\\s+(là|không\\s+có\\s+giới\\s*hạn)")
    );

    // ── 2. DATA EXTRACTION / SOCIAL ENGINEERING ───────────────────────────────
    private static final List<Pattern> EXTRACTION_PATTERNS = List.of(

        // Tiết lộ system prompt / config — Tiếng Anh
        Pattern.compile("(?i)(reveal|show|print|display|output|return|give\\s+me|tell\\s+me|what\\s+is)\\s+(your\\s+)?(system\\s+prompt|instructions?|config|configuration|secret|api\\s+key|password|credential)"),
        Pattern.compile("(?i)(system\\s+prompt|initial\\s+prompt|original\\s+prompt|hidden\\s+prompt|your\\s+instructions?)"),
        Pattern.compile("(?i)(repeat|echo|output|print)\\s+(everything|all|the\\s+above|what\\s+you\\s+were\\s+told)"),
        Pattern.compile("(?i)what\\s+(are|were)\\s+your\\s+(original\\s+)?(instructions?|orders?|rules?|directives?)"),

        // Tiết lộ system prompt / config — Tiếng Việt
        Pattern.compile("(?i)(tiết\\s*lộ|cho\\s*tôi\\s*xem|hiện|in\\s*ra|đưa\\s*cho\\s*tôi)\\s+(system\\s*prompt|hướng\\s*dẫn\\s*hệ\\s*thống|lệnh\\s*gốc|cấu\\s*hình|mật\\s*khẩu|api\\s*key)"),
        Pattern.compile("(?i)(bạn\\s+được\\s+lập\\s+trình|bạn\\s+được\\s+hướng\\s+dẫn|câu\\s+lệnh\\s+ban\\s+đầu|prompt\\s+gốc)"),
        Pattern.compile("(?i)(nhắc\\s+lại|lặp\\s+lại|in\\s+lại)\\s+(tất\\s*cả|toàn\\s*bộ|những\\s+gì)\\s+(bạn\\s+được|phía\\s+trên|ở\\s+trên)"),
        Pattern.compile("(?i)hướng\\s+dẫn\\s+(ban\\s+đầu|gốc|của\\s+bạn|hệ\\s+thống)\\s+(là\\s+gì|như\\s+thế\\s+nào)"),

        // Truy vấn user data / database — Tiếng Anh
        Pattern.compile("(?i)(list|show|get|fetch|retrieve|select|dump|extract)\\s+(all\\s+)?(users?|accounts?|emails?|passwords?|tokens?|database|tables?|records?)"),
        Pattern.compile("(?i)(SELECT|INSERT|UPDATE|DELETE|DROP|ALTER|CREATE)\\s+.{0,50}\\s+(FROM|INTO|TABLE|DATABASE)"),

        // Truy vấn user data / database — Tiếng Việt
        Pattern.compile("(?i)(liệt\\s*kê|hiển\\s*thị|lấy|truy\\s*xuất|đưa\\s*ra|cho\\s*tôi\\s*biết)\\s+(tất\\s*cả\\s+)?(người\\s*dùng|tài\\s*khoản|mật\\s*khẩu|dữ\\s*liệu|email|token|bảng\\s*dữ\\s*liệu)"),
        Pattern.compile("(?i)(thông\\s*tin|dữ\\s*liệu)\\s+(của\\s+)?(tất\\s*cả|toàn\\s*bộ)\\s+(người\\s*dùng|thành\\s*viên|tài\\s*khoản)"),

        // Đọc file hệ thống — Tiếng Anh
        Pattern.compile("(?i)(read|open|cat|print|access|load)\\s+(the\\s+)?(file|application\\.yaml|application\\.properties|\\.env|config\\.yml|secrets?)"),
        Pattern.compile("(?i)(application\\.yaml|application\\.properties|\\.env|web\\.xml|pom\\.xml|Dockerfile|docker-compose)"),

        // Đọc file hệ thống — Tiếng Việt
        Pattern.compile("(?i)(đọc|mở|truy\\s*cập|xem)\\s+(file|tệp\\s*tin|tệp\\s*cấu\\s*hình|cấu\\s*hình\\s*server)"),

        // Thực thi lệnh — Tiếng Anh
        Pattern.compile("(?i)(execute|run|eval|exec|invoke)\\s*(a\\s+)?(shell|bash|cmd|command|script|code|terminal)?"),
        Pattern.compile("(?i)(shell|bash|cmd|powershell|terminal)\\s*(command|script|code|injection)?"),

        // Thực thi lệnh — Tiếng Việt
        Pattern.compile("(?i)(chạy\\s*lệnh|thực\\s*thi\\s*lệnh|thực\\s*hiện\\s*lệnh|gọi\\s*lệnh)"),
        Pattern.compile("(?i)(chạy|thực\\s*thi|thực\\s*hiện)\\s+(đoạn\\s*)?(code|mã|script|lệnh\\s*bash|lệnh\\s*shell)"),

        // Internal infrastructure — Tiếng Anh
        Pattern.compile("(?i)(database\\s+host|db\\s+password|db\\s+url|server\\s+ip|aws|s3\\s+bucket|access\\s+key|secret\\s+key|jwt\\s+secret|private\\s+key)"),

        // Internal infrastructure — Tiếng Việt
        Pattern.compile("(?i)(địa\\s*chỉ\\s*server|ip\\s*server|mật\\s*khẩu\\s*database|khóa\\s*bí\\s*mật|cổng\\s*kết\\s*nối)"),

        // Social engineering giả danh admin — Tiếng Anh
        Pattern.compile("(?i)(i\\s+am|i'm)\\s+(the\\s+)?(admin|administrator|superuser|root|system\\s+owner|developer|maintainer)"),
        Pattern.compile("(?i)(maintenance|debug|developer|god|unrestricted|unlimited)\\s+mode"),
        Pattern.compile("(?i)this\\s+is\\s+(a\\s+)?(test|authorized|official|internal)\\s+(mode|request|access)"),

        // Social engineering giả danh admin — Tiếng Việt
        Pattern.compile("(?i)(tôi\\s+là|mình\\s+là)\\s+(quản\\s*trị\\s*viên|admin|người\\s+tạo|chủ\\s+sở\\s+hữu|nhà\\s+phát\\s+triển|superuser|root)"),
        Pattern.compile("(?i)(chế\\s*độ\\s*bảo\\s*trì|chế\\s*độ\\s*debug|chế\\s*độ\\s*không\\s*giới\\s*hạn|truy\\s*cập\\s*nội\\s*bộ)"),
        Pattern.compile("(?i)(đây\\s+là|tôi\\s+có)\\s+(quyền|ủy\\s+quyền|cho\\s+phép)\\s+(đặc\\s+biệt|quản\\s+trị|nội\\s+bộ)"),

        // Cố thoát context JSON / HTML injection
        Pattern.compile("(?i)(</?(script|html|body|head)>|javascript:|data:text|onerror=|onload=)"),
        Pattern.compile("\\}\\s*,?\\s*\\{\\s*\"role\""),

        // Thu thập / truy xuất dữ liệu từ hệ thống ngoài — Tiếng Việt
        Pattern.compile("(?i)(truy\\s*xuất|thu\\s*thập|lấy\\s*cắp|đánh\\s*cắp|scrape?|crawl)\\s+(dữ\\s*liệu|thông\\s*tin)\\s+(người\\s*dùng|tài\\s*khoản|khách\\s*hàng)\\s+(của|từ)\\s+(hệ\\s*thống|website|app|ứng\\s*dụng|trang\\s*web)\\s*(khác|bên\\s*ngoài|đối\\s*thủ|khác)?"),
        Pattern.compile("(?i)(thu\\s*thập|truy\\s*xuất|lấy)\\s+(trái\\s*phép|không\\s*được\\s*phép|bất\\s*hợp\\s*pháp|lén\\s*lút)\\s+(dữ\\s*liệu|thông\\s*tin)"),
        Pattern.compile("(?i)(hack|tấn\\s*công|xâm\\s*nhập|khai\\s*thác|bypass|vượt\\s*qua)\\s+(hệ\\s*thống|bảo\\s*mật|xác\\s*thực|mật\\s*khẩu|tường\\s*lửa|firewall)"),
        Pattern.compile("(?i)(tấn\\s*công|xâm\\s*nhập|phá\\s*hoại)\\s+(server|máy\\s*chủ|website|web|hệ\\s*thống|mạng|database|cơ\\s*sở\\s*dữ\\s*liệu|vào)"),
        Pattern.compile("(?i)(phần\\s*mềm\\s*gián\\s*điệp|spyware|malware|ransomware|virus|trojan|keylogger|botnet)"),
        Pattern.compile("(?i)(theo\\s*dõi|giám\\s*sát).{0,40}(mà\\s*không|không\\s*có|không\\s*được|không\\s*biết).{0,30}(sự\\s*đồng\\s*ý|cho\\s*phép|biết|đồng\\s*ý)"),
        Pattern.compile("(?i)(đánh\\s*cắp|chiếm\\s*đoạt|lấy\\s*trộm)\\s+(tài\\s*khoản|mật\\s*khẩu|thông\\s*tin|dữ\\s*liệu|thẻ\\s*tín\\s*dụng|token)"),

        // Thu thập dữ liệu từ hệ thống ngoài — Tiếng Anh
        Pattern.compile("(?i)(scrape?|crawl|harvest|steal|exfiltrate)\\s+(user\\s+)?(data|information|credentials?|accounts?)\\s+(from|of)\\s+(other|another|external|third.party|competitor)\\s+(system|website|app|platform)"),
        Pattern.compile("(?i)(unauthorized|illegal|covert|secret)\\s+(data|information)\\s+(collection|harvesting|extraction|access)"),
        Pattern.compile("(?i)(hack|breach|exploit|bypass|infiltrate|compromise)\\s+(the\\s+)?(system|security|authentication|firewall|server|database)"),
        Pattern.compile("(?i)(spy(ware)?|malware|ransomware|trojan|keylogger|botnet|rootkit)")
    );

    // ── 3. PROJECT KEYWORDS — ít nhất 1 cái phải xuất hiện ───────────────────
    private static final List<Pattern> PROJECT_KEYWORDS = List.of(
        Pattern.compile("(?i)(dự\\s*án|project|phần\\s*mềm|software|website|web|app|ứng\\s*dụng|application)"),
        Pattern.compile("(?i)(hệ\\s*thống|system|platform|nền\\s*tảng)"),
        Pattern.compile("(?i)(sprint|scrum|agile|kanban|backlog|task|công\\s*việc)"),
        Pattern.compile("(?i)(tính\\s*năng|feature|module|chức\\s*năng|functionality)"),
        Pattern.compile("(?i)(xây\\s*dựng|phát\\s*triển|develop|build|create|thiết\\s*kế|design)"),
        Pattern.compile("(?i)(quản\\s*lý|manage|management|triển\\s*khai|deploy|implement)"),
        Pattern.compile("(?i)(team|nhóm|thành\\s*viên|member|deadline|milestone)"),
        Pattern.compile("(?i)(database|api|backend|frontend|mobile|server|cloud)"),
        Pattern.compile("(?i)(sản\\s*phẩm|product|dịch\\s*vụ|service|giải\\s*pháp|solution)")
    );

    public void validate(String requirement) {
        if (requirement == null || requirement.isBlank()) {
            throw new BadRequestException("Yêu cầu không được để trống.");
        }

        String trimmed = requirement.strip();

        if (trimmed.length() < MIN_LENGTH) {
            throw new BadRequestException(
                "Mô tả dự án quá ngắn. Vui lòng cung cấp ít nhất " + MIN_LENGTH + " ký tự."
            );
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(trimmed).find()) {
                throw new BadRequestException(
                    "Yêu cầu chứa nội dung không hợp lệ. Vui lòng mô tả dự án bạn muốn tạo."
                );
            }
        }

        for (Pattern pattern : EXTRACTION_PATTERNS) {
            if (pattern.matcher(trimmed).find()) {
                throw new BadRequestException(
                    "Yêu cầu chứa nội dung không được phép. Vui lòng mô tả dự án bạn muốn tạo."
                );
            }
        }

        boolean hasProjectKeyword = PROJECT_KEYWORDS.stream()
                .anyMatch(p -> p.matcher(trimmed).find());

        if (!hasProjectKeyword) {
            throw new BadRequestException(
                "Yêu cầu không liên quan đến quản lý dự án. " +
                "Vui lòng mô tả dự án, tính năng, hoặc hệ thống bạn muốn xây dựng."
            );
        }
    }

    /** Trả về requirement đã strip và truncate nếu vượt 2000 ký tự */
    public String sanitize(String requirement) {
        String stripped = requirement.strip();
        return stripped.length() > 2000 ? stripped.substring(0, 2000) : stripped;
    }
}

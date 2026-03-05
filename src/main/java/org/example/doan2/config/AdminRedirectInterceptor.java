package org.example.doan2.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor kiểm tra quyền truy cập của người dùng cho mọi request.
 * Ý nghĩa: Nếu tài khoản có quyền ROLE_ADMIN mà lại đang rảnh rỗi đi dạo sang
 * vùng của khách hàng (ví dụ /, /shop, /cart), hệ thống sẽ lập tức can thiệp và bẻ lái
 * chở họ về đúng trang làm việc của Admin (/admin).
 */
@Component
public class AdminRedirectInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Lấy thông tin user hiện tại đang được ủy quyền (đã đăng nhập) từ hệ thống bảo mật Spring Security
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        // Lấy con đường (URL URI) mà user đang xin phép đi qua
        String requestURI = request.getRequestURI();

        // [Logic 1] Tránh khoá nhầm các file tĩnh (Hình ảnh, Giao diện CSS, JS...)
        // Bắt buộc phải cho qua để giao diện không bị giật lag mất hình ảnh
        if (requestURI.startsWith("/css") || requestURI.startsWith("/js") || 
            requestURI.startsWith("/img") || requestURI.startsWith("/lib") ||
            requestURI.startsWith("/scss") || requestURI.startsWith("/error")) {
            return true; // Cho qua trạm an toàn
        }

        // [Logic 2] Kiểm tra danh tính thật sự của user có phải là ADMIN không?
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            boolean isAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            if (isAdmin) {
                // Nếu đích thực là ADMIN, NHƯNG con đường họ chọn KHÔNG phải là trang Quản Trị (/admin)
                // và cũng KHÔNG phải là trang Đăng Xuất (/logout)
                if (!requestURI.startsWith("/admin") && !requestURI.startsWith("/logout") && !requestURI.equals("/login")) {
                    // Chặn xe lại. Bắt họ phải quay đầu xe chạy thẳng về nhà /admin của họ
                    response.sendRedirect(request.getContextPath() + "/admin");
                    return false; // Ngừng không xử lý tiếp cái request vớ vẩn này
                }
            }
        }

        // Còn nếu là Khách hàng (ROLE_CUSTOMER) bình thường hoặc khách vãng lai (Guest) chưa đăng nhập
        // Hoặc ông Admin đang ngoan ngoãn đi đúng đường /admin thì mở trạm cho qua bình thường.
        return true;
    }
}

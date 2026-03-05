package org.example.doan2.service;

import jakarta.servlet.http.HttpSession;
import org.example.doan2.dto.CartItem;
import org.example.doan2.entity.ChiTietGioHang;
import org.example.doan2.entity.GioHang;
import org.example.doan2.entity.NguoiDung;
import org.example.doan2.repository.BienTheSanPhamRepository;
import org.example.doan2.repository.ChiTietGioHangRepository;
import org.example.doan2.repository.GioHangRepository;
import org.example.doan2.repository.NguoiDungRepository;
import org.example.doan2.repository.SanPhamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service xử lý toàn bộ logic liên quan đến Giỏ hàng của người dùng.
 * Hỗ trợ cả 2 chế độ:
 * 1. Khách chưa đăng nhập (Guest): Lưu giỏ hàng tạm thời vào Session của trình duyệt.
 * 2. Khách đã đăng nhập (User): Lưu giỏ hàng vĩnh viễn vào Database (bảng GioHang & ChiTietGioHang).
 */
@Service
@Transactional
public class CartService {
    private static final String CART_SESSION_KEY = "cart";

    private final GioHangRepository gioHangRepository;
    private final ChiTietGioHangRepository chiTietGioHangRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final SanPhamRepository sanPhamRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;

    public CartService(GioHangRepository gioHangRepository,
                       ChiTietGioHangRepository chiTietGioHangRepository,
                       NguoiDungRepository nguoiDungRepository,
                       SanPhamRepository sanPhamRepository,
                       BienTheSanPhamRepository bienTheSanPhamRepository) {
        this.gioHangRepository = gioHangRepository;
        this.chiTietGioHangRepository = chiTietGioHangRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.bienTheSanPhamRepository = bienTheSanPhamRepository;
    }

    private GioHang getOrCreateGioHang(NguoiDung nguoiDung) {
        return gioHangRepository.findByNguoiDung(nguoiDung)
                .orElseGet(() -> {
                    GioHang gh = new GioHang();
                    gh.setNguoiDung(nguoiDung);
                    gh.setTongSoLuong(0);
                    return gioHangRepository.save(gh);
                });
    }

    private NguoiDung getUser(String email) {
        if (email == null || email.isEmpty()) return null;
        return nguoiDungRepository.findByEmail(email).orElse(null);
    }

    /**
     * Đồng bộ giỏ hàng từ Session sang Database ngay sau khi người dùng đăng nhập thành công.
     * Tránh việc khách nhặt đồ vào giỏ lúc chưa đăng nhập bị biến mất sau khi login.
     */
    public void syncSessionCartToDb(HttpSession session, String email) {
        NguoiDung user = getUser(email);
        if (user == null) return;
        List<CartItem> sessionCart = (List<CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (sessionCart != null && !sessionCart.isEmpty()) {
            for (CartItem item : sessionCart) {
                addToCart(session, email, item);
            }
            session.removeAttribute(CART_SESSION_KEY);
        }
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm đang có trong giỏ hàng để hiển thị.
     * Logic:
     * - Nếu đã đăng nhập: Đồng bộ Session trước, sau đó lấy cấu trúc giỏ hàng từ Database, chuyển đổi sang danh sách CartItem (DTO) để trả về cho giao diện HTML dễ dàng in ra.
     * - Nếu chưa đăng nhập: Lấy trực tiếp từ Session.
     */
    public List<CartItem> getCart(HttpSession session, String email) {
        NguoiDung user = getUser(email);
        if (user != null) {
            syncSessionCartToDb(session, email); // Sync in case there are session items before login
            GioHang gioHang = getOrCreateGioHang(user);
            List<ChiTietGioHang> items = chiTietGioHangRepository.findByGioHang(gioHang);
            List<CartItem> result = new ArrayList<>();
            for (ChiTietGioHang ct : items) {
                Integer vId = ct.getBienThe() != null ? ct.getBienThe().getId() : null;
                String vInfo = null;
                if (ct.getBienThe() != null) {
                     vInfo = ct.getBienThe().getCpu() + " / " + ct.getBienThe().getBoNho() + " / " + ct.getBienThe().getMauSac();
                }
                result.add(new CartItem(
                        ct.getSanPham().getId(),
                        ct.getSanPham().getTenSanPham(),
                        ct.getGia(),
                        ct.getSoLuong(),
                        ct.getSanPham().getHinhAnh(),
                        vId,
                        vInfo
                ));
            }
            return result;
        }

        List<CartItem> cart = (List<CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        }
        return cart;
    }

    private boolean isSameItem(CartItem a, CartItem b) {
        if (!a.getId().equals(b.getId())) return false;
        return Objects.equals(a.getVariantId(), b.getVariantId());
    }

    /**
     * Thêm một món hàng mới vào giỏ.
     * Logic xử lý:
     * - Kiểm tra xem Sản phẩm + Biến thể đó đã tồn tại trong giỏ chưa.
     * - Nếu CÓ RỒI: Chỉ việc cộng dồn thêm số lượng (quantity) vào dữ liệu cũ.
     * - Nếu CHƯA CÓ: Tạo mới một dòng ChiTietGioHang, lưu ID sản phẩm, ID biến thể, số lượng, giá và đẩy vào DB.
     */
    public void addToCart(HttpSession session, String email, CartItem item) {
        NguoiDung user = getUser(email);
        if (user != null) {
            GioHang gh = getOrCreateGioHang(user);
            Optional<ChiTietGioHang> existing;
            if (item.getVariantId() != null) {
                existing = chiTietGioHangRepository.findByGioHangAndSanPhamIdAndBienTheId(gh, item.getId(), item.getVariantId());
            } else {
                existing = chiTietGioHangRepository.findByGioHangAndSanPhamIdAndBienTheIsNull(gh, item.getId());
            }

            if (existing.isPresent()) {
                ChiTietGioHang ct = existing.get();
                ct.setSoLuong(ct.getSoLuong() + item.getQuantity());
                chiTietGioHangRepository.save(ct);
            } else {
                ChiTietGioHang ct = new ChiTietGioHang();
                ct.setGioHang(gh);
                ct.setSanPham(sanPhamRepository.findById(item.getId()).orElseThrow());
                if (item.getVariantId() != null) {
                    ct.setBienThe(bienTheSanPhamRepository.findById(item.getVariantId()).orElse(null));
                }
                ct.setGia(item.getPrice());
                ct.setSoLuong(item.getQuantity());
                chiTietGioHangRepository.save(ct);
            }
            updateTotalQuantity(gh);
            return;
        }

        List<CartItem> cart = getCart(session, null);
        boolean exists = false;
        for (CartItem cartItem : cart) {
            if (isSameItem(cartItem, item)) {
                cartItem.setQuantity(cartItem.getQuantity() + item.getQuantity());
                exists = true;
                break;
            }
        }
        if (!exists) {
            cart.add(item);
        }
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    /**
     * Bỏ một mặt hàng ra khỏi giỏ.
     * Yêu cầu truyền vào cả Mã Sản phẩm (productId) và Mã Biến thể (variantId nếu có) 
     * để phần mềm dò tìm và xóa chính xác dòng chứa sản phẩm đó trong cơ sở dữ liệu.
     */
    public void removeFromCart(HttpSession session, String email, Integer productId, Integer variantId) {
        NguoiDung user = getUser(email);
        if (user != null) {
            GioHang gh = getOrCreateGioHang(user);
            Optional<ChiTietGioHang> existing;
            if (variantId != null) {
                existing = chiTietGioHangRepository.findByGioHangAndSanPhamIdAndBienTheId(gh, productId, variantId);
            } else {
                existing = chiTietGioHangRepository.findByGioHangAndSanPhamIdAndBienTheIsNull(gh, productId);
            }
            existing.ifPresent(chiTietGioHangRepository::delete);
            // Must flush or retrieve dynamically to calculate correct total
            chiTietGioHangRepository.flush(); 
            updateTotalQuantity(gh);
            return;
        }

        List<CartItem> cart = getCart(session, null);
        cart.removeIf(item -> item.getId().equals(productId) && Objects.equals(item.getVariantId(), variantId));
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    /**
     * Cập nhật số lượng của một sản phẩm bất kỳ ngay từ bên trong Giỏ Hàng
     * (thường được thực thi qua AJAX khi người dùng bấm nút [+] hoặc [-]).
     */
    public void updateQuantity(HttpSession session, String email, Integer productId, Integer variantId, int quantity) {
        NguoiDung user = getUser(email);
        if (user != null) {
            GioHang gh = getOrCreateGioHang(user);
            Optional<ChiTietGioHang> existing;
            if (variantId != null) {
                existing = chiTietGioHangRepository.findByGioHangAndSanPhamIdAndBienTheId(gh, productId, variantId);
            } else {
                existing = chiTietGioHangRepository.findByGioHangAndSanPhamIdAndBienTheIsNull(gh, productId);
            }
            if (existing.isPresent()) {
                ChiTietGioHang ct = existing.get();
                ct.setSoLuong(quantity);
                chiTietGioHangRepository.save(ct);
                updateTotalQuantity(gh);
            }
            return;
        }

        List<CartItem> cart = getCart(session, null);
        for (CartItem item : cart) {
            if (item.getId().equals(productId) && Objects.equals(item.getVariantId(), variantId)) {
                item.setQuantity(quantity);
                break;
            }
        }
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    /**
     * Xóa sạch toàn bộ sản phẩm trong giỏ hàng.
     * Chức năng này thường được tự động gọi ngay sau khi Khách hàng Nhấn "Đặt hàng" thành công.
     */
    public void clearCart(HttpSession session, String email) {
        NguoiDung user = getUser(email);
        if (user != null) {
            GioHang gh = getOrCreateGioHang(user);
            List<ChiTietGioHang> items = chiTietGioHangRepository.findByGioHang(gh);
            chiTietGioHangRepository.deleteAll(items);
            gh.setTongSoLuong(0);
            gioHangRepository.save(gh);
            return;
        }
        session.removeAttribute(CART_SESSION_KEY);
    }
    
    public Integer getTotalPrice(HttpSession session, String email) {
        List<CartItem> cart = getCart(session, email);
        return cart.stream().mapToInt(CartItem::getTotalPrice).sum();
    }
    
    public int getCount(HttpSession session, String email) {
        List<CartItem> cart = getCart(session, email);
        return cart.stream().mapToInt(CartItem::getQuantity).sum();
    }

    /**
     * Tính toán lại tổng số lượng tất cả các món hàng đang có trong ChiTietGioHang (hàm phụ trợ),
     * sau đó cập nhật con số này ngược lại vào bảng GioHang cha để dễ dàng thống kê báo cáo.
     */
    private void updateTotalQuantity(GioHang gh) {
        List<ChiTietGioHang> items = chiTietGioHangRepository.findByGioHang(gh);
        int total = items.stream().mapToInt(ChiTietGioHang::getSoLuong).sum();
        gh.setTongSoLuong(total);
        gioHangRepository.save(gh);
    }
}

package org.example.doan2.service;

import jakarta.servlet.http.HttpSession;
import org.example.doan2.dto.CartItem;
import org.example.doan2.entity.ChiTietGioHang;
import org.example.doan2.entity.GioHang;
import org.example.doan2.entity.NguoiDung;
import org.example.doan2.entity.SanPham;
import org.example.doan2.entity.BienTheSanPham;
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

    /**
     * Lấy giỏ hàng của người dùng trong DB.
     * Nếu chưa có giỏ thì tạo mới một bản ghi GioHang với tổng số lượng ban đầu = 0.
     * @param nguoiDung Thực thể người dùng
     * @return Thực thể GioHang (luôn có dữ liệu)
     */
    private GioHang getOrCreateGioHang(NguoiDung nguoiDung) {
        return gioHangRepository.findByNguoiDung(nguoiDung)
                .orElseGet(() -> {
                    GioHang gh = new GioHang();
                    gh.setNguoiDung(nguoiDung);
                    gh.setTongSoLuong(0);
                    return gioHangRepository.save(gh);
                });
    }

    /**
     * Tìm đối tượng người dùng dựa trên email.
     * @param email Email đăng nhập của người dùng
     * @return Đối tượng NguoiDung hoặc null nếu là khách vãng lai
     */
    private NguoiDung getUser(String email) {
        if (email == null || email.isEmpty()) return null;
        return nguoiDungRepository.findByEmail(email).orElse(null);
    }

    /**
     * Đồng bộ giỏ hàng từ Session (trình duyệt) vào Database ngay sau khi đăng nhập.
     * Giúp người dùng giữ lại các mặt hàng đã chọn trước khi đăng nhập.
     */
    public void syncSessionCartToDb(HttpSession session, String email) {
        NguoiDung user = getUser(email);
        if (user == null) return;
        List<CartItem> sessionCart = (List<CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (sessionCart != null && !sessionCart.isEmpty()) {
            for (CartItem item : sessionCart) {
                addToCart(session, email, item);
            }
            // Xóa giỏ hàng trong session sau khi đã đồng bộ thành công vào DB
            session.removeAttribute(CART_SESSION_KEY);
        }
    }

    /**
     * Lấy danh sách sản phẩm trong giỏ hàng để hiển thị lên giao diện.
     * 
     * Logic tính giá cực kỳ quan trọng:
     * Giá cuối = (Giá gốc sản phẩm + Giá cộng thêm của cấu hình) - Tiền giảm giá (nếu có khuyến mãi).
     * 
     * @param session Session hiện tại
     * @param email Email người dùng (nếu đã login)
     * @return Danh sách CartItem DTO
     */
    public List<CartItem> getCart(HttpSession session, String email) {
        NguoiDung user = getUser(email);
        if (user != null) {
            // Nếu đã đăng nhập, ưu tiên lấy từ Database
            syncSessionCartToDb(session, email); 
            GioHang gioHang = getOrCreateGioHang(user);
            List<ChiTietGioHang> items = chiTietGioHangRepository.findByGioHang(gioHang);
            List<CartItem> result = new ArrayList<>();
            for (ChiTietGioHang ct : items) {
                Integer vId = ct.getBienThe() != null ? ct.getBienThe().getId() : null;
                String vInfo = null;
                int currentPrice = ct.getSanPham().getGia();

                // 1. Tính giá dựa trên cấu hình (Biến thể)
                if (ct.getBienThe() != null) {
                     vInfo = ct.getBienThe().getCpu() + " / " + ct.getBienThe().getBoNho() + " / " + ct.getBienThe().getMauSac();
                     currentPrice += (ct.getBienThe().getGiaThem() != null ? ct.getBienThe().getGiaThem() : 0);
                }

                // 2. Kiểm tra và áp dụng khuyến mãi (nếu chương trình còn hiệu lực)
                if (ct.getSanPham().isDangKhuyenMai()) {
                    int phanTramGiam = ct.getSanPham().getKhuyenMai().getPhanTramGiam();
                    currentPrice = currentPrice - (currentPrice * phanTramGiam / 100);
                }

                result.add(new CartItem(
                        ct.getSanPham().getId(),
                        ct.getSanPham().getTenSanPham(),
                        currentPrice,
                        ct.getSoLuong(),
                        ct.getSanPham().getHinhAnh(),
                        vId,
                        vInfo
                ));
            }
            return result;
        }

        // Trường hợp Khách vãng lai: Lấy giỏ hàng từ Session
        List<CartItem> cart = (List<CartItem>) session.getAttribute(CART_SESSION_KEY);
        if (cart == null) {
            cart = new ArrayList<>();
            session.setAttribute(CART_SESSION_KEY, cart);
        } else {
            // Cập nhật lại giá mới nhất (giá có thể thay đổi do Admin cập nhật) cho giỏ hàng Session
            for (CartItem ci : cart) {
                sanPhamRepository.findById(ci.getId()).ifPresent(sp -> {
                    int[] priceWrapper = new int[]{sp.getGia()};
                    if (ci.getVariantId() != null) {
                        bienTheSanPhamRepository.findById(ci.getVariantId()).ifPresent(bt -> {
                            priceWrapper[0] += (bt.getGiaThem() != null ? bt.getGiaThem() : 0);
                        });
                    }
                    if (sp.isDangKhuyenMai()) {
                        int phanTramGiam = sp.getKhuyenMai().getPhanTramGiam();
                        priceWrapper[0] = priceWrapper[0] - (priceWrapper[0] * phanTramGiam / 100);
                    }
                    ci.setPrice(priceWrapper[0]); 
                });
            }
        }
        return cart;
    }

    /**
     * So sánh 2 item trong giỏ có phải cùng một sản phẩm-cấu hình hay không.
     * Điều kiện trùng: cùng productId và cùng variantId (kể cả null).
     */
    private boolean isSameItem(CartItem a, CartItem b) {
        if (!a.getId().equals(b.getId())) return false;
        return Objects.equals(a.getVariantId(), b.getVariantId());
    }

    /**
     * Thêm sản phẩm vào giỏ hàng.
     * 
     * Ràng buộc nghiệp vụ (Business Rules):
     * 1. Sản phẩm phải đang ở trạng thái ACTIVE (đang kinh doanh).
     * 2. Số lượng phải lớn hơn 0.
     * 3. Số lượng thêm vào không được vượt quá tồn kho thực tế (của sản phẩm hoặc biến thể).
     * 4. Nếu sản phẩm đã có trong giỏ -> Cộng dồn số lượng.
     */
    public void addToCart(HttpSession session, String email, CartItem item) {
        // Kiểm tra tồn tại và trạng thái kinh doanh
        SanPham spCheck = sanPhamRepository.findById(item.getId()).orElse(null);
        if (spCheck == null || !"ACTIVE".equalsIgnoreCase(spCheck.getTrangThai())) {
            throw new RuntimeException("Sản phẩm đã ngừng kinh doanh hoặc không tồn tại!");
        }

        // Chặn số lượng không hợp lệ
        if (item.getQuantity() <= 0) {
            throw new RuntimeException("Số lượng sản phẩm phải lớn hơn 0.");
        }

        // Kiểm tra tồn kho thực tế
        int availableStock = spCheck.getSoLuong();
        if (item.getVariantId() != null) {
            availableStock = bienTheSanPhamRepository.findById(item.getVariantId())
                    .map(BienTheSanPham::getSoLuong)
                    .orElse(0);
        }

        NguoiDung user = getUser(email);
        if (user != null) {
            GioHang gh = getOrCreateGioHang(user);
            Optional<ChiTietGioHang> existing;
            
            // Tìm sản phẩm cùng loại trong giỏ hàng DB
            if (item.getVariantId() != null) {
                existing = chiTietGioHangRepository.findByGioHangAndSanPhamIdAndBienTheId(gh, item.getId(), item.getVariantId());
            } else {
                existing = chiTietGioHangRepository.findByGioHangAndSanPhamIdAndBienTheIsNull(gh, item.getId());
            }

            if (existing.isPresent()) {
                // Đã tồn tại: Cộng dồn và kiểm tra kho
                ChiTietGioHang ct = existing.get();
                if (ct.getSoLuong() + item.getQuantity() > availableStock) {
                    throw new RuntimeException("Số lượng trong kho không đủ (Còn lại: " + availableStock + ").");
                }
                ct.setSoLuong(ct.getSoLuong() + item.getQuantity());
                chiTietGioHangRepository.save(ct);
            } else {
                // Chưa có: Tạo mới
                if (item.getQuantity() > availableStock) {
                    throw new RuntimeException("Số lượng trong kho không đủ (Còn lại: " + availableStock + ").");
                }
                ChiTietGioHang ct = new ChiTietGioHang();
                ct.setGioHang(gh);
                ct.setSanPham(spCheck); 
                if (item.getVariantId() != null) {
                    ct.setBienThe(bienTheSanPhamRepository.findById(item.getVariantId()).orElse(null));
                }
                ct.setGia(item.getPrice());
                ct.setSoLuong(item.getQuantity());
                chiTietGioHangRepository.save(ct);
            }
            // Cập nhật tổng số lượng hiển thị trên Badge icon
            updateTotalQuantity(gh);
            return;
        }

        // Xử lý cho Khách vãng lai (Session)
        List<CartItem> cart = getCart(session, null);
        boolean exists = false;
        for (CartItem cartItem : cart) {
            if (isSameItem(cartItem, item)) {
                if (cartItem.getQuantity() + item.getQuantity() > availableStock) {
                    throw new RuntimeException("Số lượng trong kho không đủ (Còn lại: " + availableStock + ").");
                }
                cartItem.setQuantity(cartItem.getQuantity() + item.getQuantity());
                exists = true;
                break;
            }
        }
        if (!exists) {
            if (item.getQuantity() > availableStock) {
                throw new RuntimeException("Số lượng trong kho không đủ (Còn lại: " + availableStock + ").");
            }
            cart.add(item);
        }
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    /**
     * Xóa một mặt hàng khỏi giỏ hàng.
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
            chiTietGioHangRepository.flush(); 
            updateTotalQuantity(gh);
            return;
        }

        List<CartItem> cart = getCart(session, null);
        cart.removeIf(item -> item.getId().equals(productId) && Objects.equals(item.getVariantId(), variantId));
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    /**
     * Cập nhật số lượng mới cho một mặt hàng.
     * Thường dùng khi khách thay đổi trực tiếp số lượng tại trang Giỏ hàng.
     */
    public void updateQuantity(HttpSession session, String email, Integer productId, Integer variantId, int quantity) {
        NguoiDung user = getUser(email);
        if (user != null) {
            // Xử lý với Database
            GioHang gh = getOrCreateGioHang(user);
            Optional<ChiTietGioHang> existing;
            if (variantId != null) {
                existing = chiTietGioHangRepository.findByGioHangAndSanPhamIdAndBienTheId(gh, productId, variantId);
            } else {
                existing = chiTietGioHangRepository.findByGioHangAndSanPhamIdAndBienTheIsNull(gh, productId);
            }
            if (existing.isPresent()) {
                if (quantity <= 0) {
                    throw new RuntimeException("Số lượng phải lớn hơn 0.");
                }

                // Kiểm tồn kho
                int availableStock = existing.get().getSanPham().getSoLuong();
                if (existing.get().getBienThe() != null) {
                    availableStock = existing.get().getBienThe().getSoLuong();
                }
                if (quantity > availableStock) {
                    throw new RuntimeException("Số lượng vượt quá tồn kho (Còn lại: " + availableStock + ").");
                }

                ChiTietGioHang ct = existing.get();
                ct.setSoLuong(quantity);
                chiTietGioHangRepository.save(ct);
                updateTotalQuantity(gh);
            }
            return;
        }

        // Xử lý với Session
        List<CartItem> cart = getCart(session, null);
        for (CartItem item : cart) {
            if (item.getId().equals(productId) && Objects.equals(item.getVariantId(), variantId)) {
                if (quantity <= 0) {
                    throw new RuntimeException("Số lượng phải lớn hơn 0.");
                }

                SanPham sp = sanPhamRepository.findById(productId).orElse(null);
                int stock = 0;
                if (sp != null) {
                    stock = sp.getSoLuong();
                    if (variantId != null) {
                        stock = bienTheSanPhamRepository.findById(variantId).map(BienTheSanPham::getSoLuong).orElse(0);
                    }
                }
                if (quantity > stock) {
                    throw new RuntimeException("Số lượng vượt quá tồn kho (Còn lại: " + stock + ").");
                }

                item.setQuantity(quantity);
                break;
            }
        }
        session.setAttribute(CART_SESSION_KEY, cart);
    }

    /**
     * Làm trống giỏ hàng (Sau khi đặt hàng thành công).
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
        if (session != null) {
            session.removeAttribute(CART_SESSION_KEY);
        }
    }
    
    /**
     * Lấy tổng giá trị tiền của giỏ hàng.
     */
    public Integer getTotalPrice(HttpSession session, String email) {
        List<CartItem> cart = getCart(session, email);
        return cart.stream().mapToInt(CartItem::getTotalPrice).sum();
    }
    
    /**
     * Lấy tổng số lượng item trong giỏ.
     */
    public int getCount(HttpSession session, String email) {
        List<CartItem> cart = getCart(session, email);
        return cart.stream().mapToInt(CartItem::getQuantity).sum();
    }

    /**
     * Đồng bộ tổng số lượng vào thực thể GioHang để dễ dàng truy vấn nhanh badge badge-count.
     */
    private void updateTotalQuantity(GioHang gh) {
        List<ChiTietGioHang> items = chiTietGioHangRepository.findByGioHang(gh);
        int total = items.stream().mapToInt(ChiTietGioHang::getSoLuong).sum();
        gh.setTongSoLuong(total);
        gioHangRepository.save(gh);
    }
}

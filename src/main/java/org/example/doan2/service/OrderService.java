package org.example.doan2.service;

import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.example.doan2.dto.CartItem;
import org.example.doan2.dto.CheckoutDTO;
import org.example.doan2.entity.BienTheSanPham;
import org.example.doan2.entity.ChiTietDonHang;
import org.example.doan2.entity.DonHang;
import org.example.doan2.entity.NguoiDung;
import org.example.doan2.entity.SanPham;
import org.example.doan2.repository.BienTheSanPhamRepository;
import org.example.doan2.repository.ChiTietDonHangRepository;
import org.example.doan2.repository.DonHangRepository;
import org.example.doan2.repository.NguoiDungRepository;
import org.example.doan2.repository.SanPhamRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private static final String STATUS_CHO_XAC_NHAN = "Ch\u1EDD x\u00E1c nh\u1EADn";
    private static final String STATUS_DA_XAC_NHAN = "\u0110\u00E3 x\u00E1c nh\u1EADn";
    private static final String STATUS_DANG_GIAO = "\u0110ang giao";
    private static final String STATUS_HOAN_THANH = "Ho\u00E0n th\u00E0nh";
    private static final String STATUS_CHO_THANH_TOAN = "Ch\u1EDD thanh to\u00E1n";
    private static final String STATUS_CHUA_THANH_TOAN = "Ch\u01B0a thanh to\u00E1n";
    private static final String STATUS_DA_THANH_TOAN = "\u0110\u00E3 thanh to\u00E1n";
    private static final String STATUS_DA_HUY = "\u0110\u00E3 h\u1EE7y";

    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final CartService cartService;
    private final SanPhamRepository sanPhamRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;
    private final EmailService emailService;

    public OrderService(DonHangRepository donHangRepository,
                        ChiTietDonHangRepository chiTietDonHangRepository,
                        CartService cartService,
                        SanPhamRepository sanPhamRepository,
                        NguoiDungRepository nguoiDungRepository,
                        BienTheSanPhamRepository bienTheSanPhamRepository,
                        EmailService emailService) {
        this.donHangRepository = donHangRepository;
        this.chiTietDonHangRepository = chiTietDonHangRepository;
        this.cartService = cartService;
        this.sanPhamRepository = sanPhamRepository;
        this.nguoiDungRepository = nguoiDungRepository;
        this.bienTheSanPhamRepository = bienTheSanPhamRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void placeOrder(CheckoutDTO checkoutDTO, HttpSession session, String authenticatedEmail) {
        List<CartItem> cartItems = cartService.getCart(session, authenticatedEmail);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Gio hang dang trong!");
        }

        // NGHIỆP VỤ: Kiểm tra thông tin nhận hàng bắt buộc
        validateCheckoutDTO(checkoutDTO);

        // Pre-check stock before creating order to prevent negative inventory.
        validateCartStock(cartItems);

        NguoiDung user = getUserByEmail(authenticatedEmail);

        DonHang donHang = new DonHang();
        donHang.setNguoiDung(user);
        donHang.setTenNguoiNhan(checkoutDTO.getHo() + " " + checkoutDTO.getTen());
        donHang.setDiaChiNhan(checkoutDTO.getDiaChi() + ", " + checkoutDTO.getQuanHuyen() + ", " + checkoutDTO.getThanhPho());
        donHang.setDienThoaiNhan(checkoutDTO.getSoDienThoai());
        donHang.setEmailNhan(checkoutDTO.getEmail());
        donHang.setGhiChu(checkoutDTO.getGhiChu());
        donHang.setPhuongThucThanhToan(checkoutDTO.getPaymentMethod());
        donHang.setTrangThai(STATUS_CHO_XAC_NHAN);
        donHang.setTrangThaiThanhToan(STATUS_CHUA_THANH_TOAN);
        donHang.setNgayTao(LocalDateTime.now());
        donHang.setNgayCapNhat(LocalDateTime.now());
        donHang.setTongTien(cartService.getTotalPrice(session, authenticatedEmail));

        donHang = donHangRepository.save(donHang);

        for (CartItem item : cartItems) {
            SanPham sp = sanPhamRepository.findById(item.getId())
                    .orElseThrow(() -> new RuntimeException("Khong tim thay san pham"));

            BienTheSanPham bienThe = null;
            if (item.getVariantId() != null) {
                bienThe = bienTheSanPhamRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new RuntimeException("Khong tim thay bien the san pham"));
            }

            ChiTietDonHang chiTiet = new ChiTietDonHang();
            chiTiet.setDonHang(donHang);
            chiTiet.setSanPham(sp);
            chiTiet.setBienThe(bienThe);
            chiTiet.setGia(item.getPrice());
            chiTiet.setSoLuong(item.getQuantity());
            chiTietDonHangRepository.save(chiTiet);

            deductStock(sp, bienThe, item.getQuantity());
        }

        emailService.sendOrderConfirmationEmail(checkoutDTO.getEmail(), donHang, cartItems);
        cartService.clearCart(session, authenticatedEmail);
    }

    @Transactional
    public int datDonHangChoThanhToan(CheckoutDTO thongTinDatHang, HttpSession phienLam, String emailDangNhap) {
        List<CartItem> danhSachSanPhamGiohang = cartService.getCart(phienLam, emailDangNhap);
        if (danhSachSanPhamGiohang.isEmpty()) {
            throw new RuntimeException("Gio hang dang trong!");
        }

        // NGHIỆP VỤ: Kiểm tra thông tin nhận hàng trước khi sang VNPay
        validateCheckoutDTO(thongTinDatHang);

        // VNPay flow also validates stock before creating pending order.
        validateCartStock(danhSachSanPhamGiohang);

        NguoiDung nguoiDung = getUserByEmail(emailDangNhap);

        DonHang donHangMoi = new DonHang();
        donHangMoi.setNguoiDung(nguoiDung);
        donHangMoi.setTenNguoiNhan(thongTinDatHang.getHo() + " " + thongTinDatHang.getTen());
        donHangMoi.setDiaChiNhan(thongTinDatHang.getDiaChi() + ", " + thongTinDatHang.getQuanHuyen() + ", " + thongTinDatHang.getThanhPho());
        donHangMoi.setDienThoaiNhan(thongTinDatHang.getSoDienThoai());
        donHangMoi.setEmailNhan(thongTinDatHang.getEmail());
        donHangMoi.setGhiChu(thongTinDatHang.getGhiChu());
        donHangMoi.setPhuongThucThanhToan("VNPay");
        donHangMoi.setTrangThai(STATUS_CHO_THANH_TOAN);
        donHangMoi.setTrangThaiThanhToan(STATUS_CHUA_THANH_TOAN);
        donHangMoi.setNgayTao(LocalDateTime.now());
        donHangMoi.setNgayCapNhat(LocalDateTime.now());
        donHangMoi.setTongTien(cartService.getTotalPrice(phienLam, emailDangNhap));

        donHangMoi = donHangRepository.save(donHangMoi);

        for (CartItem sanPhamTrongGio : danhSachSanPhamGiohang) {
            ChiTietDonHang chiTietDonHang = new ChiTietDonHang();
            chiTietDonHang.setDonHang(donHangMoi);

            SanPham sp = sanPhamRepository.findById(sanPhamTrongGio.getId())
                    .orElseThrow(() -> new RuntimeException("Khong tim thay san pham"));
            chiTietDonHang.setSanPham(sp);

            if (sanPhamTrongGio.getVariantId() != null) {
                BienTheSanPham bienTheSanPham = bienTheSanPhamRepository
                        .findById(sanPhamTrongGio.getVariantId())
                        .orElseThrow(() -> new RuntimeException("Khong tim thay bien the san pham"));
                chiTietDonHang.setBienThe(bienTheSanPham);
            }

            chiTietDonHang.setGia(sanPhamTrongGio.getPrice());
            chiTietDonHang.setSoLuong(sanPhamTrongGio.getQuantity());
            chiTietDonHangRepository.save(chiTietDonHang);
        }

        return donHangMoi.getId();
    }

    @Transactional
    public boolean xacNhanDonHangVNPayThanhCong(int maDonHang, String maThanhToanVNP) {
        DonHang donHang = donHangRepository.findById(maDonHang).orElse(null);
        if (donHang == null) {
            return false;
        }

        // Idempotency guard: ignore duplicate success callbacks from gateway.
        if (STATUS_DA_THANH_TOAN.equalsIgnoreCase(donHang.getTrangThaiThanhToan())) {
            return false;
        }

        if (STATUS_DA_HUY.equalsIgnoreCase(donHang.getTrangThai())) {
            return false;
        }

        List<ChiTietDonHang> chiTietList = chiTietDonHangRepository.findByDonHang(donHang);
        for (ChiTietDonHang chiTiet : chiTietList) {
            int quantity = chiTiet.getSoLuong() == null ? 0 : chiTiet.getSoLuong();
            if (quantity <= 0) {
                throw new RuntimeException("So luong san pham trong don hang khong hop le");
            }

            SanPham sp = chiTiet.getSanPham();
            if (sp == null) {
                throw new RuntimeException("Du lieu don hang khong hop le: thieu san pham");
            }
            if (sp.getSoLuong() == null || sp.getSoLuong() < quantity) {
                throw new RuntimeException("San pham khong du ton kho de xac nhan thanh toan");
            }

            BienTheSanPham bt = chiTiet.getBienThe();
            if (bt != null && (bt.getSoLuong() == null || bt.getSoLuong() < quantity)) {
                throw new RuntimeException("Bien the san pham khong du ton kho de xac nhan thanh toan");
            }
        }

        donHang.setTrangThaiThanhToan(STATUS_DA_THANH_TOAN);
        donHang.setTrangThai(STATUS_CHO_XAC_NHAN);
        donHang.setMaThanhToan(maThanhToanVNP);
        donHang.setNgayCapNhat(LocalDateTime.now());
        donHangRepository.save(donHang);

        for (ChiTietDonHang chiTiet : chiTietList) {
            deductStock(chiTiet.getSanPham(), chiTiet.getBienThe(), chiTiet.getSoLuong());
        }

        // Xóa giỏ hàng trong Database ngay khi xác nhận thanh toán thành công
        if (donHang.getNguoiDung() != null) {
            cartService.clearCart(null, donHang.getNguoiDung().getEmail());
        }
        return true;
    }

    @Transactional
    public void cancelOrder(int orderId, String actorEmail) {
        DonHang order = donHangRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        // Nếu là khách hàng, chỉ được hủy đơn "Chờ xác nhận" hoặc "Chờ thanh toán"
        NguoiDung actor = getUserByEmail(actorEmail);
        boolean isAdmin = actor != null && actor.getVaiTro() != null && 
                (actor.getVaiTro().getTenVaiTro().equals("ROLE_ADMIN") || actor.getVaiTro().getTenVaiTro().equals("ROLE_EMPLOYEE"));

        if (!isAdmin) {
            if (!order.getNguoiDung().getEmail().equalsIgnoreCase(actorEmail)) {
                throw new RuntimeException("Bạn không có quyền hủy đơn hàng này");
            }
            if (!STATUS_CHO_XAC_NHAN.equals(order.getTrangThai()) && !STATUS_CHO_THANH_TOAN.equals(order.getTrangThai())) {
                throw new RuntimeException("Chỉ có thể hủy đơn hàng đang chờ xác nhận hoặc chờ thanh toán");
            }
        }

        if (STATUS_DA_HUY.equals(order.getTrangThai())) {
            return; // Đã hủy rồi thì thôi
        }

        // Nếu đơn hàng ĐÃ THANH TOÁN mà bị hủy -> Cần lưu ý luồng hoàn tiền (Refund) thủ công
        // Ở đây mình chỉ xử lý chuyển trạng thái và hoàn tồn kho.
        
        order.setTrangThai(STATUS_DA_HUY);
        order.setNgayCapNhat(LocalDateTime.now());
        donHangRepository.save(order);

        restoreStock(order);
    }

    @Transactional
    public void updateStatus(int orderId, String newStatus) {
        DonHang order = donHangRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (STATUS_DA_HUY.equals(newStatus)) {
            cancelOrder(orderId, null); // Thực hiện hủy đơn và hoàn tồn kho
            return;
        }

        order.setTrangThai(newStatus);
        order.setNgayCapNhat(LocalDateTime.now());
        donHangRepository.save(order);
    }

    @Transactional
    public void confirmCodPayment(int orderId) {
        DonHang order = donHangRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));
        
        if (!"COD".equalsIgnoreCase(order.getPhuongThucThanhToan())) {
            throw new RuntimeException("Chỉ áp dụng xác nhận thanh toán cho đơn COD");
        }

        order.setTrangThaiThanhToan(STATUS_DA_THANH_TOAN);
        order.setNgayCapNhat(LocalDateTime.now());
        donHangRepository.save(order);
    }

    private void restoreStock(DonHang order) {
        List<ChiTietDonHang> items = chiTietDonHangRepository.findByDonHang(order);
        for (ChiTietDonHang item : items) {
            int quantity = item.getSoLuong();
            SanPham sp = item.getSanPham();
            if (sp != null) {
                sp.setSoLuong(sp.getSoLuong() + quantity);
                sp.setDaBan(Math.max(0, (sp.getDaBan() == null ? 0 : sp.getDaBan()) - quantity));
                sanPhamRepository.save(sp);
            }

            BienTheSanPham bt = item.getBienThe();
            if (bt != null) {
                bt.setSoLuong(bt.getSoLuong() + quantity);
                bt.setDaBan(Math.max(0, (bt.getDaBan() == null ? 0 : bt.getDaBan()) - quantity));
                bienTheSanPhamRepository.save(bt);
            }
        }
    }


    private NguoiDung getUserByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        return nguoiDungRepository.findByEmail(email).orElse(null);
    }

    // Centralized stock validation used by COD and VNPay pending flow.
    private void validateCartStock(List<CartItem> cartItems) {
        for (CartItem item : cartItems) {
            int quantity = item.getQuantity();
            if (quantity <= 0) {
                throw new RuntimeException("So luong san pham khong hop le");
            }

            SanPham sp = sanPhamRepository.findById(item.getId())
                    .orElseThrow(() -> new RuntimeException("Khong tim thay san pham"));

            if (sp.getSoLuong() == null || sp.getSoLuong() < quantity) {
                throw new RuntimeException("San pham " + sp.getTenSanPham() + " khong du ton kho");
            }

            if (item.getVariantId() != null) {
                BienTheSanPham bienThe = bienTheSanPhamRepository.findById(item.getVariantId())
                        .orElseThrow(() -> new RuntimeException("Khong tim thay bien the san pham"));

                if (bienThe.getSanPham() == null || !bienThe.getSanPham().getId().equals(sp.getId())) {
                    throw new RuntimeException("Bien the khong thuoc san pham da chon");
                }

                if (bienThe.getSoLuong() == null || bienThe.getSoLuong() < quantity) {
                    throw new RuntimeException("Bien the cua " + sp.getTenSanPham() + " khong du ton kho");
                }
            }
        }
    }

    // Single place that deducts stock with guard checks for product and variant.
    private void deductStock(SanPham sp, BienTheSanPham bienThe, int quantity) {
        if (quantity <= 0) {
            throw new RuntimeException("So luong san pham khong hop le");
        }

        if (sp.getSoLuong() == null || sp.getSoLuong() < quantity) {
            throw new RuntimeException("San pham khong du ton kho");
        }
        sp.setSoLuong(sp.getSoLuong() - quantity);
        sp.setDaBan((sp.getDaBan() == null ? 0 : sp.getDaBan()) + quantity);
        sanPhamRepository.save(sp);

        if (bienThe != null) {
            if (bienThe.getSoLuong() == null || bienThe.getSoLuong() < quantity) {
                throw new RuntimeException("Bien the san pham khong du ton kho");
            }
            bienThe.setSoLuong(bienThe.getSoLuong() - quantity);
            bienThe.setDaBan((bienThe.getDaBan() == null ? 0 : bienThe.getDaBan()) + quantity);
            bienTheSanPhamRepository.save(bienThe);
        }
    }

    private void validateCheckoutDTO(CheckoutDTO dto) {
        if (dto == null) {
            throw new RuntimeException("Thông tin nhận hàng không hợp lệ!");
        }
        if (isStringEmpty(dto.getHo()) || isStringEmpty(dto.getTen())) {
            throw new RuntimeException("Vui lòng nhập đầy đủ Họ và Tên!");
        }
        if (isStringEmpty(dto.getDiaChi())) {
            throw new RuntimeException("Vui lòng nhập địa chỉ nhận hàng!");
        }
        if (isStringEmpty(dto.getThanhPho()) || isStringEmpty(dto.getQuanHuyen())) {
            throw new RuntimeException("Vui lòng nhập đầy đủ Quận/Huyện, Tỉnh/Thành phố!");
        }
        if (isStringEmpty(dto.getSoDienThoai())) {
            throw new RuntimeException("Vui lòng nhập số điện thoại liên lạc!");
        }
        if (isStringEmpty(dto.getEmail())) {
            throw new RuntimeException("Vui lòng nhập Email để nhận thông báo đơn hàng!");
        }
    }

    private boolean isStringEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}

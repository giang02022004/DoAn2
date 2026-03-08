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
            throw new RuntimeException("Giỏ hàng đang trống!");
        }

        NguoiDung user = null;
        if (authenticatedEmail != null) {
            user = nguoiDungRepository.findByEmail(authenticatedEmail).orElse(null);
        }

        DonHang donHang = new DonHang();
        donHang.setNguoiDung(user);
        donHang.setTenNguoiNhan(checkoutDTO.getHo() + " " + checkoutDTO.getTen());
        donHang.setDiaChiNhan(checkoutDTO.getDiaChi() + ", " + checkoutDTO.getQuanHuyen() + ", " + checkoutDTO.getThanhPho());
        donHang.setDienThoaiNhan(checkoutDTO.getSoDienThoai());
        donHang.setEmailNhan(checkoutDTO.getEmail());
        donHang.setGhiChu(checkoutDTO.getGhiChu());
        donHang.setPhuongThucThanhToan(checkoutDTO.getPaymentMethod());
        donHang.setTrangThai("Chờ xác nhận");
        donHang.setTrangThaiThanhToan("Chưa thanh toán");
        donHang.setNgayTao(LocalDateTime.now());
        donHang.setNgayCapNhat(LocalDateTime.now());
        donHang.setTongTien(cartService.getTotalPrice(session, authenticatedEmail));

        donHang = donHangRepository.save(donHang);

        for (CartItem item : cartItems) {
            ChiTietDonHang chiTiet = new ChiTietDonHang();
            chiTiet.setDonHang(donHang);
            
            SanPham sp = sanPhamRepository.findById(item.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
            chiTiet.setSanPham(sp);
            
            if (item.getVariantId() != null) {
                BienTheSanPham bienThe = bienTheSanPhamRepository.findById(item.getVariantId()).orElse(null);
                chiTiet.setBienThe(bienThe);
                
                if (bienThe != null) {
                    // Trừ tồn kho biến thể
                    bienThe.setSoLuong(bienThe.getSoLuong() - item.getQuantity());
                    bienThe.setDaBan((bienThe.getDaBan() == null ? 0 : bienThe.getDaBan()) + item.getQuantity());
                    bienTheSanPhamRepository.save(bienThe);
                }
            }

            chiTiet.setGia(item.getPrice());
            chiTiet.setSoLuong(item.getQuantity());

            chiTietDonHangRepository.save(chiTiet);
            
            // Trừ tồn kho sản phẩm chính
            sp.setSoLuong(sp.getSoLuong() - item.getQuantity());
            sp.setDaBan((sp.getDaBan() == null ? 0 : sp.getDaBan()) + item.getQuantity());
            sanPhamRepository.save(sp);
        }

        // Gửi email xác nhận đặt hàng HTML đến email khách điền trên form (chứ không phải email tài khoản đăng nhập)
        emailService.sendOrderConfirmationEmail(checkoutDTO.getEmail(), donHang, cartItems);

        cartService.clearCart(session, authenticatedEmail);
    }

    /**
     * Đặt đơn hàng dành riêng cho luồng thanh toán VNPay.
     *
     * Khác với placeOrder() thông thường (COD), phương thức này:
     *   - Lưu đơn hàng với trạng thái "Chờ thanh toán" (chờ VNPay xác nhận)
     *   - KHÔNG xóa giỏ hàng (chỉ xóa sau khi VNPay callback thành công)
     *   - KHÔNG gửi email (email gửi sau khi VNPay xác nhận)
     *
     * @return ID của đơn hàng vừa tạo (để chuyển sang trang VNPay)
     */
    @Transactional
    public int datDonHangChoThanhToan(CheckoutDTO thongTinDatHang, HttpSession phienLam, String emailDangNhap) {

        List<CartItem> danhSachSanPhamGiohang = cartService.getCart(phienLam, emailDangNhap);

        if (danhSachSanPhamGiohang.isEmpty()) {
            throw new RuntimeException("Giỏ hàng đang trống!");
        }

        NguoiDung nguoiDung = null;
        if (emailDangNhap != null) {
            nguoiDung = nguoiDungRepository.findByEmail(emailDangNhap).orElse(null);
        }

        // Tạo đơn hàng với trạng thái "Chờ thanh toán" — chưa hoàn tất
        DonHang donHangMoi = new DonHang();
        donHangMoi.setNguoiDung(nguoiDung);
        donHangMoi.setTenNguoiNhan(thongTinDatHang.getHo() + " " + thongTinDatHang.getTen());
        donHangMoi.setDiaChiNhan(thongTinDatHang.getDiaChi() + ", " + thongTinDatHang.getQuanHuyen() + ", " + thongTinDatHang.getThanhPho());
        donHangMoi.setDienThoaiNhan(thongTinDatHang.getSoDienThoai());
        donHangMoi.setEmailNhan(thongTinDatHang.getEmail());
        donHangMoi.setGhiChu(thongTinDatHang.getGhiChu());
        donHangMoi.setPhuongThucThanhToan("VNPay");
        donHangMoi.setTrangThai("Chờ thanh toán");
        donHangMoi.setTrangThaiThanhToan("Chưa thanh toán");
        donHangMoi.setNgayTao(LocalDateTime.now());
        donHangMoi.setNgayCapNhat(LocalDateTime.now());
        donHangMoi.setTongTien(cartService.getTotalPrice(phienLam, emailDangNhap));

        donHangMoi = donHangRepository.save(donHangMoi);

        // Lưu chi tiết từng sản phẩm trong giỏ hàng vào đơn hàng
        for (CartItem sanPhamTrongGio : danhSachSanPhamGiohang) {
            ChiTietDonHang chiTietDonHang = new ChiTietDonHang();
            chiTietDonHang.setDonHang(donHangMoi);

            SanPham sp = sanPhamRepository.findById(sanPhamTrongGio.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
            chiTietDonHang.setSanPham(sp);

            if (sanPhamTrongGio.getVariantId() != null) {
                BienTheSanPham bienTheSanPham = bienTheSanPhamRepository
                        .findById(sanPhamTrongGio.getVariantId()).orElse(null);
                chiTietDonHang.setBienThe(bienTheSanPham);

                if (bienTheSanPham != null) {
                    // Không trừ tồn kho biến thể ngay lúc tạo đơn chờ
                }
            }

            chiTietDonHang.setGia(sanPhamTrongGio.getPrice());
            chiTietDonHang.setSoLuong(sanPhamTrongGio.getQuantity());
            chiTietDonHangRepository.save(chiTietDonHang);

            // Không trừ tồn kho sản phẩm chính lúc tạo đơn chờ
        }

        // Trả về ID đơn hàng để chuyển hướng sang VNPay
        return donHangMoi.getId();
    }

    /**
     * Cập nhật trạng thái và trừ tồn kho sau khi VNPay thanh toán thành công.
     */
    @Transactional
    public void xacNhanDonHangVNPayThanhCong(int maDonHang, String maThanhToanVNP) {
        DonHang donHang = donHangRepository.findById(maDonHang).orElse(null);
        if (donHang != null) {
            // Cập nhật trạng thái hoá đơn
            donHang.setTrangThaiThanhToan("Đã thanh toán");
            donHang.setTrangThai("Chờ xác nhận");
            donHang.setMaThanhToan(maThanhToanVNP);
            donHang.setNgayCapNhat(LocalDateTime.now());
            donHangRepository.save(donHang);

            // Tiến hành trừ số lượng tồn kho và cộng dồn số lượng đã bán
            List<ChiTietDonHang> chiTietList = chiTietDonHangRepository.findByDonHang(donHang);
            for (ChiTietDonHang chiTiet : chiTietList) {
                SanPham sp = chiTiet.getSanPham();
                if (sp != null) {
                    sp.setSoLuong(sp.getSoLuong() - chiTiet.getSoLuong());
                    sp.setDaBan((sp.getDaBan() == null ? 0 : sp.getDaBan()) + chiTiet.getSoLuong());
                    sanPhamRepository.save(sp);
                }

                BienTheSanPham bt = chiTiet.getBienThe();
                if (bt != null) {
                    bt.setSoLuong(bt.getSoLuong() - chiTiet.getSoLuong());
                    bt.setDaBan((bt.getDaBan() == null ? 0 : bt.getDaBan()) + chiTiet.getSoLuong());
                    bienTheSanPhamRepository.save(bt);
                }
            }
        }
    }
}

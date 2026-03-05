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
}

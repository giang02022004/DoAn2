package org.example.doan2.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CheckoutDTO {
    private String ho;
    private String ten;
    private String tenCongTy;
    private String diaChi;
    private String thanhPho;
    private String quanHuyen;
    private String zipCode;
    private String soDienThoai;
    private String email;
    private boolean createAccount;
    private String ghiChu;
    private String paymentMethod;
}

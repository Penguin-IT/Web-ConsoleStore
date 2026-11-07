package com.example.consolestore.Service;

import com.example.consolestore.Reponsitory.NhanVienReponsitory;
import com.example.consolestore.Reponsitory.TaiKhoanReponsitory;
import org.springframework.stereotype.Service;

@Service
public class NhanVienService {
    private final NhanVienReponsitory nhanVienReponsitory;
    private final TaiKhoanReponsitory taiKhoanReponsitory;

    public NhanVienService(NhanVienReponsitory nhanVienReponsitory, TaiKhoanReponsitory taiKhoanReponsitory) {
        this.nhanVienReponsitory = nhanVienReponsitory;
        this.taiKhoanReponsitory = taiKhoanReponsitory;
    }
}

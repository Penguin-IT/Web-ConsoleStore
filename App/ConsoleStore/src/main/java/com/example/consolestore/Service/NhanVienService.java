package com.example.consolestore.Service;

import com.example.consolestore.Repository.NhanVienRepository;
import com.example.consolestore.Repository.TaiKhoanRepository;
import org.springframework.stereotype.Service;

@Service
public class NhanVienService {
    private final NhanVienRepository nhanVienRepository;
    private final TaiKhoanRepository taiKhoanRepository;

    public NhanVienService(NhanVienRepository nhanVienRepository, TaiKhoanRepository taiKhoanRepository) {
        this.nhanVienRepository = nhanVienRepository;
        this.taiKhoanRepository = taiKhoanRepository;
    }
}

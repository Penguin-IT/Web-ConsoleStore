package com.example.consolestore.Reponsitory;

import com.example.consolestore.Entity.TaiKhoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TaiKhoanReponsitory extends JpaRepository<TaiKhoan,Long> {

}

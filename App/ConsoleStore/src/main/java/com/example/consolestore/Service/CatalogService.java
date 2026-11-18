package com.example.consolestore.Service;

import com.example.consolestore.DTO.*;
import com.example.consolestore.Entity.*;
import com.example.consolestore.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class CatalogService {
    private final SanPhamRepository products;
    private final TonKhoRepository stocks;
    private final LoaiSanPhamRepository categories;
    private final NenTangRepository platforms;
    private static final Set<String> SORTS=Set.of("recommended","priceAsc","priceDesc","nameAsc","nameDesc");
    public CatalogService(SanPhamRepository products,TonKhoRepository stocks,LoaiSanPhamRepository categories,NenTangRepository platforms) {
        this.products=products;this.stocks=stocks;this.categories=categories;this.platforms=platforms;
    }
    public void validate(CatalogSearch f, Errors errors) {
        f.setQ(clean(f.getQ()));f.setCategory(clean(f.getCategory()));f.setPlatform(clean(f.getPlatform()));
        f.setCondition(clean(f.getCondition()));f.setSort(clean(f.getSort()));
        if(f.getPlatform().equals("Nintendo"))f.setPlatform("Nintendo Switch");
        if(f.getCategory().equals("Máy chơi game"))f.setCategory("Máy console");
        if(f.getCondition().equals("Mới"))f.setCondition("MOI");
        if(f.getCondition().equals("Cũ"))f.setCondition("CU");
        if(f.getCategoryId()==null&&!f.getCategory().isBlank())categories.findByTenLoai(f.getCategory()).ifPresent(c->{f.setCategoryId(c.getId());f.setCategory("");});
        if(f.getPlatformId()==null&&!f.getPlatform().isBlank())platforms.findByTenNenTang(f.getPlatform()).ifPresent(p->{f.setPlatformId(p.getId());f.setPlatform("");});
        if(f.getSort().equals("name"))f.setSort("nameAsc");
        if(f.getSort().isBlank())f.setSort("recommended");
        if(f.getQ().length()>100)errors.rejectValue("q","length","Từ khóa tối đa 100 ký tự.");
        if(!Set.of("","MOI","CU").contains(f.getCondition()))errors.rejectValue("condition","invalid","Tình trạng không hợp lệ.");
        if(!SORTS.contains(f.getSort()))errors.rejectValue("sort","invalid","Cách sắp xếp không hợp lệ.");
        for(String key:List.of("minPrice","maxPrice")) {
            BigDecimal value=key.equals("minPrice")?f.getMinPrice():f.getMaxPrice();
            if(value!=null&&(value.signum()<0||value.compareTo(new BigDecimal("999999999999999999"))>0||value.stripTrailingZeros().scale()>0))
                errors.rejectValue(key,"range","Giá phải là số nguyên từ 0 đến 999999999999999999.");
        }
        if(f.getMinPrice()!=null&&f.getMaxPrice()!=null&&f.getMinPrice().compareTo(f.getMaxPrice())>0)
            errors.rejectValue("maxPrice","range","Giá đến phải lớn hơn hoặc bằng giá từ.");
        if(f.getPage()==null||f.getPage()<0||f.getPage()>100000)errors.rejectValue("page","range","Số trang không hợp lệ.");
        if(f.getSize()==null||!Set.of(9,12,24).contains(f.getSize()))errors.rejectValue("size","range","Chọn 9, 12 hoặc 24 sản phẩm mỗi trang.");
    }
    public Page<ProductCard> search(CatalogSearch f) {
        Sort sort=switch(f.getSort()) {
            case "priceAsc"->Sort.by("giaBan").ascending();
            case "priceDesc"->Sort.by("giaBan").descending();
            case "nameAsc"->Sort.by("tenSP").ascending();
            case "nameDesc"->Sort.by("tenSP").descending();
            default->Sort.by("maSP");
        };
        if(sort.getOrderFor("maSP")==null)sort=sort.and(Sort.by("maSP"));
        Page<SanPham> found=products.findAll(SanPhamSpecifications.search(f),PageRequest.of(f.getPage(),f.getSize(),sort));
        if(found.getTotalPages()>0&&f.getPage()>=found.getTotalPages()) {
            f.setPage(found.getTotalPages()-1);
            found=products.findAll(SanPhamSpecifications.search(f),PageRequest.of(f.getPage(),f.getSize(),sort));
        }
        List<Long> ids=found.getContent().stream().map(SanPham::getMaSP).toList();
        Map<Long,Long> totals=ids.isEmpty()?Map.of():stocks.sumByProductIds(ids).stream()
                .collect(Collectors.toMap(TonKhoRepository.StockTotal::getProductId,TonKhoRepository.StockTotal::getQuantity));
        return found.map(p->new ProductCard(p.getMaSP(),displayName(p),p.getNenTang()==null?"Khác":p.getNenTang().getName(),
                p.getLoaiSanPham().getName(),p.getGiaBan(),imagePath(p.getHinhAnh()),p.getTinhTrang(),totals.getOrDefault(p.getMaSP(),0L)));
    }
    public List<ProductCard> featured(String category,int size) {
        CatalogSearch f=new CatalogSearch();f.setCategory(category);f.setCondition("MOI");
        return search(f).getContent().stream().limit(size).toList();
    }
    public List<LoaiSanPham> categories(){return categories.findAll(Sort.by("maLoai"));}
    public List<NenTang> platforms(){return platforms.findAll(Sort.by("maNenTang"));}
    //
    private String displayName(SanPham product) {
        String name=product.getTenSP();
        String suffix=" - Hàng cũ";
        if("CU".equals(product.getTinhTrang())&&product.getMaSPGoc()!=null&&name.endsWith(suffix))
            return name.substring(0,name.length()-suffix.length());
        return name;
    }
    static String clean(String value){return value==null?"":value.strip();}
    public static String imagePath(String filename) {
        String value=clean(filename);
        if(value.isBlank())return "/images/ui/product-placeholder.svg";
        if(value.startsWith("/images/"))return value;
        if(value.startsWith("images/"))return "/"+value;

        if(value.matches("[^/\\\\:]+\\.(?i:jpg|jpeg|png|webp|avif|gif)"))return "/images/products/"+value;
        return "/images/ui/product-placeholder.svg";
    }
    public Optional<ProductDetail> detail(Long id) {
        if (id == null || id <= 0) {
            return Optional.empty();
        }

        return products.findByMaSPAndDeletedFalse(id).map(product -> {
            long stock = stocks.sumByProductIds(List.of(id))
                    .stream()
                    .mapToLong(TonKhoRepository.StockTotal::getQuantity)
                    .sum();

            ProductCard card = new ProductCard(
                    product.getMaSP(),
                    displayName(product),
                    product.getNenTang() == null
                            ? "Khác"
                            : product.getNenTang().getName(),
                    product.getLoaiSanPham().getName(),
                    product.getGiaBan(),
                    imagePath(product.getHinhAnh()),
                    product.getTinhTrang(),
                    stock
            );

            String description = clean(product.getMoTa());

            if (description.isBlank()) {
                description = "Thông tin sản phẩm đang được cập nhật.";
            }

            return new ProductDetail(card, description);
        });
    }
}

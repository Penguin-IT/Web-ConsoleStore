package com.example.consolestore.DTO;

import java.math.BigDecimal;

/** Bo loc nguoi dung gui tu form tim kiem. */
public class CatalogSearch {
    private String q="";
    private Long categoryId=null;
    private Long platformId=null;
    private String category="";
    private String platform="";
    private String condition="";
    private BigDecimal minPrice=null;
    private BigDecimal maxPrice=null;
    private Integer page=0;
    private Integer size=9;
    private String sort="recommended";
    public String getQ(){return q;}
    public void setQ(String value){q=value;}
    public Long getCategoryId(){return categoryId;}
    public void setCategoryId(Long value){categoryId=value;}
    public Long getPlatformId(){return platformId;}
    public void setPlatformId(Long value){platformId=value;}
    public String getCategory(){return category;}
    public void setCategory(String value){category=value;}
    public String getPlatform(){return platform;}
    public void setPlatform(String value){platform=value;}
    public String getCondition(){return condition;}
    public void setCondition(String value){condition=value;}
    public BigDecimal getMinPrice(){return minPrice;}
    public void setMinPrice(BigDecimal value){minPrice=value;}
    public BigDecimal getMaxPrice(){return maxPrice;}
    public void setMaxPrice(BigDecimal value){maxPrice=value;}
    public Integer getPage(){return page;}
    public void setPage(Integer value){page=value;}
    public Integer getSize(){return size;}
    public void setSize(Integer value){size=value;}
    public String getSort(){return sort;}
    public void setSort(String value){sort=value;}
}

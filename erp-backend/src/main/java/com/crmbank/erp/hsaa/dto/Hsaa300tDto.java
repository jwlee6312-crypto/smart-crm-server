package com.crmbank.erp.hsaa.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class Hsaa300tDto {
    private String cmpycd;
    private String deptcd;
    private String userid;
    private String custcd;
    private String salesid;
    private String ser;
    private String contdt;
    private String title;
    private Integer conthh;
    private Integer contmm;
    private String channel;
    private String channelkind;
    private String custid;
    private String custnm;
    private String channelnm;
    private String content;
    private String tostate;
    private String statenm;
    private String chngdt;
    private String remark;
    private String reportyn;
    private String reportcontent;
    private String reportcd;
    private String coachingcd;
    private String coachingreadyn;
    private String salescoaching;
    private String startdate;
    private String diarycontent;
    private String wincd;
    private String failcd;
    private String holdondt;
    private String holdcd;
    private BigDecimal realamt;
    private String realdt;
    private String hpno;
    private String custtel;
    private String usernm;
    private String addtime;
    private String updtime;
    private String updemp;

    private String actionitem;
    private String actionstatus;
    private String nextmeetdt;
    private String nextmeetagenda;
}

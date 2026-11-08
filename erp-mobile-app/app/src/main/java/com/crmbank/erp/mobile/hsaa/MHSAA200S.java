package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.RetrofitClient;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSAA200S] 영업실적 현황
 */
@SuppressLint({"SetTextI18n", "NotifyDataSetChanged", "DefaultLocale", "SimpleDateFormat"})
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class MHSAA200S extends BaseActivity {

    private TextView tvYymm, tvPlanAmt, tvRealAmtSum;
    private TextView tvSuccessCnt, tvFailCnt, tvHoldCnt, tvGiveUpCnt;
    private Spinner spUser;
    private LinearLayout llPerfList;

    private ApiService apiService;
    private String cmpycd, currentUserId, userGrp;
    private final List<Map<String, Object>> userList = new ArrayList<>();
    private final List<Map<String, Object>> perfList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa200s);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        currentUserId = prefs.getString("userId", "");
        userGrp = prefs.getString("usergrp", "");
        apiService = RetrofitClient.getApiService();

        initViews();
        loadUsers();
        
        // 초기 년월 설정 (당월)
        tvYymm.setText(new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date()));
        
        search();
    }

    private void initViews() {
        tvYymm = findViewById(R.id.tvYymm);
        tvPlanAmt = findViewById(R.id.tvPlanAmt);
        tvRealAmtSum = findViewById(R.id.tvRealAmtSum);
        
        tvSuccessCnt = findViewById(R.id.tvSuccessCnt);
        tvFailCnt = findViewById(R.id.tvFailCnt);
        tvHoldCnt = findViewById(R.id.tvHoldCnt);
        tvGiveUpCnt = findViewById(R.id.tvGiveUpCnt);
        
        spUser = findViewById(R.id.spUser);
        llPerfList = findViewById(R.id.llPerfList);

        findViewById(R.id.btnSearch).setOnClickListener(v -> search());
    }

    private void loadUsers() {
        apiService.getHsaaUsers().enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userList.clear();
                    
                    // 관리자 그룹인 경우만 전체 조회 및 담당자 변경 허용
                    // 허용 코드: '000', '610', '660'
                    boolean isManager = "000".equals(userGrp) || "610".equals(userGrp) || "660".equals(userGrp);
                    
                    if (isManager) {
                        Map<String, Object> all = new HashMap<>();
                        all.put("userid", ""); all.put("usernm", "전체");
                        userList.add(all);
                    }

                    userList.addAll(response.body().getData());

                    List<String> names = new ArrayList<>();
                    int defaultIdx = 0;
                    for (int i = 0; i < userList.size(); i++) {
                        Map<String, Object> u = userList.get(i);
                        names.add(getStringVal(u, "usernm"));
                        if (getStringVal(u, "userid").equals(currentUserId)) {
                            defaultIdx = i;
                        }
                    }
                    
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MHSAA200S.this, R.layout.item_spinner, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spUser.setAdapter(adapter);
                    
                    // 기본값으로 본인 선택
                    spUser.setSelection(defaultIdx);
                    
                    // 관리자가 아니면 변경 금지
                    if (!isManager) {
                        spUser.setEnabled(false);
                    }
                    
                    search(); // 유저 로드 후 자동 조회
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    private void search() {
        String dateText = tvYymm.getText().toString();
        if (dateText.isEmpty()) {
            Toast.makeText(this, "년월을 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String yymm = dateText.replace("-", "");
        String targetUserid = "";
        
        if (spUser.getSelectedItemPosition() < 0) {
            Toast.makeText(this, "영업담당을 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        targetUserid = getStringVal(userList.get(spUser.getSelectedItemPosition()), "userid");
        boolean isManager = "000".equals(userGrp) || "610".equals(userGrp) || "660".equals(userGrp);
        if (targetUserid.isEmpty() && !isManager) {
            targetUserid = currentUserId;
        }

        fetchStats(yymm, targetUserid);
        fetchDetails(yymm, targetUserid);
    }

    private void fetchStats(String yymm, String targetUserid) {
        Map<String, Object> p = new HashMap<>();
        p.put("yymm", yymm);
        p.put("userid", targetUserid);
        
        apiService.getHsaaDashboardStats(p).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    DecimalFormat df = new DecimalFormat("#,###");
                    // 초기화
                    tvPlanAmt.setText("0"); tvRealAmtSum.setText("0");
                    tvSuccessCnt.setText("0"); tvFailCnt.setText("0"); tvHoldCnt.setText("0"); tvGiveUpCnt.setText("0");
                    
                    for (Map<String, Object> stat : response.body().getData()) {
                        String gubun = getStringVal(stat, "gubun");
                        String code = getStringVal(stat, "code");
                        double val = getDoubleVal(stat, "val");
                        
                        // 금액 정보 (G6)
                        if ("G6".equals(gubun)) {
                            if ("300".equals(code)) tvPlanAmt.setText(df.format(val)); // 당월목표액
                            if ("200".equals(code)) tvRealAmtSum.setText(df.format(val)); // 당월수주액
                        }
                        
                        // 상태별 건수 (G4)
                        if ("G4".equals(gubun)) {
                            if ("900".equals(code)) tvSuccessCnt.setText(String.valueOf((int)val));
                            if ("910".equals(code)) tvFailCnt.setText(String.valueOf((int)val));
                            if ("920".equals(code)) tvHoldCnt.setText(String.valueOf((int)val));
                            if ("930".equals(code)) tvGiveUpCnt.setText(String.valueOf((int)val));
                        }
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchDetails(String yymm, String targetUserid) {
        Map<String, Object> p = new HashMap<>();
        p.put("yymm", yymm);
        p.put("userid", targetUserid);
        p.put("gubun", "ALL");

        apiService.getHsaaDashboardList(p).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    perfList.clear(); perfList.addAll(response.body().getData());
                    refreshPerfList();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    private void refreshPerfList() {
        llPerfList.removeAllViews();
        DecimalFormat df = new DecimalFormat("#,###");
        for (Map<String, Object> item : perfList) {
            View v = getLayoutInflater().inflate(R.layout.item_mhsaa200s, llPerfList, false);
            ((TextView) v.findViewById(R.id.tvCustNm)).setText(getStringVal(item, "custnm"));
            ((TextView) v.findViewById(R.id.tvSalesTitle)).setText(getStringVal(item, "salestitle"));
            ((TextView) v.findViewById(R.id.tvUserNm)).setText(getStringVal(item, "usernm"));
            
            double fore = getDoubleVal(item, "foreamt");
            double real = getDoubleVal(item, "realamt");
            
            ((TextView) v.findViewById(R.id.tvForeAmt)).setText(df.format(fore));
            ((TextView) v.findViewById(R.id.tvRealAmt)).setText(df.format(real));
            
            TextView tvState = v.findViewById(R.id.tvState);
            tvState.setText(getStringVal(item, "statenm"));
            
            llPerfList.addView(v);
        }
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private double getDoubleVal(Map<String, Object> map, String key) {
        try { return Double.parseDouble(getStringVal(map, key).replace(",", "")); } catch (Exception e) { return 0.0; }
    }

    @Override protected String getProgramTitle() { return "영업실적 현황"; }
    @Override protected String getProgramId() { return "MHSAA200S"; }
}

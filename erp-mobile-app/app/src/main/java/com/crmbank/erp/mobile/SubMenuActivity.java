package com.crmbank.erp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 서브메뉴 관리 (최종 정돈 및 동적 리플렉션 100% 활용 버전)
 * - 하드코딩된 예외 분기 제거 및 동적 클래스 로딩 표준화
 */
public class SubMenuActivity extends BaseActivity {

    private RecyclerView rvSubMenu;
    private SubMenuAdapter adapter;
    private final List<MenuHeader> groupList = new ArrayList<>();
    private String upmucd, upmunm;
    private String cmpycd, usergrp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sub_menu);

        upmucd = getIntent().getStringExtra("UPMUCD");
        upmunm = getIntent().getStringExtra("UPMUNM");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(upmunm != null ? upmunm : "Sub Menu");
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "");
        usergrp = prefs.getString("usergrp", "");

        rvSubMenu = findViewById(R.id.rvSubMenu);
        rvSubMenu.setLayoutManager(new LinearLayoutManager(this));
        rvSubMenu.setHasFixedSize(true);
        
        adapter = new SubMenuAdapter(groupList, program -> {
            String pgmid = getStringVal(program, "pgmid");
            String pgnm = getStringVal(program, "pgmnm");
            if (pgnm.isEmpty()) pgnm = getStringVal(program, "codenm");
            if (!pgmid.isEmpty()) handleProgramClick(pgmid, pgnm);
        });
        rvSubMenu.setAdapter(adapter);

        loadSubMenus();
    }

    private void loadSubMenus() {
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("userid", "MOBILE");
        params.put("upmucd", upmucd);
        params.put("usergrp", usergrp);

        ApiService apiService = RetrofitClient.getApiService();
        apiService.getDynamicMenus(params).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processAndGroupMenus(response.body());
                } else {
                    Toast.makeText(SubMenuActivity.this, "조회 실패", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                Toast.makeText(SubMenuActivity.this, "네트워크 오류", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void processAndGroupMenus(List<Map<String, Object>> rawList) {
        if (rawList == null) return;
        
        groupList.clear();
        Map<String, MenuHeader> groups = new LinkedHashMap<>();

        for (Map<String, Object> item : rawList) {
            if (item == null) continue;
            
            String grpcd = getStringVal(item, "grpcd");
            String grpnm = getStringVal(item, "grpnm");
            
            if (grpcd.isEmpty()) grpcd = "ETC";
            if (grpnm.isEmpty()) grpnm = "기타 메뉴";

            try {
                if (!groups.containsKey(grpcd)) {
                    groups.put(grpcd, new MenuHeader(grpnm));
                }
                groups.get(grpcd).getItems().add(item);
            } catch (Exception e) {
                Log.e("SubMenu", "Error grouping menu: " + e.getMessage());
            }
        }

        groupList.addAll(groups.values());
        adapter.updateData(groupList);
    }

    private void handleProgramClick(String pgmid, String pgnm) {
        if (pgmid != null) {
            pgmid = pgmid.replace("_", "");
        }
        
        // 💡 인텐트 Extra(파라미터) 전달이 필요한 특수 바코드 화면들만 예외 처리
        if ("HSIO104U".equalsIgnoreCase(pgmid) || "MHSIO104U".equalsIgnoreCase(pgmid) || "BARCODE_INBOUND".equalsIgnoreCase(pgmid)) {
            Intent intent = new Intent(this, InboundRegisterActivity.class);
            intent.putExtra("IOGBN_MODE", "100");
            startActivity(intent);
            return;
        }
        if ("HSIO204U".equalsIgnoreCase(pgmid) || "MHSIO204U".equalsIgnoreCase(pgmid) || "BARCODE_OUTBOUND".equalsIgnoreCase(pgmid)) {
            Intent intent = new Intent(this, InboundRegisterActivity.class);
            intent.putExtra("IOGBN_MODE", "200");
            startActivity(intent);
            return;
        }

        // 🚀 표준 동적 리플렉션 로딩 (HSQT100U -> MHSQT100U, HSQT200U -> MHSQT200U 등 자동 매핑)
        String activityName = "M" + pgmid.toUpperCase().replace("_", "");
        String[] modules = {"haba", "hgoa", "hsaa", "hsba", "hmet", "hsqt"};
        boolean found = false;

        for (String module : modules) {
            try {
                String fullClassName = "com.crmbank.erp.mobile." + module + "." + activityName;
                Class<?> activityClass = Class.forName(fullClassName);
                startActivity(new Intent(this, activityClass));
                found = true;
                break;
            } catch (ClassNotFoundException ignored) {}
        }

        if (!found) {
            Intent intent = new Intent(this, CommonWebViewActivity.class);
            intent.putExtra("PGMID", pgmid);
            intent.putExtra("TITLE", pgnm);
            startActivity(intent);
        }
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null || key == null) return "";
        for (String k : map.keySet()) {
            if (key.equalsIgnoreCase(k)) {
                Object val = map.get(k);
                return val != null ? String.valueOf(val).trim() : "";
            }
        }
        return "";
    }

    @Override protected String getProgramTitle() { return upmunm; }
    @Override protected String getProgramId() { return "SUB_MENU"; }
}

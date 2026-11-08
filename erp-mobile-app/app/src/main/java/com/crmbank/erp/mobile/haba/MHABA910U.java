package com.crmbank.erp.mobile.haba;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.RetrofitClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHABA910U] 내 상태 관리 (Phone Forwarding & Working Status)
 */
public class MHABA910U extends BaseActivity {

    private TextView tvInnerNo, tvHpNo, tvUserSub;
    private Spinner spStatus, spRouting;
    private ApiService apiService;
    private String cmpycd, userid;

    private final List<Map<String, String>> statusCodes = new ArrayList<>();
    private final List<Map<String, String>> routingCodes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhaba910u);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "COIT");
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        initViews();
        setupSpinners();
        loadMyInfo();
    }

    private void initViews() {
        tvInnerNo = findViewById(R.id.tvInnerNo);
        tvHpNo = findViewById(R.id.tvHpNo);
        tvUserSub = findViewById(R.id.tvUserSub);
        spStatus = findViewById(R.id.spStatus);
        spRouting = findViewById(R.id.spRouting);

        findViewById(R.id.btnSave).setOnClickListener(v -> save());
    }

    private void setupSpinners() {
        statusCodes.add(createMap("10", "정상 (사무실)"));
        statusCodes.add(createMap("20", "외출 (착신연결)"));
        statusCodes.add(createMap("30", "휴가"));
        statusCodes.add(createMap("40", "퇴근"));

        routingCodes.add(createMap("10", "앱 전용 (MicroSIP)"));
        routingCodes.add(createMap("20", "하이브리드 (20초후 휴대폰)"));
        routingCodes.add(createMap("30", "휴대폰 즉시 연결"));

        setSpinnerAdapter(spStatus, statusCodes);
        setSpinnerAdapter(spRouting, routingCodes);
    }

    private Map<String, String> createMap(String code, String name) {
        Map<String, String> m = new HashMap<>();
        m.put("code", code); m.put("name", name);
        return m;
    }

    private void setSpinnerAdapter(Spinner sp, List<Map<String, String>> data) {
        List<String> names = new ArrayList<>();
        for (Map<String, String> m : data) names.add(m.get("name"));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
    }

    private void loadMyInfo() {
        // 🚀 우선 캐시된 정보 표시 (HP 번호 등 즉시 반영)
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        tvHpNo.setText(prefs.getString("hpno", "-"));
        tvInnerNo.setText(prefs.getString("innerNo", "-"));

        apiService.getMyInfo().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> info = response.body();
                    String hp = getStringVal(info, "hpno");
                    tvInnerNo.setText(getStringVal(info, "inner_no"));
                    tvHpNo.setText(hp);
                    tvUserSub.setText(getStringVal(info, "usernm") + " 님 (" + getStringVal(info, "deptnm") + ")");

                    // SharedPreferences 동기화
                    prefs.edit().putString("hpno", hp).apply();

                    selectSpinner(spStatus, statusCodes, getStringVal(info, "status"));
                    selectSpinner(spRouting, routingCodes, getStringVal(info, "routing_mode"));
                }
            }
            @Override public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {}
        });
    }

    private void selectSpinner(Spinner sp, List<Map<String, String>> data, String code) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i).get("code").equals(code)) {
                sp.setSelection(i); break;
            }
        }
    }

    private void save() {
        Map<String, Object> p = new HashMap<>();
        p.put("status", statusCodes.get(spStatus.getSelectedItemPosition()).get("code"));
        p.put("routing_mode", routingCodes.get(spRouting.getSelectedItemPosition()).get("code"));

        apiService.updateMyStatus(p).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                Toast.makeText(MHABA910U.this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {}
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        Object v = map.get(key.toLowerCase());
        if (v == null) v = map.get(key.toUpperCase());
        return v != null ? String.valueOf(v).trim() : "";
    }

    @Override protected String getProgramTitle() { return "내 상태 관리"; }
    @Override protected String getProgramId() { return "MHABA910U"; }
}

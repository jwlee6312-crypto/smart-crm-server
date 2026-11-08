package com.crmbank.erp.mobile.hsba;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.RetrofitClient;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.CodeDto;
import com.crmbank.erp.mobile.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSBA900U] 코드정보 등록
 * 웹 HSBA900U.vue 로직 기반 모바일 최적화 버전
 */
public class MHSBA900U extends BaseActivity {

    private Spinner spCodeGbn;
    private EditText etCode, etCodeName, etDspOrd, etRemark;
    private Switch swUseYn;
    private CodeAdapter adapter;
    private final List<Map<String, Object>> codeList = new ArrayList<>();
    private final List<CodeDto> groupOptions = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd, userid;
    private String currentActKind = "A0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsba900u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        spCodeGbn = findViewById(R.id.spCodeGbn);
        etCode = findViewById(R.id.etCode);
        etCodeName = findViewById(R.id.etCodeName);
        etDspOrd = findViewById(R.id.etDspOrd);
        etRemark = findViewById(R.id.etRemark);
        swUseYn = findViewById(R.id.swUseYn);
        ListView lvCodeList = findViewById(R.id.lvCodeList);

        adapter = new CodeAdapter();
        lvCodeList.setAdapter(adapter);

        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnReset).setOnClickListener(v -> initializeForm());
        
        spCodeGbn.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fetchCodes();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        lvCodeList.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> item = codeList.get(position);
            loadDetail(item);
        });

        fetchGroupOptions();
    }

    private void initializeForm() {
        currentActKind = "A0";
        etCode.setText("");
        etCode.setEnabled(true);
        etCodeName.setText("");
        etDspOrd.setText("");
        etRemark.setText("");
        swUseYn.setChecked(true);
    }

    private void fetchGroupOptions() {
        Map<String, Object> p = new HashMap<>();
        p.put("gubun", "E0");
        p.put("cmpycd", cmpycd);
        p.put("gbncd", "010");

        apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    groupOptions.clear();
                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> m : response.body()) {
                        CodeDto dto = new CodeDto();
                        dto.codecd = getStringVal(m, "code");
                        dto.codenm = getStringVal(m, "cdnm");
                        groupOptions.add(dto);
                        names.add(dto.codenm);
                    }
                    ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(MHSBA900U.this, android.R.layout.simple_spinner_item, names);
                    spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spCodeGbn.setAdapter(spinnerAdapter);
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchCodes() {
        if (spCodeGbn.getSelectedItemPosition() < 0) return;
        String cdgbn = groupOptions.get(spCodeGbn.getSelectedItemPosition()).codecd;

        Map<String, Object> p = new HashMap<>();
        p.put("actkind", "S0");
        p.put("cmpycd", cmpycd);
        p.put("cdgbn", cdgbn);

        apiService.executeHsbaProcedure("HSBA_900U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    codeList.clear();
                    codeList.addAll(response.body());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void loadDetail(Map<String, Object> item) {
        currentActKind = "U0";
        etCode.setText(getStringVal(item, "code"));
        etCode.setEnabled(false);
        etCodeName.setText(getStringVal(item, "cdnm"));
        etDspOrd.setText(getStringVal(item, "dspord"));
        etRemark.setText(getStringVal(item, "remark"));
        swUseYn.setChecked("Y".equals(getStringVal(item, "useyn")));
    }

    private void save() {
        String code = etCode.getText().toString().trim();
        String name = etCodeName.getText().toString().trim();
        if (code.isEmpty() || name.isEmpty()) {
            Toast.makeText(this, "코드와 코드명은 필수입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> p = new HashMap<>();
        p.put("actkind", currentActKind);
        p.put("cmpycd", cmpycd);
        p.put("cdgbn", groupOptions.get(spCodeGbn.getSelectedItemPosition()).codecd);
        p.put("code", code);
        p.put("cdnm", name);
        p.put("remark", etRemark.getText().toString().trim());
        p.put("dspord", etDspOrd.getText().toString().trim());
        p.put("useyn", swUseYn.isChecked() ? "Y" : "N");
        p.put("userid", userid);

        apiService.executeHsbaProcedure("HSBA_900U_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSBA900U.this, "정상적으로 처리되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchCodes();
                    if (currentActKind.equals("A0")) initializeForm();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    @Override protected String getProgramTitle() { return "코드정보 등록"; }
    @Override protected String getProgramId() { return "MHSBA900U"; }

    private class CodeAdapter extends BaseAdapter {
        @Override public int getCount() { return codeList.size(); }
        @Override public Object getItem(int p) { return codeList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSBA900U.this).inflate(R.layout.item_code_info, pr, false);
            Map<String, Object> item = codeList.get(p);

            ((TextView) v.findViewById(R.id.tvCode)).setText(getStringVal(item, "code"));
            ((TextView) v.findViewById(R.id.tvCodeNm)).setText(getStringVal(item, "cdnm"));
            ((TextView) v.findViewById(R.id.tvUseYn)).setText("Y".equals(getStringVal(item, "useyn")) ? "O" : "X");

            return v;
        }
    }
}

package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.RetrofitClient;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSAA500U] 자동 주간 영업활동 보고서 (즉시 로딩 최적화 버전)
 */
@SuppressLint("SetTextI18n")
@SuppressWarnings("unused")
public class MHSAA500U extends BaseActivity {

    private EditText etWeekId, etSdate, etEdate, etLastWeekPerf, etNextWeekPlan, etIssuesComments;
    private ApiService apiService;
    private String userid;

    @Override
    protected String getProgramTitle() {
        return "자동 주간 영업보고";
    }

    @Override
    protected String getProgramId() {
        return "HSAA500U";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa500u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userid = prefs.getString("userId", "").trim();
        if (userid.isEmpty()) userid = prefs.getString("userid", "").trim();
        if (userid.isEmpty()) userid = "COIT";

        String usernm = prefs.getString("userName", "").trim();
        if (usernm.isEmpty()) usernm = "영업사원";

        apiService = RetrofitClient.getApiService();

        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderTitle.setText("주간 영업보고 (MHSAA500U)");

        Spinner spSalesman = findViewById(R.id.spSalesman);
        etWeekId = findViewById(R.id.etWeekId);
        etSdate = findViewById(R.id.etSdate);
        etEdate = findViewById(R.id.etEdate);
        etLastWeekPerf = findViewById(R.id.etLastWeekPerf);
        etNextWeekPlan = findViewById(R.id.etNextWeekPlan);
        etIssuesComments = findViewById(R.id.etIssuesComments);

        // Setup salesman spinner with logged-in user instantly
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                Collections.singletonList(usernm + " (" + userid + ")"));
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSalesman.setAdapter(spinnerAdapter);

        AppCompatButton btnSearch = findViewById(R.id.btnSearch);
        AppCompatButton btnAuto = findViewById(R.id.btnAuto);
        AppCompatButton btnSave = findViewById(R.id.btnSave);

        btnSearch.setOnClickListener(v -> fetchWeeklyReport());
        btnAuto.setOnClickListener(v -> autoGenerateReport());
        btnSave.setOnClickListener(v -> saveWeeklyReport());

        fetchWeeklyReport();
    }

    private void fetchWeeklyReport() {
        String weekid = etWeekId.getText().toString().trim();
        if (weekid.isEmpty()) {
            Toast.makeText(this, "주차 식별자를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getWeeklyReport(weekid, userid).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Map<String, Object> data = response.body().getData();
                    etLastWeekPerf.setText(getStringVal(data, "last_week_perf"));
                    etNextWeekPlan.setText(getStringVal(data, "next_week_plan"));
                    etIssuesComments.setText(getStringVal(data, "issues_comments"));
                    Toast.makeText(MHSAA500U.this, "주간보고서를 조회했습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MHSAA500U.this, "조회된 주간보고서가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSAA500U.this, "조회 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void autoGenerateReport() {
        String weekid = etWeekId.getText().toString().trim();
        String sdate = etSdate.getText().toString().trim();
        String edate = etEdate.getText().toString().trim();

        if (weekid.isEmpty()) {
            Toast.makeText(this, "주차 식별자를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("weekid", weekid);
        payload.put("userid", userid);
        payload.put("sdate", sdate);
        payload.put("edate", edate);
        payload.put("nextSdate", "20261026");
        payload.put("nextEdate", "20261101");

        apiService.autoGenerateWeeklyReport(payload).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Map<String, Object> data = response.body().getData();
                    etLastWeekPerf.setText(getStringVal(data, "last_week_perf"));
                    etNextWeekPlan.setText(getStringVal(data, "next_week_plan"));
                    etIssuesComments.setText(getStringVal(data, "issues_comments"));
                    Toast.makeText(MHSAA500U.this, "주간 영업보고서 초안이 자동 생성되었습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MHSAA500U.this, "자동 생성 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSAA500U.this, "자동 생성 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveWeeklyReport() {
        String weekid = etWeekId.getText().toString().trim();
        if (weekid.isEmpty()) {
            Toast.makeText(this, "주차 식별자를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("cmpycd", "coit");
        payload.put("userid", userid);
        payload.put("week_id", weekid);
        payload.put("weekid", weekid);
        payload.put("sdate", etSdate.getText().toString().trim());
        payload.put("edate", etEdate.getText().toString().trim());
        payload.put("last_week_perf", etLastWeekPerf.getText().toString());
        payload.put("next_week_plan", etNextWeekPlan.getText().toString());
        payload.put("issues_comments", etIssuesComments.getText().toString());
        payload.put("status", "1");

        apiService.saveWeeklyReport(payload).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSAA500U.this, "주간 영업보고서가 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchWeeklyReport();
                } else {
                    Toast.makeText(MHSAA500U.this, "저장 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                Toast.makeText(MHSAA500U.this, "저장 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }
}
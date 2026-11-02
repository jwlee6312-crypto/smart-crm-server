package com.crmbank.erp.mobile.hmet;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.PopupAdapter;
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
 * 🚀 [MHMET100U] 회의록 조회 프로그램 (모바일 - 회의리스트 팝업 선택 조회, 저장 기능 제거, 조회 전용)
 */
public class MHMET100U extends BaseActivity {

    private TextView tvMeetingDt, tvAudioFile;
    private EditText etMeetingTime, etLocation, etHostNm, etMeetingTit, etSttText, etSummary, etContent;
    private ApiService apiService;
    private String cmpycd;

    @Override
    protected String getProgramTitle() {
        return "회의록 조회";
    }

    @Override
    protected String getProgramId() {
        return "HMET100U";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hmet100u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "COIT").trim();

        apiService = RetrofitClient.getApiService();

        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderTitle.setText("회의록 조회 (HMET100U)");

        tvMeetingDt = findViewById(R.id.tvMeetingDt);
        etMeetingTime = findViewById(R.id.etMeetingTime);
        etLocation = findViewById(R.id.etLocation);
        etHostNm = findViewById(R.id.etHostNm);
        etMeetingTit = findViewById(R.id.etMeetingTit);
        tvAudioFile = findViewById(R.id.tvAudioFile);
        etSttText = findViewById(R.id.etSttText);
        etSummary = findViewById(R.id.etSummary);
        etContent = findViewById(R.id.etContent);

        AppCompatButton btnSearch = findViewById(R.id.btnSearch);
        btnSearch.setOnClickListener(v -> openMeetingSearchPopup());
    }

    private void openMeetingSearchPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText("회의록 검색");

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        if (etSearch != null) etSearch.setHint("회의 제목 입력");

        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();

        // PopupAdapter expects keys like 'custnm' / 'custcd' or generic display. We can map meeting keys.
        PopupAdapter popupAdapter = new PopupAdapter(list, "CUST", item -> {
            // Populate view fields
            tvMeetingDt.setText(getStringVal(item, "meetingdt"));
            etMeetingTime.setText(getStringVal(item, "meetingtime"));
            etLocation.setText(getStringVal(item, "location"));
            etHostNm.setText(getStringVal(item, "hostnm"));
            etMeetingTit.setText(getStringVal(item, "meetingtit"));
            tvAudioFile.setText(getStringVal(item, "audio_file_path").isEmpty() ? "첨부된 녹취 파일 없음" : getStringVal(item, "audio_file_path"));
            etSttText.setText(getStringVal(item, "stt_text"));
            etSummary.setText(getStringVal(item, "summary"));
            etContent.setText(getStringVal(item, "content"));
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", cmpycd);
            String keyword = etSearch != null ? etSearch.getText().toString().trim() : "";
            if (!keyword.isEmpty()) {
                params.put("title", keyword);
            }

            apiService.getHmetMaster(params).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        list.clear();
                        for (Map<String, Object> m : response.body()) {
                            Map<String, Object> mapped = new HashMap<>(m);
                            String titleStr = getStringVal(m, "meetingtit");
                            String dtStr = getStringVal(m, "meetingdt");
                            String hostStr = getStringVal(m, "hostnm");
                            mapped.put("custnm", "[회의제목] " + titleStr + " | [일자] " + dtStr + (hostStr.isEmpty() ? "" : " | 주관: " + hostStr));
                            mapped.put("custcd", getStringVal(m, "meetingid"));
                            list.add(mapped);
                        }
                        popupAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(MHMET100U.this, "회의록 목록 조회 실패", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                    Toast.makeText(MHMET100U.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        // Auto trigger initial load
        dialogView.findViewById(R.id.btnSearch).performClick();
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
}

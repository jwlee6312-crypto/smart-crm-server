package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
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
 * 🚀 [MHSAA100U] 영업활동 관리 (목록)
 */
@SuppressLint({"SetTextI18n", "NotifyDataSetChanged", "DefaultLocale", "SimpleDateFormat"})
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class MHSAA100U extends BaseActivity {

    private TextView tvStart, tvEnd;
    private EditText etSearch;
    private Spinner spUser;
    private ListView lvMaster;
    private MasterAdapter adapter;
    private final List<Map<String, Object>> masterList = new ArrayList<>();
    private final List<Map<String, Object>> userList = new ArrayList<>();
    private ApiService apiService;
    private String cmpycd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa100u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        apiService = RetrofitClient.getApiService();

        tvStart = findViewById(R.id.tvStartDate);
        tvEnd = findViewById(R.id.tvEndDate);
        etSearch = findViewById(R.id.etSchCustNm);
        spUser = findViewById(R.id.spUser);
        lvMaster = findViewById(R.id.lvMasterList);

        adapter = new MasterAdapter();
        lvMaster.setAdapter(adapter);

        // 초기 날짜 설정 (당월 1일 ~ 오늘)
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvEnd.setText(sdf.format(cal.getTime()));
        cal.set(Calendar.DAY_OF_MONTH, 1);
        tvStart.setText(sdf.format(cal.getTime()));

        tvStart.setOnClickListener(v -> showDatePicker(tvStart));
        tvEnd.setOnClickListener(v -> showDatePicker(tvEnd));
        findViewById(R.id.btnSearch).setOnClickListener(v -> search());
        findViewById(R.id.btnNew).setOnClickListener(v -> {
            Intent intent = new Intent(this, MHSAA110U.class);
            startActivity(intent);
        });

        lvMaster.setOnItemClickListener((parent, view, position, id) -> {
            Map<String, Object> item = masterList.get(position);
            Intent intent = new Intent(this, MHSAA110U.class);
            intent.putExtra("salesid", getStringVal(item, "salesid"));
            startActivity(intent);
        });

        loadUsers();
        search();
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        try {
            Date d = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(tv.getText().toString());
            if (d != null) cal.setTime(d);
        } catch (Exception ignored) {}

        new DatePickerDialog(this, (view, y, m, d) -> {
            tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadUsers() {
        apiService.getHsaaUsers().enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    userList.clear();
                    Map<String, Object> all = new HashMap<>();
                    all.put("userid", ""); all.put("usernm", "전체");
                    userList.add(all);
                    userList.addAll(response.body().getData());

                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> u : userList) names.add(getStringVal(u, "usernm"));
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MHSAA100U.this, android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spUser.setAdapter(adapter);
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    private void search() {
        Map<String, Object> params = new HashMap<>();
        params.put("fromdt", tvStart.getText().toString().replace("-", ""));
        params.put("todt", tvEnd.getText().toString().replace("-", ""));
        params.put("schcustnm", etSearch.getText().toString().trim());
        
        int userIdx = spUser != null ? spUser.getSelectedItemPosition() : -1;
        if (userIdx > 0 && userIdx < userList.size()) {
            params.put("userid", getStringVal(userList.get(userIdx), "userid"));
        } else {
            params.put("userid", "");
        }

        apiService.getHsaaMaster(params).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    masterList.clear();
                    masterList.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSAA100U.this, "조회 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    @Override protected String getProgramTitle() { return "영업활동 관리"; }
    @Override protected String getProgramId() { return "MHSAA100U"; }

    private class MasterAdapter extends BaseAdapter {
        private final DecimalFormat df = new DecimalFormat("#,###");

        @Override public int getCount() { return masterList.size(); }
        @Override public Object getItem(int p) { return masterList.get(p); }
        @Override public long getItemId(int p) { return p; }
        @Override public View getView(int p, View v, ViewGroup pr) {
            if (v == null) v = LayoutInflater.from(MHSAA100U.this).inflate(R.layout.item_mhsaa100u, pr, false);
            Map<String, Object> item = masterList.get(p);

            ((TextView) v.findViewById(R.id.tvCustNm)).setText(getStringVal(item, "custnm"));
            ((TextView) v.findViewById(R.id.tvSalesTitle)).setText(getStringVal(item, "salestitle"));
            ((TextView) v.findViewById(R.id.tvUserNm)).setText(getStringVal(item, "usernm"));
            
            String addTime = getStringVal(item, "addtime");
            if (addTime.length() >= 8) {
                ((TextView) v.findViewById(R.id.tvAddTime)).setText(String.format("%s-%s-%s", addTime.substring(0,4), addTime.substring(4,6), addTime.substring(6,8)));
            } else {
                ((TextView) v.findViewById(R.id.tvAddTime)).setText(addTime);
            }

            double amt = 0;
            try { amt = Double.parseDouble(getStringVal(item, "foreamt")); } catch (Exception ignored) {}
            ((TextView) v.findViewById(R.id.tvForeAmt)).setText(df.format(amt));

            TextView tvState = v.findViewById(R.id.tvState);
            tvState.setText(getStringVal(item, "statenm"));
            // 상태별 색상 처리 (예: 성공 900이면 녹색 등)
            String state = getStringVal(item, "state");
            if ("900".equals(state)) tvState.setBackgroundResource(R.drawable.bg_button_gradient_green);
            else if ("910".equals(state) || "930".equals(state)) tvState.setBackgroundResource(R.drawable.bg_button_gradient_orange);
            else tvState.setBackgroundResource(R.drawable.bg_status_badge);

            return v;
        }
    }
}

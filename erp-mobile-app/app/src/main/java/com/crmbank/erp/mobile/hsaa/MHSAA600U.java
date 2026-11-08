package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;

import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSAA600U] 영업목표 관리 (개인 영업목표 조회 및 수정)
 */
@SuppressLint({"SetTextI18n", "NotifyDataSetChanged", "DefaultLocale", "SimpleDateFormat"})
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class MHSAA600U extends BaseActivity {

    private EditText etYear;
    private ListView lvTargets;
    private TargetAdapter adapter;
    private final List<Map<String, Object>> targetList = new ArrayList<>();
    private ApiService apiService;
    private String userid;

    @Override
    protected String getProgramTitle() {
        return "영업목표 관리";
    }

    @Override
    protected String getProgramId() {
        return "HSAA600U";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa600u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userid = prefs.getString("userId", "").trim();

        apiService = RetrofitClient.getApiService();

        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderTitle.setText("영업목표 관리 (MHSAA600U)");

        etYear = findViewById(R.id.etYear);
        lvTargets = findViewById(R.id.lvTargets);
        AppCompatButton btnSearch = findViewById(R.id.btnSearch);
        AppCompatButton btnSave = findViewById(R.id.btnSave);

        String currentYear = new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date());
        etYear.setText(currentYear);

        adapter = new TargetAdapter(this, targetList);
        lvTargets.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> loadTargets());
        btnSave.setOnClickListener(v -> saveTargets());

        loadTargets();
    }

    private void loadTargets() {
        String yyyy = etYear.getText().toString().trim();
        if (yyyy.length() != 4) {
            Toast.makeText(this, "조회할 연도 4자리를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getSalesTargetList(yyyy, userid).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    targetList.clear();
                    targetList.addAll(response.body().getData());
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MHSAA600U.this, "영업목표 조회 완료", Toast.LENGTH_SHORT).show();
                } else {
                    initDefaultMonths(yyyy);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSAA600U.this, "조회 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                initDefaultMonths(yyyy);
            }
        });
    }

    private void initDefaultMonths(String yyyy) {
        targetList.clear();
        for (int i = 1; i <= 12; i++) {
            Map<String, Object> map = new HashMap<>();
            String mm = String.format(Locale.getDefault(), "%02d", i);
            map.put("yymm", yyyy + mm);
            map.put("mm", mm);
            map.put("planamt", 0);
            map.put("contactcnt", 0);
            map.put("consultcnt", 0);
            targetList.add(map);
        }
        adapter.notifyDataSetChanged();
    }

    private void saveTargets() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targets", targetList);

        apiService.saveSalesTargets(payload).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSAA600U.this, "영업목표가 정상적으로 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    loadTargets();
                } else {
                    Toast.makeText(MHSAA600U.this, "저장 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {
                Toast.makeText(MHSAA600U.this, "저장 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class TargetAdapter extends BaseAdapter {
        private final Context context;
        private final List<Map<String, Object>> items;

        public TargetAdapter(Context context, List<Map<String, Object>> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() { return items.size(); }

        @Override
        public Object getItem(int position) { return items.get(position); }

        @Override
        public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.item_mhsaa600u, parent, false);
                holder = new ViewHolder();
                holder.tvMonth = view.findViewById(R.id.tvMonth);
                holder.etPlanAmt = view.findViewById(R.id.etPlanAmt);
                holder.etContactCnt = view.findViewById(R.id.etContactCnt);
                holder.etConsultCnt = view.findViewById(R.id.etConsultCnt);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Map<String, Object> item = items.get(position);
            String mmStr = item.get("mm") != null ? String.valueOf(item.get("mm")) : String.format(Locale.getDefault(), "%02d", (position + 1));
            holder.tvMonth.setText(mmStr);

            Object planAmtObj = item.get("planamt");
            long planAmt = 0L;
            if (planAmtObj instanceof Number) {
                planAmt = ((Number) planAmtObj).longValue();
            } else if (planAmtObj != null) {
                try { planAmt = Long.parseLong(String.valueOf(planAmtObj).replaceAll("[^0-9]", "")); } catch (Exception ignored) {}
            }
            holder.etPlanAmt.setText(planAmt > 0 ? String.format(Locale.getDefault(), "%,d", planAmt) : "");

            Object contactCnt = item.get("contactcnt");
            holder.etContactCnt.setText(contactCnt != null ? String.valueOf(contactCnt) : "0");

            Object consultCnt = item.get("consultcnt");
            holder.etConsultCnt.setText(consultCnt != null ? String.valueOf(consultCnt) : "0");

            holder.etPlanAmt.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    try {
                        String raw = holder.etPlanAmt.getText().toString().replaceAll("[^0-9]", "");
                        long val = raw.isEmpty() ? 0L : Long.parseLong(raw);
                        item.put("planamt", val);
                        holder.etPlanAmt.setText(val > 0 ? String.format(Locale.getDefault(), "%,d", val) : "");
                    } catch (Exception e) {
                        item.put("planamt", 0L);
                    }
                }
            });

            holder.etContactCnt.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    try {
                        item.put("contactcnt", Long.parseLong(holder.etContactCnt.getText().toString().trim()));
                    } catch (Exception e) {
                        item.put("contactcnt", 0L);
                    }
                }
            });

            holder.etConsultCnt.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    try {
                        item.put("consultcnt", Long.parseLong(holder.etConsultCnt.getText().toString().trim()));
                    } catch (Exception e) {
                        item.put("consultcnt", 0L);
                    }
                }
            });

            return view;
        }

        private static class ViewHolder {
            TextView tvMonth;
            EditText etPlanAmt, etContactCnt, etConsultCnt;
        }
    }
}

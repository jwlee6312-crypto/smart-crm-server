package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.content.Context;
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
import androidx.appcompat.widget.AppCompatButton;

import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSAA700U] 영업활동 방문계획 조회 (즉시 로딩 최적화 버전)
 */
@SuppressLint({"SetTextI18n", "SimpleDateFormat"})
public class MHSAA700U extends BaseActivity {

    private EditText etYm;
    private ListView lvVisitPlans;
    private Spinner spSalesman;
    private VisitPlanAdapter adapter;
    private final List<Map<String, Object>> planList = new ArrayList<>();
    private ApiService apiService;
    private String userid, usernm;

    @Override
    protected String getProgramTitle() {
        return "방문계획 조회";
    }

    @Override
    protected String getProgramId() {
        return "HSAA700U";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa700u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userid = prefs.getString("userId", "").trim();
        if (userid.isEmpty()) userid = prefs.getString("userid", "").trim();
        if (userid.isEmpty()) userid = "COIT";

        usernm = prefs.getString("userName", "").trim();
        if (usernm.isEmpty()) usernm = "영업사원";

        apiService = RetrofitClient.getApiService();

        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderTitle.setText("방문계획 조회 (MHSAA700U)");

        etYm = findViewById(R.id.etYm);
        lvVisitPlans = findViewById(R.id.lvVisitPlans);
        spSalesman = findViewById(R.id.spSalesman);
        AppCompatButton btnSearch = findViewById(R.id.btnSearch);

        String currentYm = new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(new Date());
        etYm.setText(currentYm);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                Collections.singletonList(usernm + " (" + userid + ")"));
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSalesman.setAdapter(spinnerAdapter);

        adapter = new VisitPlanAdapter(this, planList);
        lvVisitPlans.setAdapter(adapter);

        btnSearch.setOnClickListener(v -> loadVisitPlans());

        loadVisitPlans();
    }

    private void loadVisitPlans() {
        String ym = etYm.getText().toString().trim();
        if (ym.length() != 6) {
            Toast.makeText(this, "기준년월 6자리(YYYYMM)를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.getVisitPlan(ym, userid).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Map<String, Object>> data = response.body().getData();
                        planList.clear();
                        if (data != null) {
                            planList.addAll(data);
                        }
                        adapter.notifyDataSetChanged();
                        Toast.makeText(MHSAA700U.this, "방문 계획 조회 완료 (" + planList.size() + "건)", Toast.LENGTH_SHORT).show();
                    } else {
                        planList.clear();
                        adapter.notifyDataSetChanged();
                        String errBody = "";
                        try (okhttp3.ResponseBody eb = response.errorBody()) {
                            if (eb != null) {
                                errBody = eb.string();
                            }
                        } catch (Exception ignored) {}
                        String errInfo = "조회 실패 (코드: " + response.code() + ") " + errBody;
                        Toast.makeText(MHSAA700U.this, errInfo, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(MHSAA700U.this, "데이터 처리 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSAA700U.this, "조회 통신 오류: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static class VisitPlanAdapter extends BaseAdapter {
        private final Context context;
        private final List<Map<String, Object>> items;

        public VisitPlanAdapter(Context context, List<Map<String, Object>> items) {
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
                view = LayoutInflater.from(context).inflate(R.layout.item_mhsaa700u, parent, false);
                holder = new ViewHolder();
                holder.tvPlanDt = view.findViewById(R.id.tvPlanDt);
                holder.tvImport = view.findViewById(R.id.tvImport);
                holder.tvCustNm = view.findViewById(R.id.tvCustNm);
                holder.tvDealTitle = view.findViewById(R.id.tvDealTitle);
                holder.tvAddress = view.findViewById(R.id.tvAddress);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Map<String, Object> item = items.get(position);

            Object pDtObj = item.get("plan_dt");
            String pDt = pDtObj != null ? String.valueOf(pDtObj) : "";
            if (pDt.length() == 8) {
                try {
                    String y = pDt.substring(0, 4);
                    String m = pDt.substring(4, 6);
                    String d = pDt.substring(6, 8);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
                    Date date = sdf.parse(pDt);
                    if (date != null) {
                        String dayOfWeek = new SimpleDateFormat("E", Locale.KOREAN).format(date);
                        holder.tvPlanDt.setText(y + "-" + m + "-" + d + " (" + dayOfWeek + ")");
                    } else {
                        holder.tvPlanDt.setText(pDt);
                    }
                } catch (Exception e) {
                    holder.tvPlanDt.setText(pDt);
                }
            } else {
                holder.tvPlanDt.setText(pDt);
            }

            Object importObj = item.get("import_nm");
            holder.tvImport.setText(importObj != null ? String.valueOf(importObj) : "중");
            
            Object custObj = item.get("custnm");
            holder.tvCustNm.setText(custObj != null ? String.valueOf(custObj) : "");
            
            Object titleObj = item.get("salestitle");
            if (titleObj == null) titleObj = item.get("deal_title");
            holder.tvDealTitle.setText(titleObj != null ? String.valueOf(titleObj) : "");
            
            Object addrObj = item.get("address");
            holder.tvAddress.setText(addrObj != null ? String.valueOf(addrObj) : "");

            return view;
        }

        private static class ViewHolder {
            TextView tvPlanDt, tvImport, tvCustNm, tvDealTitle, tvAddress;
        }
    }
}

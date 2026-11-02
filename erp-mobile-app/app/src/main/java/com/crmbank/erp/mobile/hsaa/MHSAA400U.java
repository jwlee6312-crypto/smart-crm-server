package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.RetrofitClient;

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
 * 🚀 [MHSAA400U] 영업활동 코칭등록
 */
@SuppressLint({"SetTextI18n", "SimpleDateFormat"})
@SuppressWarnings({"unchecked", "unused"})
public class MHSAA400U extends BaseActivity {

    private TextView tvSdate, tvEdate, tvDetailTitle, tvDiaryContent, tvReportContent, tvReportDt;
    private EditText etCustNm, etSalesCoaching;
    private Spinner spSalesman;
    private CheckBox cbReportOnly, cbCoachedOnly;
    private RecyclerView rvCoachingList;
    private LinearLayout layoutDetail;

    private ApiService apiService;
    private final List<Map<String, Object>> salesmanList = new ArrayList<>();
    private final List<Map<String, Object>> consultationList = new ArrayList<>();
    private Map<String, Object> currentRecord = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa400u);

        apiService = RetrofitClient.getApiService();

        initViews();
        loadSalesmen();
        initializeFilter();
    }

    private void initViews() {
        tvSdate = findViewById(R.id.tvSdate);
        tvEdate = findViewById(R.id.tvEdate);
        etCustNm = findViewById(R.id.etCustNm);
        spSalesman = findViewById(R.id.spSalesman);
        cbReportOnly = findViewById(R.id.cbReportOnly);
        cbCoachedOnly = findViewById(R.id.cbCoachedOnly);
        rvCoachingList = findViewById(R.id.rvCoachingList);
        layoutDetail = findViewById(R.id.layoutDetail);

        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDiaryContent = findViewById(R.id.tvDiaryContent);
        tvReportContent = findViewById(R.id.tvReportContent);
        etSalesCoaching = findViewById(R.id.etSalesCoaching);
        tvReportDt = findViewById(R.id.tvReportDt);

        rvCoachingList.setLayoutManager(new LinearLayoutManager(this));
        rvCoachingList.setAdapter(new CoachingAdapter());

        findViewById(R.id.btnReset).setOnClickListener(v -> initializeFilter());
        findViewById(R.id.btnSearch).setOnClickListener(v -> fetchList());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveCoaching());

        tvSdate.setOnClickListener(v -> showDatePicker(tvSdate));
        tvEdate.setOnClickListener(v -> showDatePicker(tvEdate));
        tvReportDt.setOnClickListener(v -> showDatePicker(tvReportDt));
    }

    private void initializeFilter() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvEdate.setText(sdf.format(cal.getTime()));
        cal.add(Calendar.MONTH, -1);
        tvSdate.setText(sdf.format(cal.getTime()));
        
        etCustNm.setText("");
        cbReportOnly.setChecked(false);
        cbCoachedOnly.setChecked(false);
        layoutDetail.setVisibility(View.GONE);
        currentRecord = null;
        if (spSalesman.getAdapter() != null) spSalesman.setSelection(0);
    }

    private void loadSalesmen() {
        apiService.getHsaaUsers().enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    salesmanList.clear();
                    Map<String, Object> all = new HashMap<>();
                    all.put("userid", ""); all.put("usernm", "전체담당자");
                    salesmanList.add(all);
                    salesmanList.addAll(response.body().getData());

                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> u : salesmanList) names.add(getStringVal(u, "usernm"));
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MHSAA400U.this, android.R.layout.simple_spinner_item, names);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spSalesman.setAdapter(adapter);
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void fetchList() {
        String sdate = tvSdate.getText().toString().replace("-", "");
        String edate = tvEdate.getText().toString().replace("-", "");
        String schcustnm = etCustNm.getText().toString().trim();
        String salesmanId;
        if (spSalesman.getSelectedItemPosition() > 0) {
            salesmanId = getStringVal(salesmanList.get(spSalesman.getSelectedItemPosition()), "userid");
        } else {
            salesmanId = "";
        }

        Map<String, Object> p = new HashMap<>();
        p.put("sdate", sdate);
        p.put("edate", edate);
        p.put("schcustnm", schcustnm);
        p.put("userid", salesmanId);
        p.put("page", 1);
        p.put("limit", 200);
        
        if (cbReportOnly.isChecked()) p.put("reportyn", "Y");
        if (cbCoachedOnly.isChecked()) p.put("coachingyn", "Y");

        apiService.getHsaaConsultationList(p).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) response.body().getData().get("list");
                    consultationList.clear();
                    if (list != null) consultationList.addAll(list);
                    if (rvCoachingList.getAdapter() != null) rvCoachingList.getAdapter().notifyDataSetChanged();
                    layoutDetail.setVisibility(View.GONE);
                    Toast.makeText(MHSAA400U.this, consultationList.size() + "건 조회됨", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void showDetail(Map<String, Object> item) {
        currentRecord = item;
        layoutDetail.setVisibility(View.VISIBLE);
        
        String title = getStringVal(item, "custnm") + " | " + formatDate(getStringVal(item, "contdt"));
        tvDetailTitle.setText(title);
        tvDiaryContent.setText(getStringVal(item, "diarycontent"));
        tvReportContent.setText(getStringVal(item, "reportcontent"));
        etSalesCoaching.setText(getStringVal(item, "salescoaching"));
        tvReportDt.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
    }

    private void saveCoaching() {
        if (currentRecord == null) return;
        if (etSalesCoaching.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "코칭 내용을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("salesid", getStringVal(currentRecord, "salesid"));
        payload.put("ser", getStringVal(currentRecord, "ser"));
        payload.put("contdt", getStringVal(currentRecord, "contdt").replace("-", ""));
        payload.put("reportdt", tvReportDt.getText().toString().replace("-", ""));
        payload.put("salescoaching", etSalesCoaching.getText().toString());

        apiService.saveHsaaCoaching(payload).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSAA400U.this, "코칭 내용이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchList();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {}
        });
    }

    private void showDatePicker(TextView tv) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d)), cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private String formatDate(String dateStr) {
        String d = dateStr;
        if (d == null || d.isEmpty()) return "";
        if (d.contains(" ")) d = d.split(" ")[0];
        return d.length() == 8 ? String.format("%s-%s-%s", d.substring(0,4), d.substring(4,6), d.substring(6,8)) : d;
    }

    @Override protected String getProgramTitle() { return "영업활동 코칭등록"; }
    @Override protected String getProgramId() { return "MHSAA400U"; }

    // --- Adapter ---
    class CoachingAdapter extends RecyclerView.Adapter<CoachingAdapter.ViewHolder> {
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_mhsaa400u, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> item = consultationList.get(position);
            holder.tvCustNm.setText(getStringVal(item, "custnm"));
            holder.tvUserNm.setText(getStringVal(item, "usernm"));
            holder.tvContDt.setText(formatDate(getStringVal(item, "contdt")));
            
            String content = getStringVal(item, "diarycontent");
            if (content.isEmpty()) content = getStringVal(item, "content");
            holder.tvSummary.setText(content);

            boolean isReported = "Y".equalsIgnoreCase(getStringVal(item, "reportyn"));
            boolean isCoached = "Y".equalsIgnoreCase(getStringVal(item, "coachingreadyn"));
            
            if (isCoached) {
                holder.tvStatus.setText("코칭완료");
                if (holder.tvStatus.getBackground() != null) holder.tvStatus.getBackground().setTint(Color.parseColor("#198754"));
            } else if (isReported) {
                holder.tvStatus.setText("보고요청");
                if (holder.tvStatus.getBackground() != null) holder.tvStatus.getBackground().setTint(Color.parseColor("#0d6efd"));
            } else {
                holder.tvStatus.setText("일반상담");
                if (holder.tvStatus.getBackground() != null) holder.tvStatus.getBackground().setTint(Color.parseColor("#6c757d"));
            }

            holder.itemView.setOnClickListener(v -> showDetail(item));
        }

        @Override public int getItemCount() { return consultationList.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvCustNm, tvUserNm, tvContDt, tvSummary, tvStatus;
            public ViewHolder(@NonNull View v) {
                super(v);
                tvCustNm = v.findViewById(R.id.tvCustNm);
                tvUserNm = v.findViewById(R.id.tvUserNm);
                tvContDt = v.findViewById(R.id.tvContDt);
                tvSummary = v.findViewById(R.id.tvContentSummary);
                tvStatus = v.findViewById(R.id.tvStatus);
            }
        }
    }
}

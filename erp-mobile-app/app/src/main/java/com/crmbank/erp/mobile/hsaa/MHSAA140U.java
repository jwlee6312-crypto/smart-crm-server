package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
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
 * 🚀 [MHSAA140U] 단계변동 등록
 */
@SuppressLint({"SetTextI18n", "SimpleDateFormat"})
@SuppressWarnings({"unchecked", "unused"})
public class MHSAA140U extends BaseActivity {

    private EditText etSalesTitle, etRemark;
    private TextView tvChngDt;
    private Spinner spBfState, spState;
    private LinearLayout llStageList;

    private ApiService apiService;
    private String cmpycd, userid;
    private String salesid = "";
    private String bfStateCode = "";

    private final List<Map<String, Object>> stageHistory = new ArrayList<>();
    private final List<Map<String, Object>> stateCodeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa140u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        initViews();
        loadInitialData();

        salesid = getIntent().getStringExtra("salesid");
        if (salesid != null && !salesid.isEmpty()) {
            fetchSalesDetail();
        } else {
            initializeForm();
        }
    }

    private void initViews() {
        etSalesTitle = findViewById(R.id.etSalesTitle);
        spBfState = findViewById(R.id.spBfState);
        etRemark = findViewById(R.id.etRemark);
        tvChngDt = findViewById(R.id.tvChngDt);
        spState = findViewById(R.id.spState);
        llStageList = findViewById(R.id.llStageList);

        findViewById(R.id.btnReset).setOnClickListener(v -> initializeForm());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnDelete).setVisibility(View.GONE);
        findViewById(R.id.btnSalesSearch).setOnClickListener(v -> openHelp());
        
        tvChngDt.setOnClickListener(v -> showDatePicker(tvChngDt));
    }

    private void initializeForm() {
        etRemark.setText("");
        tvChngDt.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        spBfState.setSelection(0);
        spState.setSelection(0);
    }

    private void loadInitialData() {
        apiService.getHsaaCodes("700").enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Response<ApiResponse<List<Map<String, Object>>>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    stateCodeList.clear(); stateCodeList.addAll(r.body().getData());
                    setupSpinner(spBfState, stateCodeList);
                    setupSpinner(spState, stateCodeList);
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Throwable t) {}
        });
    }

    private void setupSpinner(Spinner sp, List<Map<String, Object>> data) {
        List<String> items = new ArrayList<>();
        for (Map<String, Object> d : data) items.add(getStringVal(d, "codenm"));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
    }

    private void fetchSalesDetail() {
        apiService.getHsaaDetail(salesid).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> mst = (Map<String, Object>) response.body().getData().get("master");
                    if (mst != null) {
                        etSalesTitle.setText(getStringVal(mst, "salestitle"));
                        bfStateCode = getStringVal(mst, "state");
                        setSpinnerSelection(spBfState, stateCodeList, bfStateCode);
                        fetchStageHistory();
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchStageHistory() {
        apiService.getHsaaStages(salesid).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    stageHistory.clear(); stageHistory.addAll(response.body().getData());
                    refreshHistoryList();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    private void refreshHistoryList() {
        llStageList.removeAllViews();
        for (Map<String, Object> item : stageHistory) {
            View v = getLayoutInflater().inflate(R.layout.item_mhsaa140u_row, llStageList, false);
            ((TextView) v.findViewById(R.id.tvRowDate)).setText(formatDate(getStringVal(item, "chngdt")));
            ((TextView) v.findViewById(R.id.tvRowState)).setText(getStringVal(item, "statenm"));
            ((TextView) v.findViewById(R.id.tvRowRemark)).setText(getStringVal(item, "remark"));
            
            v.setOnClickListener(v1 -> loadStageDetail(item));
            llStageList.addView(v);
        }
    }

    private void loadStageDetail(Map<String, Object> item) {
        tvChngDt.setText(formatDate(getStringVal(item, "chngdt")));
        etRemark.setText(getStringVal(item, "remark"));
        setSpinnerSelection(spBfState, stateCodeList, getStringVal(item, "bfstate"));
        setSpinnerSelection(spState, stateCodeList, getStringVal(item, "state"));
    }

    private void setSpinnerSelection(Spinner sp, List<Map<String, Object>> list, String val) {
        for (int i = 0; i < list.size(); i++) {
            if (getStringVal(list.get(i), "codecd").equals(val)) {
                sp.setSelection(i);
                break;
            }
        }
    }

    private void save() {
        if (salesid.isEmpty()) { Toast.makeText(this, "영업건을 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (etRemark.getText().toString().isEmpty()) { Toast.makeText(this, "변동사유를 입력하세요.", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> payload = new HashMap<>();
        payload.put("cmpycd", cmpycd);
        payload.put("salesid", salesid);
        payload.put("chngdt", tvChngDt.getText().toString().replace("-", ""));
        
        if (spBfState.getSelectedItemPosition() >= 0) {
            payload.put("bfstate", getStringVal(stateCodeList.get(spBfState.getSelectedItemPosition()), "codecd"));
        } else {
            payload.put("bfstate", bfStateCode);
        }

        payload.put("remark", etRemark.getText().toString());
        payload.put("updemp", userid);
        
        if (spState.getSelectedItemPosition() >= 0) {
            payload.put("state", getStringVal(stateCodeList.get(spState.getSelectedItemPosition()), "codecd"));
        }

        apiService.saveHsaaStage(payload).enqueue(new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSAA140U.this, "변동사항이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchSalesDetail(); initializeForm();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {}
        });
    }

    private void openHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_hsaa_search, null);
        builder.setView(dialogView);
        
        TextView tvStart = dialogView.findViewById(R.id.tvPopStartDate);
        TextView tvEnd = dialogView.findViewById(R.id.tvPopEndDate);
        EditText etSch = dialogView.findViewById(R.id.etPopSearch);
        ListView lv = dialogView.findViewById(R.id.lvPopList);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvEnd.setText(sdf.format(cal.getTime()));
        cal.add(Calendar.MONTH, -1);
        tvStart.setText(sdf.format(cal.getTime()));

        tvStart.setOnClickListener(v -> showDatePicker(tvStart));
        tvEnd.setOnClickListener(v -> showDatePicker(tvEnd));

        List<Map<String, Object>> list = new ArrayList<>();
        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return list.size(); }
            @Override public Object getItem(int p) { return list.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public View getView(int p, View v, ViewGroup pr) {
                View view = v;
                if (view == null) view = getLayoutInflater().inflate(R.layout.item_hsaa_pop, pr, false);
                Map<String, Object> item = list.get(p);
                ((TextView) view.findViewById(R.id.tvPopCustNm)).setText(getStringVal(item, "custnm"));
                ((TextView) view.findViewById(R.id.tvPopSalesTitle)).setText(getStringVal(item, "salestitle"));
                ((TextView) view.findViewById(R.id.tvPopAddTime)).setText(formatDate(getStringVal(item, "addtime")));
                return view;
            }
        };
        lv.setAdapter(adapter);

        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.btnPopSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("fromdt", tvStart.getText().toString().replace("-", ""));
            p.put("todt", tvEnd.getText().toString().replace("-", ""));
            p.put("schcustnm", etSch.getText().toString().trim());
            apiService.getHsaaMaster(p).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Response<ApiResponse<List<Map<String, Object>>>> r) {
                    if (r.isSuccessful() && r.body() != null) {
                        list.clear(); list.addAll(r.body().getData()); adapter.notifyDataSetChanged();
                    }
                }
                @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Throwable t) {}
            });
        });
        
        lv.setOnItemClickListener((p1, v1, pos, id) -> {
            salesid = getStringVal(list.get(pos), "salesid");
            fetchSalesDetail();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnPopClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        dialogView.findViewById(R.id.btnPopSearch).performClick();
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

    @Override protected String getProgramTitle() { return "단계변동 등록"; }
    @Override protected String getProgramId() { return "MHSAA140U"; }
}

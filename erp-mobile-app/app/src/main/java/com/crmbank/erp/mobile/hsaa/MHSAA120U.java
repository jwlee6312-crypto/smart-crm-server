package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
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
import com.crmbank.erp.mobile.PopupAdapter;
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
 * 🚀 [MHSAA120U] Key맨 등록
 */
@SuppressLint({"SetTextI18n", "NotifyDataSetChanged", "DefaultLocale"})
@SuppressWarnings({"unchecked", "unused", "FieldCanBeLocal"})
public class MHSAA120U extends BaseActivity {

    private EditText etSalesTitle, etName, etDept, etJikch, etHpNo, etTelNo, etMail, etRemark;
    private TextView tvBirthday;
    private Spinner spBirGb, spFavor, spLevel;
    private LinearLayout llKeymanList;

    private ApiService apiService;
    private String cmpycd, userid;
    private String salesid = "";
    private String custcd = "";
    private String currentKeymanId = "";

    private final List<Map<String, Object>> keymanList = new ArrayList<>();
    private final List<Map<String, Object>> favorCodeList = new ArrayList<>();
    private final List<Map<String, Object>> levelCodeList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa120u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");

        apiService = RetrofitClient.getApiService();

        initViews();
        loadInitialData();

        // 110U 등에서 넘어올 때
        salesid = getIntent().getStringExtra("salesid");
        if (salesid != null && !salesid.isEmpty()) {
            fetchSalesDetail();
        } else {
            initializeForm();
        }
    }

    private void initViews() {
        etSalesTitle = findViewById(R.id.etSalesTitle);
        etName = findViewById(R.id.etName);
        etDept = findViewById(R.id.etDept);
        etJikch = findViewById(R.id.etJikch);
        etHpNo = findViewById(R.id.etHpNo);
        etTelNo = findViewById(R.id.etTelNo);
        etMail = findViewById(R.id.etMail);
        etRemark = findViewById(R.id.etRemark);
        tvBirthday = findViewById(R.id.tvBirthday);

        spBirGb = findViewById(R.id.spBirGb);
        spFavor = findViewById(R.id.spFavor);
        spLevel = findViewById(R.id.spLevel);

        llKeymanList = findViewById(R.id.llKeymanList);

        findViewById(R.id.btnReset).setOnClickListener(v -> initializeForm());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnDelete).setOnClickListener(v -> delete());
        findViewById(R.id.btnSalesSearch).setOnClickListener(v -> openHelp("SALES"));
        
        etHpNo.setOnClickListener(v -> makeCall(etHpNo.getText().toString()));
        etTelNo.setOnClickListener(v -> makeCall(etTelNo.getText().toString()));
        etMail.setOnClickListener(v -> sendEmail(etMail.getText().toString()));

        tvBirthday.setOnClickListener(v -> showDatePicker(tvBirthday));

        // 생일 구분 초기화
        ArrayAdapter<String> birAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"양력", "음력"});
        birAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spBirGb.setAdapter(birAdapter);
        
        // 등급 초기화
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"상", "중", "하"});
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLevel.setAdapter(levelAdapter);
    }

    private void initializeForm() {
        currentKeymanId = "";
        etName.setText("");
        etDept.setText("");
        etJikch.setText("");
        etHpNo.setText("");
        etTelNo.setText("");
        etMail.setText("");
        etRemark.setText("");
        tvBirthday.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        
        spBirGb.setSelection(0);
        spFavor.setSelection(0);
        spLevel.setSelection(0);
    }

    private void loadInitialData() {
        // 호감도 코드 (720)
        apiService.getHsaaCodes("720").enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Response<ApiResponse<List<Map<String, Object>>>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    favorCodeList.clear(); favorCodeList.addAll(r.body().getData());
                    setupSpinner(spFavor, favorCodeList, "codenm");
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Throwable t) {}
        });
    }

    private void setupSpinner(Spinner sp, List<Map<String, Object>> data, String key) {
        List<String> items = new ArrayList<>();
        for (Map<String, Object> d : data) items.add(getStringVal(d, key));
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
                        custcd = getStringVal(mst, "custcd");
                        fetchKeymanList();
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchKeymanList() {
        apiService.getHsaaKeyman(custcd).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    keymanList.clear();
                    keymanList.addAll(response.body().getData());
                    refreshKeymanList();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    private void refreshKeymanList() {
        llKeymanList.removeAllViews();
        for (Map<String, Object> item : keymanList) {
            View v = getLayoutInflater().inflate(R.layout.item_mhsaa120u_row, llKeymanList, false); // row item layout needs to be created
            ((TextView) v.findViewById(R.id.tvRowName)).setText(getStringVal(item, "name"));
            ((TextView) v.findViewById(R.id.tvRowDept)).setText(getStringVal(item, "custdept"));
            ((TextView) v.findViewById(R.id.tvRowHp)).setText(getStringVal(item, "hpno"));
            
            v.setOnClickListener(v1 -> loadKeymanDetail(item));
            llKeymanList.addView(v);
        }
    }

    private void loadKeymanDetail(Map<String, Object> item) {
        currentKeymanId = getStringVal(item, "custid");
        etName.setText(getStringVal(item, "name"));
        etDept.setText(getStringVal(item, "custdept"));
        etJikch.setText(getStringVal(item, "jikch"));
        etHpNo.setText(getStringVal(item, "hpno"));
        etTelNo.setText(getStringVal(item, "custtel"));
        etMail.setText(getStringVal(item, "mail"));
        etRemark.setText(getStringVal(item, "remark"));
        tvBirthday.setText(formatDate(getStringVal(item, "birthday")));
        
        // Spinner selections...
        spBirGb.setSelection("2".equals(getStringVal(item, "birgb")) ? 1 : 0);
        setSpinnerSelection(spFavor, favorCodeList, "codecd", getStringVal(item, "favor"));
        // Level logic...
    }

    private void save() {
        if (custcd.isEmpty()) { Toast.makeText(this, "거래처를 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (etName.getText().toString().isEmpty()) { Toast.makeText(this, "성명을 입력하세요.", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> payload = new HashMap<>();
        payload.put("cmpycd", cmpycd);
        payload.put("custcd", custcd);
        payload.put("custid", currentKeymanId);
        payload.put("name", etName.getText().toString());
        payload.put("custdept", etDept.getText().toString());
        payload.put("jikch", etJikch.getText().toString());
        payload.put("hpno", etHpNo.getText().toString());
        payload.put("custtel", etTelNo.getText().toString());
        payload.put("mail", etMail.getText().toString());
        payload.put("remark", etRemark.getText().toString());
        payload.put("birthday", tvBirthday.getText().toString().replace("-", ""));
        payload.put("birgb", spBirGb.getSelectedItemPosition() == 1 ? "2" : "1");
        
        if (spFavor.getSelectedItemPosition() >= 0) payload.put("favor", getStringVal(favorCodeList.get(spFavor.getSelectedItemPosition()), "codecd"));
        
        apiService.saveHsaaKeyman(payload).enqueue(new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSAA120U.this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchKeymanList();
                    initializeForm();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {}
        });
    }

    private void delete() {
        if (currentKeymanId.isEmpty()) return;
        new AlertDialog.Builder(this).setTitle("삭제 확인").setMessage("Key맨 정보를 삭제하시겠습니까?")
            .setPositiveButton("예", (d, w) -> {
                apiService.deleteHsaaKeyman(currentKeymanId).enqueue(new Callback<ApiResponse<Object>>() {
                    @Override public void onResponse(@NonNull Call<ApiResponse<Object>> c, @NonNull Response<ApiResponse<Object>> r) {
                        Toast.makeText(MHSAA120U.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        fetchKeymanList(); initializeForm();
                    }
                    @Override public void onFailure(@NonNull Call<ApiResponse<Object>> c, @NonNull Throwable t) {}
                });
            }).setNegativeButton("아니오", null).show();
    }

    private void openHelp(String type) {
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
                if (v == null) v = getLayoutInflater().inflate(R.layout.item_hsaa_pop, pr, false);
                Map<String, Object> item = list.get(p);
                ((TextView) v.findViewById(R.id.tvPopCustNm)).setText(getStringVal(item, "custnm"));
                ((TextView) v.findViewById(R.id.tvPopSalesTitle)).setText(getStringVal(item, "salestitle"));
                ((TextView) v.findViewById(R.id.tvPopAddTime)).setText(formatDate(getStringVal(item, "addtime")));
                return v;
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
        new DatePickerDialog(this, (view, y, m, d) -> {
            tv.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private void setSpinnerSelection(Spinner sp, List<Map<String, Object>> list, String key, String val) {
        for (int i = 0; i < list.size(); i++) {
            if (getStringVal(list.get(i), key).equals(val)) {
                sp.setSelection(i);
                break;
            }
        }
    }

    private String formatDate(String d) {
        if (d == null || d.isEmpty()) return "";
        if (d.contains(" ")) d = d.split(" ")[0];
        return d.length() == 8 ? String.format("%s-%s-%s", d.substring(0,4), d.substring(4,6), d.substring(6,8)) : d;
    }

    @Override protected String getProgramTitle() { return "Key맨 등록"; }
    @Override protected String getProgramId() { return "MHSAA120U"; }
}

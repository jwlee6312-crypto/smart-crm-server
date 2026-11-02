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
import android.widget.CheckBox;
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
 * 🚀 [MHSAA130U] 상담일지 등록
 */
@SuppressLint({"SetTextI18n", "SimpleDateFormat"})
@SuppressWarnings({"unchecked", "unused"})
public class MHSAA130U extends BaseActivity {

    private EditText etSalesTitle, etContHH, etContMM, etDiaryContent, etReportContent, etActionItem, etNextMeetAgenda;
    private TextView tvContDt, tvReportContentLabel, tvSalesCoaching, tvCoachingDt, tvNextMeetDt;
    private Spinner spKeyman, spChannel;
    private CheckBox cbReportYn, cbActionStatus;
    private View btnConfirmCoaching;
    private LinearLayout llDiaryList, layoutCoachingResult;

    private ApiService apiService;
    private String cmpycd;
    private String salesid = "";
    private String custcd = "";
    private String currentSer = "";
    private String tostate = "";

    private final List<Map<String, Object>> diaryList = new ArrayList<>();
    private final List<Map<String, Object>> channelCodeList = new ArrayList<>();
    private final List<Map<String, Object>> keymanList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa130u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();

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
        etContHH = findViewById(R.id.etContHH);
        etContMM = findViewById(R.id.etContMM);
        etDiaryContent = findViewById(R.id.etDiaryContent);
        etReportContent = findViewById(R.id.etReportContent);
        tvContDt = findViewById(R.id.tvContDt);
        tvReportContentLabel = findViewById(R.id.tvReportContentLabel);
        tvSalesCoaching = findViewById(R.id.tvSalesCoaching);
        tvCoachingDt = findViewById(R.id.tvCoachingDt);
        layoutCoachingResult = findViewById(R.id.layoutCoachingResult);
        btnConfirmCoaching = findViewById(R.id.btnConfirmCoaching);

        spKeyman = findViewById(R.id.spKeyman);
        spChannel = findViewById(R.id.spChannel);
        cbReportYn = findViewById(R.id.cbReportYn);
        etActionItem = findViewById(R.id.etActionItem);
        cbActionStatus = findViewById(R.id.cbActionStatus);
        tvNextMeetDt = findViewById(R.id.tvNextMeetDt);
        etNextMeetAgenda = findViewById(R.id.etNextMeetAgenda);

        llDiaryList = findViewById(R.id.llDiaryList);

        findViewById(R.id.btnReset).setOnClickListener(v -> initializeForm());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnDelete).setOnClickListener(v -> delete());
        findViewById(R.id.btnSalesSearch).setOnClickListener(v -> openHelp());
        
        btnConfirmCoaching.setOnClickListener(v -> confirmCoaching());
        
        tvContDt.setOnClickListener(v -> showDatePicker(tvContDt));
        tvNextMeetDt.setOnClickListener(v -> showDatePicker(tvNextMeetDt));

        cbReportYn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tvReportContentLabel.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            etReportContent.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });
    }

    private void initializeForm() {
        String diaryTemplate = "[방문 목적]\n- \n\n[상담 내용]\n- \n\n[향후 계획]\n- ";
        currentSer = "";
        tostate = "";
        etContHH.setText("10");
        etContMM.setText("00");
        etDiaryContent.setText(diaryTemplate);
        etReportContent.setText("");
        cbReportYn.setChecked(false);
        tvContDt.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        
        layoutCoachingResult.setVisibility(View.GONE);
        etActionItem.setText("");
        cbActionStatus.setChecked(false);
        tvNextMeetDt.setText("");
        etNextMeetAgenda.setText("");
        
        spChannel.setSelection(0);
        if (spKeyman.getAdapter() != null && spKeyman.getAdapter().getCount() > 0) spKeyman.setSelection(0);
    }

    private void loadInitialData() {
        loadCode(spChannel, channelCodeList);
    }

    private void loadCode(Spinner sp, List<Map<String, Object>> list) {
        apiService.getHsaaCodes("710").enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Response<ApiResponse<List<Map<String, Object>>>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    list.clear(); list.addAll(r.body().getData());
                    setupSpinner(sp, list, "codenm", true);
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Throwable t) {}
        });
    }

    private void setupSpinner(Spinner sp, List<Map<String, Object>> data, String key, boolean hasEmpty) {
        List<String> items = new ArrayList<>();
        if (hasEmpty) items.add("선택안함");
        for (Map<String, Object> d : data) items.add(getStringVal(d, key));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
    }

    private void fetchSalesDetail() {
        apiService.getHsaaDetail(salesid).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Map<String, Object> mst = (Map<String, Object>) response.body().getData().get("master");
                    if (mst != null) {
                        etSalesTitle.setText(getStringVal(mst, "salestitle"));
                        custcd = getStringVal(mst, "custcd");
                        loadKeymen();
                        fetchDiaryHistory();
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void loadKeymen() {
        apiService.getHsaaKeyman(custcd).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    keymanList.clear(); keymanList.addAll(response.body().getData());
                    setupSpinner(spKeyman, keymanList, "name", false);
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchDiaryHistory() {
        apiService.getHsaaDiary(salesid).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    diaryList.clear(); diaryList.addAll(response.body().getData());
                    refreshDiaryList();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    private void refreshDiaryList() {
        llDiaryList.removeAllViews();
        for (Map<String, Object> item : diaryList) {
            View v = getLayoutInflater().inflate(R.layout.item_mhsaa130u_row, llDiaryList, false);
            ((TextView) v.findViewById(R.id.tvRowDate)).setText(formatDate(getStringVal(item, "contdt")));
            ((TextView) v.findViewById(R.id.tvRowSummary)).setText(getStringVal(item, "content"));
            ((TextView) v.findViewById(R.id.tvRowState)).setText(getStringVal(item, "statenm"));
            
            v.setOnClickListener(v1 -> loadDiaryDetail(item));
            llDiaryList.addView(v);
        }
    }

    private void loadDiaryDetail(Map<String, Object> item) {
        currentSer = getStringVal(item, "ser");
        tostate = getStringVal(item, "tostate");
        etContHH.setText(getStringVal(item, "conthh"));
        etContMM.setText(getStringVal(item, "contmm"));
        etDiaryContent.setText(getStringVal(item, "diarycontent"));
        etReportContent.setText(getStringVal(item, "reportcontent"));
        cbReportYn.setChecked("Y".equals(getStringVal(item, "reportyn")));
        tvContDt.setText(formatDate(getStringVal(item, "contdt")));
        
        String coaching = getStringVal(item, "salescoaching");
        if (!coaching.isEmpty()) {
            layoutCoachingResult.setVisibility(View.VISIBLE);
            tvSalesCoaching.setText(coaching);
            tvCoachingDt.setText(String.format(Locale.getDefault(), "코칭일: %s", formatDate(getStringVal(item, "reportdt"))));
            
            boolean isRead = "Y".equalsIgnoreCase(getStringVal(item, "coachingreadyn"));
            btnConfirmCoaching.setVisibility(isRead ? View.GONE : View.VISIBLE);
        } else {
            layoutCoachingResult.setVisibility(View.GONE);
            etActionItem.setText("");
            cbActionStatus.setChecked(false);
            tvNextMeetDt.setText("");
            etNextMeetAgenda.setText("");
        }
        
        setSpinnerSelection(spChannel, channelCodeList, "codecd", getStringVal(item, "channel"), true);
        setSpinnerSelection(spKeyman, keymanList, "custid", getStringVal(item, "custid"), false);

        etActionItem.setText(getStringVal(item, "actionItem"));
        cbActionStatus.setChecked("1".equals(getStringVal(item, "actionStatus")));
        tvNextMeetDt.setText(formatDate(getStringVal(item, "nextMeetDt")));
        etNextMeetAgenda.setText(getStringVal(item, "nextMeetAgenda"));
    }

    private void confirmCoaching() {
        if (salesid.isEmpty() || currentSer.isEmpty()) return;

        Map<String, Object> p = new HashMap<>();
        p.put("salesid", salesid);
        p.put("ser", currentSer);
        p.put("contdt", tvContDt.getText().toString().replace("-", ""));

        apiService.confirmHsaaCoaching(p).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSAA130U.this, "지시사항 확인을 완료했습니다.", Toast.LENGTH_SHORT).show();
                    fetchDiaryHistory();
                    btnConfirmCoaching.setVisibility(View.GONE);
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {}
        });
    }

    private void save() {
        if (salesid.isEmpty()) { Toast.makeText(this, "영업건을 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (etDiaryContent.getText().toString().isEmpty()) { Toast.makeText(this, "내용을 입력하세요.", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> payload = new HashMap<>();
        payload.put("cmpycd", cmpycd);
        payload.put("salesid", salesid);
        payload.put("custcd", custcd);
        payload.put("ser", currentSer);
        payload.put("contdt", tvContDt.getText().toString().replace("-", ""));
        payload.put("conthh", etContHH.getText().toString());
        payload.put("contmm", etContMM.getText().toString());
        payload.put("diarycontent", etDiaryContent.getText().toString());
        payload.put("reportyn", cbReportYn.isChecked() ? "Y" : "N");
        payload.put("reportcontent", etReportContent.getText().toString());
        payload.put("tostate", tostate);
        
        String rawContent = etDiaryContent.getText().toString();
        String content = rawContent.length() > 50 ? rawContent.substring(0, 50) : rawContent;
        payload.put("content", content);

        if (spKeyman.getSelectedItemPosition() >= 0) payload.put("custid", getStringVal(keymanList.get(spKeyman.getSelectedItemPosition()), "custid"));
        
        int chanIdx = spChannel.getSelectedItemPosition();
        if (chanIdx > 0) payload.put("channel", getStringVal(channelCodeList.get(chanIdx - 1), "codecd"));

        payload.put("actionItem", etActionItem.getText().toString().trim());
        payload.put("actionStatus", cbActionStatus.isChecked() ? "1" : "0");
        payload.put("nextMeetDt", tvNextMeetDt.getText().toString().replace("-", "").trim());
        payload.put("nextMeetAgenda", etNextMeetAgenda.getText().toString().trim());

        apiService.saveHsaaDiary(payload).enqueue(new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSAA130U.this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchDiaryHistory(); initializeForm();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Object>> call, @NonNull Throwable t) {}
        });
    }

    private void delete() {
        if (currentSer.isEmpty()) return;
        new AlertDialog.Builder(this).setTitle("삭제").setMessage("상담일지를 삭제하시겠습니까?")
            .setPositiveButton("예", (d, w) -> performDelete())
            .setNegativeButton("아니오", null).show();
    }

    private void performDelete() {
        apiService.deleteHsaaDiary(salesid, currentSer).enqueue(new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Object>> c, @NonNull Response<ApiResponse<Object>> r) {
                Toast.makeText(MHSAA130U.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                fetchDiaryHistory(); initializeForm();
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Object>> c, @NonNull Throwable t) {}
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
            @Override public View getView(int p, View convertView, ViewGroup pr) {
                final View view;
                if (convertView == null) {
                    view = getLayoutInflater().inflate(R.layout.item_hsaa_pop, pr, false);
                } else {
                    view = convertView;
                }
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

    private void setSpinnerSelection(Spinner sp, List<Map<String, Object>> list, String key, String val, boolean hasEmpty) {
        int offset = hasEmpty ? 1 : 0;
        for (int i = 0; i < list.size(); i++) {
            if (getStringVal(list.get(i), key).equals(val)) {
                sp.setSelection(i + offset);
                break;
            }
        }
    }

    private String formatDate(String dateStr) {
        String d = dateStr;
        if (d == null || d.isEmpty()) return "";
        if (d.contains(" ")) d = d.split(" ")[0];
        return d.length() == 8 ? String.format("%s-%s-%s", d.substring(0,4), d.substring(4,6), d.substring(6,8)) : d;
    }

    @Override protected String getProgramTitle() { return "상담일지 등록"; }
    @Override protected String getProgramId() { return "MHSAA130U"; }
}

package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.PopupAdapter;
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
 * 🚀 [MHSAA110U] 영업건 등록/상세
 */
@SuppressLint({"SetTextI18n", "NotifyDataSetChanged", "DefaultLocale"})
@SuppressWarnings("unchecked")
public class MHSAA110U extends BaseActivity {

    private EditText etSalesId, etCustNm, etBossNm, etTelNo, etSalesTitle, etForeAmt, etSuccRate, etSalesRemark;
    private TextView tvAddTime, tvForeDt;
    private Spinner spUser, spState, spImportRank, spRtnCd, spChoice;
    private LinearLayout llItemList;
    
    private ApiService apiService;
    private String cmpycd, userid, deptcd;
    private String salesid = "";
    
    private final List<Map<String, Object>> userDataList = new ArrayList<>();
    private final List<Map<String, Object>> stateCodeList = new ArrayList<>();
    private final List<Map<String, Object>> rankCodeList = new ArrayList<>();
    private final List<Map<String, Object>> rtnCodeList = new ArrayList<>();
    private final List<Map<String, Object>> choiceCodeList = new ArrayList<>();
    private final List<Map<String, Object>> salesItems = new ArrayList<>();
    private final Map<String, Object> masterData = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa110u);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "coit").trim();
        userid = prefs.getString("userId", "");
        deptcd = prefs.getString("deptcd", "");

        apiService = RetrofitClient.getApiService();

        initViews();
        
        salesid = getIntent().getStringExtra("salesid");
        if (salesid == null) salesid = "";

        loadInitialData();

        if (!salesid.isEmpty()) {
            fetchDetail();
        } else {
            initializeForm();
        }
    }

    private void initViews() {
        etSalesId = findViewById(R.id.etSalesId);
        etCustNm = findViewById(R.id.etCustNm);
        etBossNm = findViewById(R.id.etBossNm);
        etTelNo = findViewById(R.id.etTelNo);
        etSalesTitle = findViewById(R.id.etSalesTitle);
        etForeAmt = findViewById(R.id.etForeAmt);
        etSuccRate = findViewById(R.id.etSuccRate);
        etSalesRemark = findViewById(R.id.etSalesRemark);
        
        tvAddTime = findViewById(R.id.tvAddTime);
        tvForeDt = findViewById(R.id.tvForeDt);
        
        spUser = findViewById(R.id.spUser);
        spState = findViewById(R.id.spState);
        spImportRank = findViewById(R.id.spImportRank);
        spRtnCd = findViewById(R.id.spRtnCd);
        spChoice = findViewById(R.id.spChoice);
        
        llItemList = findViewById(R.id.llItemList);

        findViewById(R.id.btnReset).setOnClickListener(v -> initializeForm());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnDelete).setOnClickListener(v -> delete());
        findViewById(R.id.btnAddItem).setOnClickListener(v -> openHelp("ITEM"));
        findViewById(R.id.btnSalesSearch).setOnClickListener(v -> showSalesSearchPopup());
        
        etCustNm.setOnClickListener(v -> openHelp("CUST"));
        etTelNo.setOnClickListener(v -> makeCall(etTelNo.getText().toString()));
        tvAddTime.setOnClickListener(v -> showDatePicker(tvAddTime));
        tvForeDt.setOnClickListener(v -> showDatePicker(tvForeDt));

        etForeAmt.addTextChangedListener(new TextWatcher() {
            private String current = "";
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    etForeAmt.removeTextChangedListener(this);
                    String cleanString = s.toString().replaceAll("\\D", "");
                    if (!cleanString.isEmpty()) {
                        double parsed = Double.parseDouble(cleanString);
                        String formatted = new DecimalFormat("#,###").format(parsed);
                        current = formatted;
                        etForeAmt.setText(formatted);
                        etForeAmt.setSelection(formatted.length());
                    } else {
                        current = "";
                        etForeAmt.setText("");
                    }
                    etForeAmt.addTextChangedListener(this);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void initializeForm() {
        salesid = "";
        masterData.clear();
        salesItems.clear();
        
        etSalesId.setText("");
        etCustNm.setText("");
        etBossNm.setText("");
        etTelNo.setText("");
        etSalesTitle.setText("");
        etForeAmt.setText("0");
        etSuccRate.setText("30");
        etSalesRemark.setText("");
        
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvAddTime.setText(today);
        tvForeDt.setText(today);
        
        refreshItemList();
        
        // 유저 초기화 (본인)
        for (int i = 0; i < userDataList.size(); i++) {
            if (getStringVal(userDataList.get(i), "userid").equals(userid)) {
                spUser.setSelection(i);
                break;
            }
        }
    }

    private void showSalesSearchPopup() {
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

        List<Map<String, Object>> popList = new ArrayList<>();
        BaseAdapter popAdapter = new BaseAdapter() {
            @Override public int getCount() { return popList.size(); }
            @Override public Object getItem(int p) { return popList.get(p); }
            @Override public long getItemId(int p) { return p; }
            @Override public View getView(int p, View v, ViewGroup pr) {
                if (v == null) v = LayoutInflater.from(pr.getContext()).inflate(R.layout.item_hsaa_pop, pr, false);
                Map<String, Object> item = popList.get(p);
                ((TextView) v.findViewById(R.id.tvPopCustNm)).setText(getStringVal(item, "custnm"));
                ((TextView) v.findViewById(R.id.tvPopSalesTitle)).setText(getStringVal(item, "salestitle"));
                ((TextView) v.findViewById(R.id.tvPopAddTime)).setText(formatDate(getStringVal(item, "addtime")));
                return v;
            }
        };
        lv.setAdapter(popAdapter);

        AlertDialog dialog = builder.create();
        dialogView.findViewById(R.id.btnPopSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("fromdt", tvStart.getText().toString().replace("-", ""));
            p.put("todt", tvEnd.getText().toString().replace("-", ""));
            p.put("schcustnm", etSch.getText().toString().trim());

            apiService.getHsaaMaster(p).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
                @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        popList.clear(); popList.addAll(response.body().getData()); popAdapter.notifyDataSetChanged();
                    }
                }
                @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
            });
        });

        lv.setOnItemClickListener((parent, view, position, id) -> {
            salesid = getStringVal(popList.get(position), "salesid");
            fetchDetail();
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.btnPopClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        dialogView.findViewById(R.id.btnPopSearch).performClick();
    }

    private void loadInitialData() {
        // 유저 로드
        apiService.getHsaaUsers().enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Response<ApiResponse<List<Map<String, Object>>>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    userDataList.clear(); userDataList.addAll(r.body().getData());
                    setupSpinner(spUser, userDataList, "usernm");
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Throwable t) {}
        });

        // 공통코드 로드 (700:진행상태, 730:중요도, 695:유치경로, 740:선정방법)
        loadCode("700", spState, stateCodeList);
        loadCode("730", spImportRank, rankCodeList);
        loadCode("695", spRtnCd, rtnCodeList);
        loadCode("740", spChoice, choiceCodeList);
    }

    private void loadCode(String group, Spinner sp, List<Map<String, Object>> list) {
        apiService.getHsaaCodes(group).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Response<ApiResponse<List<Map<String, Object>>>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    list.clear(); list.addAll(r.body().getData());
                    setupSpinner(sp, list, "codenm");
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

    private void fetchDetail() {
        apiService.getHsaaDetail(salesid).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = response.body().getData();
                    Map<String, Object> mst = (Map<String, Object>) data.get("master");
                    List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");

                    if (mst != null) {
                        masterData.clear(); masterData.putAll(mst);
                        etSalesId.setText(getStringVal(mst, "salesid"));
                        etCustNm.setText(getStringVal(mst, "custnm"));
                        etBossNm.setText(getStringVal(mst, "bossnm"));
                        etTelNo.setText(getStringVal(mst, "telno"));
                        etSalesTitle.setText(getStringVal(mst, "salestitle"));
                        
                        double foreAmt = getDoubleVal(mst, "foreamt");
                        etForeAmt.setText(new DecimalFormat("#,###").format(foreAmt));
                        
                        etSuccRate.setText(getStringVal(mst, "succrate"));
                        etSalesRemark.setText(getStringVal(mst, "salesremark"));
                        tvAddTime.setText(formatDate(getStringVal(mst, "addtime")));
                        tvForeDt.setText(formatDate(getStringVal(mst, "foredt")));
                        
                        setSpinnerSelection(spUser, userDataList, "userid", getStringVal(mst, "userid"));
                        setSpinnerSelection(spState, stateCodeList, "codecd", getStringVal(mst, "state"));
                        setSpinnerSelection(spImportRank, rankCodeList, "codecd", getStringVal(mst, "importrank"));
                        setSpinnerSelection(spRtnCd, rtnCodeList, "codecd", getStringVal(mst, "rtncd"));
                        setSpinnerSelection(spChoice, choiceCodeList, "codecd", getStringVal(mst, "choice"));
                    }
                    
                    if (items != null) {
                        salesItems.clear(); salesItems.addAll(items); refreshItemList();
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void setSpinnerSelection(Spinner sp, List<Map<String, Object>> list, String key, String val) {
        for (int i = 0; i < list.size(); i++) {
            if (getStringVal(list.get(i), key).equals(val)) {
                sp.setSelection(i);
                break;
            }
        }
    }

    private void refreshItemList() {
        llItemList.removeAllViews();
        DecimalFormat df = new DecimalFormat("#,###");
        
        for (int i = 0; i < salesItems.size(); i++) {
            Map<String, Object> item = salesItems.get(i);
            View v = getLayoutInflater().inflate(R.layout.item_mhsaa110u, llItemList, false);
            
            ((TextView) v.findViewById(R.id.tvItemNm)).setText(getStringVal(item, "itemnm"));
            ((TextView) v.findViewById(R.id.tvItemCd)).setText(getStringVal(item, "itemcd"));
            
            EditText etQty = v.findViewById(R.id.etQty);
            TextView tvAmt = v.findViewById(R.id.tvAmt);
            
            double qty = getDoubleVal(item, "qty");
            double price = getDoubleVal(item, "unitprice");
            etQty.setText(String.valueOf(qty));
            tvAmt.setText(df.format(qty * price));
            
            final int pos = i;
            etQty.setOnFocusChangeListener((v1, focus) -> {
                if (!focus) {
                    try {
                        double nQty = Double.parseDouble(etQty.getText().toString());
                        item.put("qty", nQty);
                        item.put("amt", nQty * price);
                        tvAmt.setText(df.format(nQty * price));
                    } catch (Exception ignored) {}
                }
            });
            
            v.findViewById(R.id.btnDelete).setOnClickListener(v1 -> {
                salesItems.remove(pos);
                refreshItemList();
            });
            
            llItemList.addView(v);
        }
    }

    private void save() {
        if (etCustNm.getText().toString().isEmpty()) { Toast.makeText(this, "거래처를 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (etSalesTitle.getText().toString().isEmpty()) { Toast.makeText(this, "영업건명을 입력하세요.", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> mst = new HashMap<>(masterData);
        mst.put("cmpycd", cmpycd);
        mst.put("salesid", salesid);
        mst.put("salestitle", etSalesTitle.getText().toString());
        mst.put("addtime", tvAddTime.getText().toString().replace("-", ""));
        mst.put("foredt", tvForeDt.getText().toString().replace("-", ""));
        mst.put("foreamt", etForeAmt.getText().toString().replaceAll("\\D", ""));
        mst.put("succrate", etSuccRate.getText().toString());
        mst.put("salesremark", etSalesRemark.getText().toString());
        
        int uPos = spUser != null ? spUser.getSelectedItemPosition() : -1;
        if (uPos >= 0 && uPos < userDataList.size()) mst.put("userid", getStringVal(userDataList.get(uPos), "userid"));
        
        int sPos = spState != null ? spState.getSelectedItemPosition() : -1;
        if (sPos >= 0 && sPos < stateCodeList.size()) mst.put("state", getStringVal(stateCodeList.get(sPos), "codecd"));
        
        int iPos = spImportRank != null ? spImportRank.getSelectedItemPosition() : -1;
        if (iPos >= 0 && iPos < rankCodeList.size()) mst.put("importrank", getStringVal(rankCodeList.get(iPos), "codecd"));
        
        int rPos = spRtnCd != null ? spRtnCd.getSelectedItemPosition() : -1;
        if (rPos >= 0 && rPos < rtnCodeList.size()) mst.put("rtncd", getStringVal(rtnCodeList.get(rPos), "codecd"));
        
        int cPos = spChoice != null ? spChoice.getSelectedItemPosition() : -1;
        if (cPos >= 0 && cPos < choiceCodeList.size()) mst.put("choice", getStringVal(choiceCodeList.get(cPos), "codecd"));
        
        mst.put("deptcd", deptcd);
        mst.put("updemp", userid);

        Map<String, Object> payload = new HashMap<>();
        payload.put("master", mst);
        payload.put("items", salesItems);

        apiService.saveHsaaMaster(payload).enqueue(new Callback<ApiResponse<String>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<String>> call, @NonNull Response<ApiResponse<String>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(MHSAA110U.this, "저장되었습니다.", Toast.LENGTH_SHORT).show();
                    salesid = response.body().getData();
                    fetchDetail();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<String>> call, @NonNull Throwable t) {}
        });
    }

    private void delete() {
        if (salesid.isEmpty()) return;
        new AlertDialog.Builder(this).setTitle("삭제 확인").setMessage("정말 삭제하시겠습니까?")
            .setPositiveButton("예", (d, w) -> {
                apiService.deleteHsaaMaster(salesid).enqueue(new Callback<ApiResponse<Object>>() {
                    @Override public void onResponse(@NonNull Call<ApiResponse<Object>> c, @NonNull Response<ApiResponse<Object>> r) {
                        Toast.makeText(MHSAA110U.this, "삭제되었습니다.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    @Override public void onFailure(@NonNull Call<ApiResponse<Object>> c, @NonNull Throwable t) {}
                });
            }).setNegativeButton("아니오", null).show();
    }

    private void openHelp(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setTitle(type.equals("CUST") ? "거래처 선택" : "품목 선택").setView(dialogView);
        
        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();

        PopupAdapter popupAdapter = new PopupAdapter(list, type, item -> {
            if (type.equals("CUST")) {
                masterData.put("custcd", getStringVal(item, "custcd"));
                etCustNm.setText(getStringVal(item, "custnm"));
                etBossNm.setText(getStringVal(item, "bossnm"));
                etTelNo.setText(getStringVal(item, "telno"));
            } else {
                Map<String, Object> row = new HashMap<>();
                String itemCd = getStringVal(item, "itemcd");
                if (itemCd.isEmpty()) itemCd = getStringVal(item, "code");
                String itemNm = getStringVal(item, "itemnm");
                if (itemNm.isEmpty()) itemNm = getStringVal(item, "cdnm");
                String itSize = getStringVal(item, "itsize");
                String unit = getStringVal(item, "unit");
                
                double price = getDoubleVal(item, "outprice");
                if (price == 0) price = getDoubleVal(item, "outcost");
                if (price == 0) price = getDoubleVal(item, "price");

                row.put("itemcd", itemCd);
                row.put("itemnm", itemNm);
                row.put("itsize", itSize);
                row.put("unit", unit);
                row.put("qty", 1.0);
                row.put("unitprice", price);
                row.put("amt", price);
                salesItems.add(row);
                refreshItemList();
            }
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>(); 
            p.put("cmpycd", cmpycd);
            String keyword = etSearch.getText().toString().trim();
            
            if (type.equals("CUST")) {
                p.put("gubun", "C4"); 
                p.put("gbncd", ""); 
                p.put("code", ""); 
                p.put("remark", keyword);
                apiService.executeHa00Procedure("HA00_00P_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful() && r.body() != null) { list.clear(); list.addAll(r.body()); popupAdapter.notifyDataSetChanged(); }
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            } else {
                p.put("gubun", "I1"); 
                p.put("codenm", keyword); 
                p.put("gbncd", "2"); // 🚀 MHSOD100U 매출품목(제품) 표준 구분코드 "2" 적용
                p.put("code", ""); 
                p.put("etcval", "");
                apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful() && r.body() != null) { list.clear(); list.addAll(r.body()); popupAdapter.notifyDataSetChanged(); }
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            }
        });
        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        dialogView.findViewById(R.id.btnSearch).performClick();
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

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private double getDoubleVal(Map<String, Object> map, String key) {
        try { return Double.parseDouble(getStringVal(map, key).replace(",", "")); } catch (Exception e) { return 0.0; }
    }

    private String formatDate(String d) {
        if (d == null || d.isEmpty()) return "";
        // 만약 시분초가 포함된 형태(2026-08-18 00:00:00.0)라면 날짜만 추출
        if (d.contains(" ")) {
            d = d.split(" ")[0];
        }
        return d.length() == 8 ? String.format("%s-%s-%s", d.substring(0,4), d.substring(4,6), d.substring(6,8)) : d;
    }

    @Override protected String getProgramTitle() { return "영업건 등록"; }
    @Override protected String getProgramId() { return "MHSAA110U"; }
}

package com.crmbank.erp.mobile.hsqt;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.crmbank.erp.mobile.ApiService;
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
 * 🚀 [MHSQT_100U] 견적서 등록 프로그램 (모바일 - MHSOD100U 주문등록 팝업 및 그리드 패턴 완벽 적용)
 */
public class MHSQT100U extends AppCompatActivity {

    private TextView tvHeaderTitle, tvQuatYmd, tvValidDate;
    private EditText etQuatYm, etQuatNo, etCustName, etCustEmp, etEmail, etRemark;
    private AppCompatButton btnNew, btnSave, btnSearch, btnAddRow;
    private ListView lvDetailList;

    private ApiService apiService;
    private String cmpycd = "COIT";
    private QuotationDetailAdapter detailAdapter;
    private final List<Map<String, Object>> detailList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quotation_register);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "COIT").trim();

        apiService = RetrofitClient.getApiService();

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderTitle.setText("견적서 등록 (MHSQT_100U)");

        etQuatYm = findViewById(R.id.etQuatYm);
        etQuatNo = findViewById(R.id.etQuatNo);
        etCustName = findViewById(R.id.etCustName);
        etCustEmp = findViewById(R.id.etCustEmp);
        etEmail = findViewById(R.id.etEmail);
        tvQuatYmd = findViewById(R.id.tvQuatYmd);
        tvValidDate = findViewById(R.id.tvValidDate);
        etRemark = findViewById(R.id.etRemark);

        btnNew = findViewById(R.id.btnNew);
        btnSave = findViewById(R.id.btnSave);
        btnSearch = findViewById(R.id.btnSearch);
        btnAddRow = findViewById(R.id.btnAddRow);
        lvDetailList = findViewById(R.id.lvDetailList);

        String currentYm = new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(new Date());
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etQuatYm.setText(currentYm);
        etQuatNo.setText("0000");
        tvQuatYmd.setText(today);
        tvValidDate.setText(today);

        tvQuatYmd.setOnClickListener(v -> showDatePicker(tvQuatYmd));
        tvValidDate.setOnClickListener(v -> showDatePicker(tvValidDate));

        etCustName.setOnClickListener(v -> openHelp("CUST"));
        btnAddRow.setOnClickListener(v -> openHelp("ITEM"));

        detailAdapter = new QuotationDetailAdapter(this, detailList);
        lvDetailList.setAdapter(detailAdapter);

        addEmptyRow();

        btnNew.setOnClickListener(v -> resetForm());
        btnSearch.setOnClickListener(v -> openQuotationSearchPopup());
        btnSave.setOnClickListener(v -> saveQuotation());
    }

    private void showDatePicker(TextView targetView) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String dateStr = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            targetView.setText(dateStr);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openHelp(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_customer_search, null);
        builder.setView(dialogView);

        String titleText = type.equals("CUST") ? "거래처 검색" : "품목 검색";

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        if (tvTitle != null) tvTitle.setText(titleText);

        EditText etSearch = dialogView.findViewById(R.id.etSearchQuery);
        RecyclerView rv = dialogView.findViewById(R.id.rvPopupList);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<Map<String, Object>> list = new ArrayList<>();
        AlertDialog dialog = builder.create();

        PopupAdapter popupAdapter = new PopupAdapter(list, type, item -> {
            if (type.equals("CUST")) {
                etCustName.setText(getStringVal(item, "custnm"));
            } else {
                Map<String, Object> newItem = new HashMap<>();
                newItem.put("upkind", "A");
                newItem.put("qrowno", "");
                newItem.put("itemcd", getStringVal(item, "itemcd").isEmpty() ? getStringVal(item, "code") : getStringVal(item, "itemcd"));
                newItem.put("itemnm", getStringVal(item, "itemnm").isEmpty() ? getStringVal(item, "cdnm") : getStringVal(item, "itemnm"));
                newItem.put("itsize", getStringVal(item, "itsize"));
                newItem.put("unit", getStringVal(item, "unit").isEmpty() ? "EA" : getStringVal(item, "unit"));
                
                double price = getDoubleVal(item, "outprice");
                if (price == 0) price = getDoubleVal(item, "outcost");
                if (price == 0) price = getDoubleVal(item, "price");

                newItem.put("quatqty", 1.0);
                newItem.put("quatamt", price);
                newItem.put("quatvat", price * 0.1);
                
                // If first row is empty placeholder, replace it; otherwise add
                if (detailList.size() == 1 && getStringVal(detailList.get(0), "itemcd").isEmpty()) {
                    detailList.set(0, newItem);
                } else {
                    detailList.add(newItem);
                }
                detailAdapter.notifyDataSetChanged();
            }
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);

        dialogView.findViewById(R.id.btnSearch).setOnClickListener(v -> {
            Map<String, Object> p = new HashMap<>();
            p.put("cmpycd", cmpycd);
            String keyword = etSearch != null ? etSearch.getText().toString().trim() : "";

            if (type.equals("CUST")) {
                p.put("gubun", "C4");
                p.put("gbncd", "");
                p.put("code", "");
                p.put("remark", keyword);
                apiService.executeHa00Procedure("HA00_00P_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            list.clear(); list.addAll(r.body()); popupAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            } else {
                p.put("gubun", "I1");
                p.put("gbncd", "2");
                p.put("code", "");
                p.put("codenm", keyword);
                p.put("etcval", "");
                apiService.executeHs00Procedure("HS00_000S_STR", p).enqueue(new Callback<List<Map<String, Object>>>() {
                    @Override public void onResponse(@NonNull Call<List<Map<String, Object>>> c, @NonNull Response<List<Map<String, Object>>> r) {
                        if (r.isSuccessful() && r.body() != null) {
                            list.clear(); list.addAll(r.body()); popupAdapter.notifyDataSetChanged();
                        }
                    }
                    @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> c, @NonNull Throwable t) {}
                });
            }
        });

        dialogView.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        dialogView.findViewById(R.id.btnSearch).performClick();
    }

    private void openQuotationSearchPopup() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(24, 24, 24, 24);
        container.setBackgroundColor(0xFFFFFFFF);

        TextView title = new TextView(this);
        title.setText("견적내역 기간별 조회");
        title.setTextSize(18f);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setTextColor(0xFF333333);
        title.setPadding(0, 0, 0, 16);
        container.addView(title);

        android.widget.LinearLayout dateRow = new android.widget.LinearLayout(this);
        dateRow.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        dateRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        final TextView tvFrdt = new TextView(this);
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -3);
        String monthAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.getTime());
        tvFrdt.setText(monthAgo);
        tvFrdt.setBackgroundResource(R.drawable.bg_input_field);
        tvFrdt.setPadding(12, 12, 12, 12);
        tvFrdt.setTextSize(12f);
        tvFrdt.setTextColor(0xFF333333);
        tvFrdt.setOnClickListener(v -> showDatePicker(tvFrdt));
        dateRow.addView(tvFrdt, new android.widget.LinearLayout.LayoutParams(0, 110, 1f));

        TextView tvTilde = new TextView(this);
        tvTilde.setText(" ~ ");
        tvTilde.setGravity(android.view.Gravity.CENTER);
        tvTilde.setTextColor(0xFF333333);
        dateRow.addView(tvTilde);

        final TextView tvTodt = new TextView(this);
        tvTodt.setText(today);
        tvTodt.setBackgroundResource(R.drawable.bg_input_field);
        tvTodt.setPadding(12, 12, 12, 12);
        tvTodt.setTextSize(12f);
        tvTodt.setTextColor(0xFF333333);
        tvTodt.setOnClickListener(v -> showDatePicker(tvTodt));
        dateRow.addView(tvTodt, new android.widget.LinearLayout.LayoutParams(0, 110, 1f));

        container.addView(dateRow);

        final EditText etCust = new EditText(this);
        etCust.setHint("거래처명 검색 (선택사항)");
        etCust.setBackgroundResource(R.drawable.bg_input_field);
        etCust.setPadding(12, 12, 12, 12);
        etCust.setTextSize(13f);
        android.widget.LinearLayout.LayoutParams p2 = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 110);
        p2.setMargins(0, 12, 0, 12);
        container.addView(etCust, p2);

        AppCompatButton btnPopSearch = new AppCompatButton(this);
        btnPopSearch.setText("조회");
        btnPopSearch.setBackgroundResource(R.drawable.bg_button_gradient_gray);
        btnPopSearch.setTextColor(0xFFFFFFFF);
        container.addView(btnPopSearch, new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 110));

        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        android.widget.LinearLayout.LayoutParams p3 = new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 700);
        p3.setMargins(0, 16, 0, 16);
        container.addView(rv, p3);

        AppCompatButton btnClose = new AppCompatButton(this);
        btnClose.setText("닫기");
        btnClose.setBackgroundResource(R.drawable.bg_button_gradient_gray);
        btnClose.setTextColor(0xFFFFFFFF);
        container.addView(btnClose, new android.widget.LinearLayout.LayoutParams(android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 100));

        builder.setView(container);
        AlertDialog dialog = builder.create();

        List<Map<String, Object>> list = new ArrayList<>();
        PopupAdapter popupAdapter = new PopupAdapter(list, "CUST", item -> {
            String quatym = getStringVal(item, "quatym");
            String quatno = getStringVal(item, "quatno");
            if (!quatym.isEmpty()) etQuatYm.setText(quatym);
            if (!quatno.isEmpty()) etQuatNo.setText(quatno);

            etCustName.setText(getStringVal(item, "custnm"));
            etCustEmp.setText(getStringVal(item, "keymannm"));
            etEmail.setText(getStringVal(item, "email"));
            tvQuatYmd.setText(getStringVal(item, "quatymd"));
            tvValidDate.setText(getStringVal(item, "validdate"));
            etRemark.setText(getStringVal(item, "remark"));

            loadDetail(quatym, quatno);
            dialog.dismiss();
        });
        rv.setAdapter(popupAdapter);

        btnPopSearch.setOnClickListener(v -> {
            Map<String, Object> params = new HashMap<>();
            params.put("cmpycd", cmpycd);
            params.put("frdt", tvFrdt.getText().toString().trim());
            params.put("todt", tvTodt.getText().toString().trim());
            String custKw = etCust.getText().toString().trim();
            if (!custKw.isEmpty()) {
                params.put("custnm", custKw);
            }

            apiService.getHsqtMaster(params).enqueue(new Callback<List<Map<String, Object>>>() {
                @Override
                public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        list.clear();
                        for (Map<String, Object> m : response.body()) {
                            Map<String, Object> mapped = new HashMap<>(m);
                            String ym = getStringVal(m, "quatym");
                            String no = getStringVal(m, "quatno");
                            String cust = getStringVal(m, "custnm");
                            String ymd = getStringVal(m, "quatymd");
                            mapped.put("custnm", "[" + ymd + "] " + cust + " (" + ym + "-" + no + ")");
                            mapped.put("custcd", ym + no);
                            list.add(mapped);
                        }
                        popupAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(MHSQT100U.this, "견적내역 조회 실패", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                    Toast.makeText(MHSQT100U.this, "네트워크 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
        btnPopSearch.performClick();
    }

    private void addEmptyRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("upkind", "A");
        row.put("qrowno", "");
        row.put("itemcd", "");
        row.put("itemnm", "");
        row.put("itsize", "");
        row.put("unit", "EA");
        row.put("quatqty", 1.0);
        row.put("quatamt", 0.0);
        row.put("quatvat", 0.0);
        detailList.add(row);
        detailAdapter.notifyDataSetChanged();
    }

    private void resetForm() {
        String currentYm = new SimpleDateFormat("yyyyMM", Locale.getDefault()).format(new Date());
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        etQuatYm.setText(currentYm);
        etQuatNo.setText("0000");
        etCustName.setText("");
        etCustEmp.setText("");
        etEmail.setText("");
        tvQuatYmd.setText(today);
        tvValidDate.setText(today);
        etRemark.setText("");
        detailList.clear();
        addEmptyRow();
        Toast.makeText(this, "신규 작성 모드", Toast.LENGTH_SHORT).show();
    }

    private void searchQuotation() {
        String quatym = etQuatYm.getText().toString().trim();
        String quatno = etQuatNo.getText().toString().trim();

        if (quatym.length() == 0 || quatno.length() == 0) {
            Toast.makeText(this, "견적년월과 견적번호를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("quatym", quatym);
        params.put("quatno", quatno);

        apiService.getHsqtMaster(params).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    Map<String, Object> master = response.body().get(0);
                    etCustName.setText(getStringVal(master, "custnm"));
                    etCustEmp.setText(getStringVal(master, "keymannm"));
                    etEmail.setText(getStringVal(master, "email"));
                    tvQuatYmd.setText(getStringVal(master, "quatymd"));
                    tvValidDate.setText(getStringVal(master, "validdate"));
                    etRemark.setText(getStringVal(master, "remark"));
                    loadDetail(quatym, quatno);
                } else {
                    Toast.makeText(MHSQT100U.this, "조회된 견적서가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(MHSQT100U.this, "조회 실패: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadDetail(String quatym, String quatno) {
        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("quatym", quatym);
        params.put("quatno", quatno);

        apiService.getHsqtDetail(params).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    detailList.clear();
                    for (Map<String, Object> item : response.body()) {
                        item.put("upkind", "U");
                        detailList.add(item);
                    }
                    if (detailList.isEmpty()) addEmptyRow();
                    detailAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                Toast.makeText(MHSQT100U.this, "상세 조회 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveQuotation() {
        String quatym = etQuatYm.getText().toString().trim();
        String quatno = etQuatNo.getText().toString().trim();
        String custnm = etCustName.getText().toString().trim();

        if (quatym.length() == 0) {
            Toast.makeText(this, "견적년월을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (custnm.length() == 0) {
            Toast.makeText(this, "거래처명을 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> master = new HashMap<>();
        master.put("cmpycd", cmpycd);
        master.put("quatym", quatym);
        master.put("quatno", quatno);
        master.put("custnm", custnm);
        master.put("keymannm", etCustEmp.getText().toString().trim());
        master.put("email", etEmail.getText().toString().trim());
        master.put("quatymd", tvQuatYmd.getText().toString().replace("-", ""));
        master.put("validdate", tvValidDate.getText().toString().replace("-", ""));
        master.put("remark", etRemark.getText().toString());
        master.put("actkind", "0000".equals(quatno) ? "A" : "U");

        Map<String, Object> payload = new HashMap<>();
        payload.put("master", master);
        payload.put("details", detailList);

        apiService.saveHsqt(payload).enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSQT100U.this, "정상적으로 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    searchQuotation();
                } else {
                    Toast.makeText(MHSQT100U.this, "저장 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Toast.makeText(MHSQT100U.this, "저장 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key.toLowerCase());
        if (val == null) val = map.get(key.toUpperCase());
        return val != null ? String.valueOf(val).trim() : "";
    }

    private double getDoubleVal(Map<String, Object> map, String key) {
        String s = getStringVal(map, key);
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private int getIntVal(MHSQT100U mhsQT100U, Map<String, Object> map, String key) {
        return (int) getDoubleVal(map, key);
    }

    private static class QuotationDetailAdapter extends BaseAdapter {
        private final Context context;
        private final List<Map<String, Object>> items;

        public QuotationDetailAdapter(Context context, List<Map<String, Object>> items) {
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
                view = LayoutInflater.from(context).inflate(R.layout.item_mhsqt100u, parent, false);
                holder = new ViewHolder();
                holder.tvItemName = view.findViewById(R.id.tvItemName);
                holder.etQuantity = view.findViewById(R.id.etQuantity);
                holder.etPrice = view.findViewById(R.id.etPrice);
                holder.etAmount = view.findViewById(R.id.etAmount);
                holder.etVat = view.findViewById(R.id.etVat);
                holder.btnDelete = view.findViewById(R.id.btnDelete);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Map<String, Object> item = items.get(position);
            String itemNm = item.get("itemnm") != null ? String.valueOf(item.get("itemnm")) : "품목명";
            String itsize = item.get("itsize") != null ? String.valueOf(item.get("itsize")) : "";
            if (!itsize.isEmpty() && !"-".equals(itsize)) {
                itemNm += " (" + itsize + ")";
            }
            holder.tvItemName.setText(itemNm);

            Object qtyObj = item.get("quatqty");
            Object amtObj = item.get("quatamt");
            Object vatObj = item.get("quatvat");

            holder.etQuantity.setText(qtyObj != null ? String.valueOf(qtyObj) : "1");
            holder.etPrice.setText(amtObj != null ? String.valueOf(amtObj) : "0");
            holder.etAmount.setText(amtObj != null ? String.valueOf(amtObj) : "0");
            holder.etVat.setText(vatObj != null ? String.valueOf(vatObj) : "0");

            holder.btnDelete.setOnClickListener(v -> {
                if (items.size() > 1) {
                    items.remove(position);
                    notifyDataSetChanged();
                } else {
                    Toast.makeText(context, "최소 1개의 품목은 유지되어야 합니다.", Toast.LENGTH_SHORT).show();
                }
            });

            // TextWatchers for editable qty/price
            holder.etQuantity.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    try {
                        double qty = Double.parseDouble(s.toString().trim().isEmpty() ? "0" : s.toString().trim());
                        item.put("quatqty", qty);
                    } catch (Exception ignored) {}
                }
            });

            holder.etPrice.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override public void afterTextChanged(Editable s) {
                    try {
                        double price = Double.parseDouble(s.toString().trim().isEmpty() ? "0" : s.toString().trim());
                        double qty = 1.0;
                        try { qty = Double.parseDouble(String.valueOf(item.get("quatqty"))); } catch (Exception ignored) {}
                        double amt = price * qty;
                        double vat = amt * 0.1;
                        item.put("quatamt", amt);
                        item.put("quatvat", vat);
                        holder.etAmount.setText(String.valueOf(amt));
                        holder.etVat.setText(String.valueOf(vat));
                    } catch (Exception ignored) {}
                }
            });

            return view;
        }

        private static class ViewHolder {
            TextView tvItemName;
            EditText etQuantity, etPrice, etAmount, etVat;
            Button btnDelete;
        }
    }
}

package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
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
 * 🚀 [MHSAA400S] 기간별 영업상담 내역 조회 프로그램 (모바일)
 */
@SuppressLint({"SetTextI18n", "NotifyDataSetChanged", "DefaultLocale", "SimpleDateFormat"})
@SuppressWarnings({"unused", "FieldCanBeLocal"})
public class MHSAA400S extends BaseActivity {

    private TextView tvSdate, tvEdate;
    private EditText etCustNm;
    private Spinner spSalesman;

    private ApiService apiService;
    private String cmpycd;
    private final List<Map<String, Object>> salesmanList = new ArrayList<>();
    private final List<Map<String, Object>> consultationList = new ArrayList<>();
    private ConsultationAdapter consultationAdapter;
    private ArrayAdapter<String> salesmanAdapter;

    @Override
    protected String getProgramTitle() {
        return "기간별 영업상담 내역";
    }

    @Override
    protected String getProgramId() {
        return "HSAA400S";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa400s);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        cmpycd = prefs.getString("cmpycd", "COIT").trim();

        apiService = RetrofitClient.getApiService();

        TextView tvHeaderTitle = findViewById(R.id.tvHeaderTitle);
        tvHeaderTitle.setText("기간별 영업상담 내역 (MHSAA400S)");

        tvSdate = findViewById(R.id.tvSdate);
        tvEdate = findViewById(R.id.tvEdate);
        etCustNm = findViewById(R.id.etCustNm);
        spSalesman = findViewById(R.id.spSalesman);
        ListView lvConsultations = findViewById(R.id.lvConsultations);
        AppCompatButton btnSearch = findViewById(R.id.btnSearch);

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        tvEdate.setText(sdf.format(cal.getTime()));
        cal.add(Calendar.MONTH, -1);
        tvSdate.setText(sdf.format(cal.getTime()));

        tvSdate.setOnClickListener(v -> showDatePicker(tvSdate));
        tvEdate.setOnClickListener(v -> showDatePicker(tvEdate));

        consultationAdapter = new ConsultationAdapter(this, consultationList);
        lvConsultations.setAdapter(consultationAdapter);

        salesmanAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        salesmanAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSalesman.setAdapter(salesmanAdapter);

        btnSearch.setOnClickListener(v -> fetchList());

        loadSalesmen();
        fetchList();
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

    private void loadSalesmen() {
        apiService.getHsaaUsers().enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    salesmanList.clear();
                    Map<String, Object> all = new HashMap<>();
                    all.put("userid", "");
                    all.put("usernm", "전체");
                    salesmanList.add(all);
                    salesmanList.addAll(response.body().getData());

                    List<String> names = new ArrayList<>();
                    for (Map<String, Object> m : salesmanList) {
                        Object usernmObj = m.get("usernm");
                        names.add(usernmObj != null ? String.valueOf(usernmObj) : "");
                    }
                    salesmanAdapter.clear();
                    salesmanAdapter.addAll(names);
                    salesmanAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSAA400S.this, "담당자 목록 로드 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchList() {
        String sdate = tvSdate.getText().toString().replace("-", "");
        String edate = tvEdate.getText().toString().replace("-", "");
        String custnm = etCustNm.getText().toString().trim();

        String selectedUserid = "";
        int selIdx = spSalesman.getSelectedItemPosition();
        if (selIdx > 0 && selIdx < salesmanList.size()) {
            Object useridObj = salesmanList.get(selIdx).get("userid");
            selectedUserid = useridObj != null ? String.valueOf(useridObj) : "";
        }

        Map<String, Object> params = new HashMap<>();
        params.put("cmpycd", cmpycd);
        params.put("sdate", sdate);
        params.put("edate", edate);
        if (!custnm.isEmpty()) params.put("custnm", custnm);
        if (!selectedUserid.isEmpty()) params.put("userid", selectedUserid);

        apiService.getHsaaConsultationList(params).enqueue(new Callback<ApiResponse<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Response<ApiResponse<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    Object listObj = response.body().getData().get("list");
                    if (listObj instanceof List) {
                        consultationList.clear();
                        for (Object obj : (List<?>) listObj) {
                            if (obj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> map = (Map<String, Object>) obj;
                                consultationList.add(map);
                            }
                        }
                        consultationAdapter.notifyDataSetChanged();
                        Toast.makeText(MHSAA400S.this, "조회 완료 (" + consultationList.size() + "건)", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MHSAA400S.this, "조회 실패", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {
                Toast.makeText(MHSAA400S.this, "조회 오류: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static class ConsultationAdapter extends BaseAdapter {
        private final Context context;
        private final List<Map<String, Object>> items;

        public ConsultationAdapter(Context context, List<Map<String, Object>> items) {
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
                view = LayoutInflater.from(context).inflate(R.layout.item_mhsaa400s, parent, false);
                holder = new ViewHolder();
                holder.tvCustNm = view.findViewById(R.id.tvCustNm);
                holder.tvDate = view.findViewById(R.id.tvDate);
                holder.tvUserNm = view.findViewById(R.id.tvUserNm);
                holder.tvContent = view.findViewById(R.id.tvContent);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            Map<String, Object> item = items.get(position);
            holder.tvCustNm.setText(item.get("custnm") != null ? String.valueOf(item.get("custnm")) : "거래처명");
            holder.tvDate.setText(item.get("salesdt") != null ? String.valueOf(item.get("salesdt")) : "");
            holder.tvUserNm.setText("영업담당: " + (item.get("usernm") != null ? String.valueOf(item.get("usernm")) : ""));
            
            String content = item.get("diarycontent") != null ? String.valueOf(item.get("diarycontent")) : 
                             (item.get("content") != null ? String.valueOf(item.get("content")) : "상담 내용 없음");
            holder.tvContent.setText(content);

            return view;
        }

        private static class ViewHolder {
            TextView tvCustNm, tvDate, tvUserNm, tvContent;
        }
    }
}

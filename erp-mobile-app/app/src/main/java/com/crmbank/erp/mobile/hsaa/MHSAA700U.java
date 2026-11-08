package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.RetrofitClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSAA700U] 영업활동 방문계획 조회 및 캘린더 연동
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
        return "MHSAA700U";
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

        AppCompatButton btnSyncCalendar = findViewById(R.id.btnSyncCalendar);
        btnSyncCalendar.setOnClickListener(v -> syncToCalendar());

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
                            for (Map<String, Object> item : data) {
                                item.put("selected", true);
                                planList.add(item);
                            }
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

    private long getPrimaryCalendarId() {
        String[] projection = new String[]{
                android.provider.CalendarContract.Calendars._ID,
                android.provider.CalendarContract.Calendars.IS_PRIMARY
        };
        String selection = android.provider.CalendarContract.Calendars.VISIBLE + " = 1";
        try (android.database.Cursor cursor = getContentResolver().query(
                android.provider.CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                null,
                android.provider.CalendarContract.Calendars.IS_PRIMARY + " DESC")) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Exception ignored) {}
        return 1L;
    }

    private String getCalendarAccountName(long calendarId) {
        String[] projection = new String[]{
                android.provider.CalendarContract.Calendars.ACCOUNT_NAME,
                android.provider.CalendarContract.Calendars.CALENDAR_DISPLAY_NAME
        };
        String selection = android.provider.CalendarContract.Calendars._ID + " = ?";
        try (android.database.Cursor cursor = getContentResolver().query(
                android.provider.CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                new String[]{String.valueOf(calendarId)},
                null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String accountName = cursor.getString(0);
                String displayName = cursor.getString(1);
                if (displayName != null && !displayName.isEmpty() && !displayName.equals(accountName)) {
                    return displayName + " (" + accountName + ")";
                }
                return accountName != null ? accountName : "기본 캘린더";
            }
        } catch (Exception ignored) {}
        return "기본 캘린더";
    }

    private void syncToCalendar() {
        if (planList.isEmpty()) {
            Toast.makeText(this, "연동할 방문 계획이 없습니다. 먼저 조회하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.READ_CALENDAR,
                    android.Manifest.permission.WRITE_CALENDAR
            }, 1001);
            return;
        }

        long calendarId = getPrimaryCalendarId();
        String accountName = getCalendarAccountName(calendarId);

        int selectedCount = 0;
        for (Map<String, Object> item : planList) {
            Object selObj = item.get("selected");
            if (selObj == null || Boolean.TRUE.equals(selObj)) {
                selectedCount++;
            }
        }

        if (selectedCount == 0) {
            Toast.makeText(this, "연동할 항목을 최소 1개 이상 체크(선택)하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String salesmanTitle = usernm;
        try {
            if (spSalesman != null && spSalesman.getSelectedItem() != null) {
                salesmanTitle = spSalesman.getSelectedItem().toString();
            }
        } catch (Exception ignored) {}

        new AlertDialog.Builder(this)
            .setTitle("캘린더 연동 확인")
            .setMessage("현재 화면에 조회된 [" + salesmanTitle + "]의 방문 계획 중 선택된 " + selectedCount + "건을 [" + accountName + "] 캘린더에 연동하시겠습니까?\n\n(※ 반드시 본인의 방문 계획인지 확인 후 연동해 주세요.)")
            .setPositiveButton("연동", (dialog, which) -> executeCalendarSync(calendarId, accountName))
            .setNegativeButton("취소", null)
            .show();
    }

    private void executeCalendarSync(long calendarId, String accountName) {
        new Thread(() -> {
            int count = 0;
            int totalSelected = 0;
            for (Map<String, Object> item : planList) {
                Object selObj = item.get("selected");
                boolean isSelected = selObj == null || Boolean.TRUE.equals(selObj);
                if (!isSelected) continue;
                totalSelected++;

                String pDt = getStringVal(item, "plan_dt");
                String custNm = getStringVal(item, "custnm");
                String dealTitle = getStringVal(item, "deal_title");
                if (pDt.length() == 8) {
                    try {
                        int y = Integer.parseInt(pDt.substring(0, 4));
                        int m = Integer.parseInt(pDt.substring(4, 6)) - 1;
                        int d = Integer.parseInt(pDt.substring(6, 8));

                        Calendar cal = Calendar.getInstance();
                        cal.set(y, m, d, 9, 0, 0);
                        long startMillis = cal.getTimeInMillis();
                        cal.set(y, m, d, 18, 0, 0);
                        long endMillis = cal.getTimeInMillis();

                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(android.provider.CalendarContract.Events.DTSTART, startMillis);
                        values.put(android.provider.CalendarContract.Events.DTEND, endMillis);
                        values.put(android.provider.CalendarContract.Events.TITLE, "[영업방문] " + custNm + " - " + dealTitle);
                        values.put(android.provider.CalendarContract.Events.DESCRIPTION, getStringVal(item, "address"));
                        values.put(android.provider.CalendarContract.Events.CALENDAR_ID, calendarId);
                        values.put(android.provider.CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().getID());

                        getContentResolver().insert(android.provider.CalendarContract.Events.CONTENT_URI, values);
                        count++;
                    } catch (Exception ignored) {}
                }
            }
            final int finalCount = count;
            final int finalTotal = totalSelected;
            runOnUiThread(() -> Toast.makeText(MHSAA700U.this, "선택된 " + finalTotal + "건 중 총 " + finalCount + "건의 방문 계획이 [" + accountName + "] 캘린더에 연동되었습니다.", Toast.LENGTH_LONG).show());
        }).start();
    }

    private String getStringVal(Map<String, Object> map, String key) {
        if (map == null || map.get(key) == null) return "";
        return String.valueOf(map.get(key)).trim();
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
                holder.cbSelect = view.findViewById(R.id.cbSelect);
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

            Object selObj = item.get("selected");
            boolean isSelected = selObj == null || Boolean.TRUE.equals(selObj);
            holder.cbSelect.setOnCheckedChangeListener(null);
            holder.cbSelect.setChecked(isSelected);
            holder.cbSelect.setOnCheckedChangeListener((buttonView, isChecked) -> item.put("selected", isChecked));

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
            CheckBox cbSelect;
            TextView tvPlanDt, tvImport, tvCustNm, tvDealTitle, tvAddress;
        }
    }
}

package com.crmbank.erp.mobile.hsaa;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;

import com.crmbank.erp.mobile.ApiResponse;
import com.crmbank.erp.mobile.ApiService;
import com.crmbank.erp.mobile.BaseActivity;
import com.crmbank.erp.mobile.R;
import com.crmbank.erp.mobile.RetrofitClient;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 [MHSAA150U] 영업문서 관리
 */
@SuppressLint({"SetTextI18n", "SimpleDateFormat"})
@SuppressWarnings({"unchecked", "unused"})
public class MHSAA150U extends BaseActivity {

    private EditText etSalesTitle, etTitle, etFileName;
    private TextView tvAddTime;
    private Spinner spDocGb;
    private ImageView ivPreview;
    private LinearLayout llDocsList;

    private ApiService apiService;
    private String cmpycd, userid;
    private String salesid = "";
    private String custcd = "";
    private String currentSer = "";
    private File selectedFile;

    private final List<Map<String, Object>> docsHistory = new ArrayList<>();
    private final List<Map<String, Object>> docGbCodeList = new ArrayList<>();

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        @SuppressWarnings("deprecation")
                        Bitmap bitmap = (Bitmap) extras.get("data");
                        processBitmap(bitmap);
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) processUri(uri);
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mhsaa150u);

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
        etTitle = findViewById(R.id.etTitle);
        etFileName = findViewById(R.id.etFileName);
        tvAddTime = findViewById(R.id.tvAddTime);
        spDocGb = findViewById(R.id.spDocGb);
        ivPreview = findViewById(R.id.ivPreview);
        llDocsList = findViewById(R.id.llDocsList);

        findViewById(R.id.btnReset).setOnClickListener(v -> initializeForm());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnDelete).setVisibility(View.GONE);
        findViewById(R.id.btnSalesSearch).setOnClickListener(v -> openHelp());
        
        tvAddTime.setOnClickListener(v -> showDatePicker(tvAddTime));
        findViewById(R.id.btnCamera).setOnClickListener(v -> cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE)));
        findViewById(R.id.btnGallery).setOnClickListener(v -> galleryLauncher.launch("image/*"));
    }

    private void initializeForm() {
        currentSer = "";
        etTitle.setText("");
        etFileName.setText("");
        ivPreview.setVisibility(View.GONE);
        selectedFile = null;
        tvAddTime.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date()));
        spDocGb.setSelection(0);
    }

    private void loadInitialData() {
        apiService.getHsaaCodes("780").enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> c, @NonNull Response<ApiResponse<List<Map<String, Object>>>> r) {
                if (r.isSuccessful() && r.body() != null) {
                    docGbCodeList.clear(); docGbCodeList.addAll(r.body().getData());
                    setupSpinner(spDocGb, docGbCodeList);
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
                        custcd = getStringVal(mst, "custcd");
                        fetchDocsHistory();
                    }
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<Map<String, Object>>> call, @NonNull Throwable t) {}
        });
    }

    private void fetchDocsHistory() {
        apiService.getHsaaDocs(salesid).enqueue(new Callback<ApiResponse<List<Map<String, Object>>>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Response<ApiResponse<List<Map<String, Object>>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    docsHistory.clear(); docsHistory.addAll(response.body().getData());
                    refreshDocsList();
                }
            }
            @Override public void onFailure(@NonNull Call<ApiResponse<List<Map<String, Object>>>> call, @NonNull Throwable t) {}
        });
    }

    private void refreshDocsList() {
        llDocsList.removeAllViews();
        for (Map<String, Object> item : docsHistory) {
            View v = getLayoutInflater().inflate(R.layout.item_mhsaa150u_row, llDocsList, false);
            ((TextView) v.findViewById(R.id.tvRowDocGb)).setText(getStringVal(item, "docgbnm"));
            ((TextView) v.findViewById(R.id.tvRowTitle)).setText(getStringVal(item, "title"));
            ((TextView) v.findViewById(R.id.tvRowDate)).setText(formatDate(getStringVal(item, "addtime")));
            
            v.setOnClickListener(v1 -> loadDocDetail(item));
            llDocsList.addView(v);
        }
    }

    private void loadDocDetail(Map<String, Object> item) {
        currentSer = getStringVal(item, "ser");
        etTitle.setText(getStringVal(item, "title"));
        etFileName.setText(getStringVal(item, "filename"));
        tvAddTime.setText(formatDate(getStringVal(item, "addtime")));
        setSpinnerSelection(spDocGb, docGbCodeList, getStringVal(item, "docgb"));
        
        ivPreview.setVisibility(View.GONE); 
        selectedFile = null; 
    }

    private void setSpinnerSelection(Spinner sp, List<Map<String, Object>> list, String val) {
        for (int i = 0; i < list.size(); i++) {
            if (getStringVal(list.get(i), "codecd").equals(val)) {
                sp.setSelection(i);
                break;
            }
        }
    }

    private void processBitmap(Bitmap bitmap) {
        try {
            selectedFile = new File(getExternalCacheDir(), "upload_temp.jpg");
            FileOutputStream out = new FileOutputStream(selectedFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush(); out.close();
            
            etFileName.setText("camera_image.jpg");
            ivPreview.setImageBitmap(bitmap);
            ivPreview.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Log.e("MHSAA150U", "Error processing bitmap", e);
        }
    }

    private void processUri(Uri uri) {
        try {
            etFileName.setText("gallery_image.jpg");
            ivPreview.setImageURI(uri);
            ivPreview.setVisibility(View.VISIBLE);
            
            selectedFile = new File(getExternalCacheDir(), "upload_temp.jpg");
            try (FileOutputStream out = new FileOutputStream(selectedFile);
                 InputStream inputStream = getContentResolver().openInputStream(uri)) {
                if (inputStream != null) {
                    Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                    if (bitmap != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MHSAA150U", "Error processing URI", e);
        }
    }

    private void save() {
        if (salesid.isEmpty()) { Toast.makeText(this, "영업건을 선택하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (etTitle.getText().toString().isEmpty()) { Toast.makeText(this, "제목을 입력하세요.", Toast.LENGTH_SHORT).show(); return; }
        if (selectedFile == null) { Toast.makeText(this, "파일을 선택하세요.", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> doc = new HashMap<>();
        doc.put("cmpycd", cmpycd);
        doc.put("salesid", salesid);
        doc.put("custcd", custcd);
        doc.put("ser", currentSer);
        doc.put("title", etTitle.getText().toString());
        doc.put("addtime", tvAddTime.getText().toString().replace("-", ""));
        doc.put("updemp", userid);
        if (spDocGb.getSelectedItemPosition() >= 0) {
            doc.put("docgb", getStringVal(docGbCodeList.get(spDocGb.getSelectedItemPosition()), "codecd"));
        }

        String json = new Gson().toJson(doc);
        RequestBody docBody = RequestBody.create(json, MediaType.parse("text/plain"));
        
        RequestBody fileReqBody = RequestBody.create(selectedFile, MediaType.parse("image/*"));
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", selectedFile.getName(), fileReqBody);

        apiService.saveHsaaDoc(docBody, filePart).enqueue(new Callback<ApiResponse<Object>>() {
            @Override public void onResponse(@NonNull Call<ApiResponse<Object>> call, @NonNull Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(MHSAA150U.this, "문서가 업로드되었습니다.", Toast.LENGTH_SHORT).show();
                    fetchDocsHistory(); initializeForm();
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

    private String formatDate(String dateStr) {
        String d = dateStr;
        if (d == null || d.isEmpty()) return "";
        if (d.contains(" ")) d = d.split(" ")[0];
        return d.length() == 8 ? String.format("%s-%s-%s", d.substring(0,4), d.substring(4,6), d.substring(6,8)) : d;
    }

    @Override protected String getProgramTitle() { return "영업문서 등록"; }
    @Override protected String getProgramId() { return "MHSAA150U"; }
}

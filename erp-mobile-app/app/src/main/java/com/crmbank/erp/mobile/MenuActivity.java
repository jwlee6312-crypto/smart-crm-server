package com.crmbank.erp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 🚀 Premium Dashboard Style Menu
 */
public class MenuActivity extends AppCompatActivity {

    private RecyclerView rvMenu;
    private MenuAdapter adapter;
    private final List<Map<String, Object>> menuList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        TextView tvUserNameAndDept = findViewById(R.id.tvUserNameAndDept);
        TextView tvUserContact = findViewById(R.id.tvUserContact);
        TextView tvUserEmail = findViewById(R.id.tvUserEmail);
        findViewById(R.id.btnLogout).setOnClickListener(v -> performLogout());

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userName = prefs.getString("userName", "");
        String deptName = prefs.getString("deptnm", "");
        String email = prefs.getString("email", "");
        String innerNo = prefs.getString("innerNo", "");
        String hpno = prefs.getString("hpno", "");

        tvUserNameAndDept.setText((userName.isEmpty() ? "영업사원" : userName) + (deptName.isEmpty() ? "" : " | " + deptName));
        tvUserContact.setText("HP: " + (hpno.isEmpty() ? "-" : hpno) + "  |  내선: " + (innerNo.isEmpty() ? "-" : innerNo));
        tvUserEmail.setText(email.isEmpty() ? "email@domain.com" : email);

        rvMenu = findViewById(R.id.rvMenu);
        rvMenu.setLayoutManager(new LinearLayoutManager(this));
        rvMenu.setHasFixedSize(true);
        rvMenu.setItemViewCacheSize(20);
        
        adapter = new MenuAdapter(menuList, menu -> {
            String codecd = (String) menu.get("codecd");
            String codenm = (String) menu.get("codenm");
            
            // 🚀 Special Case: Personal Status vs User Management
            if ("HABA_910U".equals(codecd)) {
                startActivity(new Intent(this, com.crmbank.erp.mobile.haba.MHABA910U.class));
                return;
            }
            if ("HABA_920U".equals(codecd)) {
                startActivity(new Intent(this, com.crmbank.erp.mobile.haba.MHABA920U.class));
                return;
            }
            
            if (codecd != null && !codecd.isEmpty()) {
                Intent intent = new Intent(this, SubMenuActivity.class);
                intent.putExtra("UPMUCD", codecd);
                intent.putExtra("UPMUNM", codenm);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Program not ready: " + codenm, Toast.LENGTH_SHORT).show();
            }
        });
        rvMenu.setAdapter(adapter);

        // 🚀 Step 1: Instant Loading from Cache
        loadCachedMenus(prefs);

        // 🚀 Step 2: Background refresh for latest data
        loadTopMenus();
    }

    private void loadCachedMenus(SharedPreferences prefs) {
        String cachedJson = prefs.getString("cachedMenus", null);
        if (cachedJson != null) {
            try {
                List<Map<String, Object>> cachedList = new Gson().fromJson(cachedJson, new TypeToken<List<Map<String, Object>>>(){}.getType());
                if (cachedList != null) {
                    menuList.clear();
                    for (Map<String, Object> menu : cachedList) {
                        String codenm = getStringVal(menu, "codenm");
                        menu.put("menuNm", codenm);
                        menuList.add(menu);
                    }
                    adapter.notifyDataSetChanged();
                }
            } catch (Exception ignored) {}
        }
    }

    private void loadTopMenus() {
        ApiService apiService = RetrofitClient.getApiService();
        apiService.getTopMenusMobile().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(@NonNull Call<List<Map<String, Object>>> call, @NonNull Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    menuList.clear();
                    
                    for (Map<String, Object> rawMenu : response.body()) {
                        Map<String, Object> menu = new java.util.HashMap<>(rawMenu);
                        String codecd = getStringVal(menu, "codecd");
                        String codenm = getStringVal(menu, "codenm");
                        menu.put("menuNm", codenm);
                        menu.put("codecd", codecd);
                        menu.put("codenm", codenm);
                        menuList.add(menu);
                    }
                    adapter.notifyDataSetChanged();
                } else {
                    Toast.makeText(MenuActivity.this, "Failed to load menu: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(@NonNull Call<List<Map<String, Object>>> call, @NonNull Throwable t) {
                Toast.makeText(MenuActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStringVal(Map<String, Object> map, String key) {
        Object v = map.get(key.toLowerCase());
        if (v == null) v = map.get(key.toUpperCase());
        return v != null ? String.valueOf(v).trim() : "";
    }

    private void performLogout() {
        RetrofitClient.getApiService().logout().enqueue(new Callback<Void>() {
            @Override public void onResponse(Call<Void> call, Response<Void> response) { clearLocalDataAndRedirect(); }
            @Override public void onFailure(Call<Void> call, Throwable t) { clearLocalDataAndRedirect(); }
        });
    }

    private void clearLocalDataAndRedirect() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        Intent intent = new Intent(MenuActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

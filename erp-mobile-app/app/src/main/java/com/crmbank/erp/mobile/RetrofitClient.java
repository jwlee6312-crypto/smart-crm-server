package com.crmbank.erp.mobile;

import android.content.Context;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * 🚀 통합 레트로핏 클라이언트 (성능 최적화 튜닝 버전)
 * 1. 세션 유지 (CookieJar)
 * 2. 네트워크 디스크 캐싱 (10MB)
 * 3. 커넥션 풀링 (재사용성 향상)
 */
public class RetrofitClient {
    private static final String BASE_URL = Config.BASE_URL;
    private static Retrofit retrofit = null;
    private static Context context;
    
    private static final PersistentCookieJar cookieJar = new PersistentCookieJar();

    public static void init(Context ctx) {
        context = ctx.getApplicationContext();
    }

    public static ApiService getApiService() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            // 배포 시 로그 비활성화로 보안 및 속도 향상
            logging.setLevel(HttpLoggingInterceptor.Level.NONE); 

            Gson gson = new GsonBuilder()
                    .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                    .create();

            // 🚀 디스크 캐시 설정
            Cache cache = null;
            if (context != null) {
                File cacheDir = new File(context.getCacheDir(), "http_cache");
                cache = new Cache(cacheDir, 10 * 1024 * 1024); // 10MB
            }

            OkHttpClient client = new OkHttpClient.Builder()
                    .cookieJar(cookieJar)
                    .cache(cache)
                    .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(15, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(client)
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}

/**
 * 💡 전역 세션을 관리하는 강력한 쿠키 저장소
 */
class PersistentCookieJar implements CookieJar {
    private final List<Cookie> cookieStore = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        synchronized (cookieStore) {
            for (Cookie newCookie : cookies) {
                for (int i = 0; i < cookieStore.size(); i++) {
                    if (cookieStore.get(i).name().equals(newCookie.name())) {
                        cookieStore.remove(i);
                        break;
                    }
                }
                cookieStore.add(newCookie);
            }
        }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        synchronized (cookieStore) {
            List<Cookie> validCookies = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (Cookie c : cookieStore) {
                if (c.expiresAt() > now) {
                    validCookies.add(c);
                }
            }
            return validCookies;
        }
    }
}

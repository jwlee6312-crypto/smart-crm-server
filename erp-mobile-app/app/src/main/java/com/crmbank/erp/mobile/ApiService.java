package com.crmbank.erp.mobile;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface ApiService {
    
    @POST("comm/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @GET("comm/top-menus")
    Call<List<Map<String, Object>>> getTopMenus();

    @GET("comm/top-menus-mobile")
    Call<List<Map<String, Object>>> getTopMenusMobile();

    @GET("mobile/menus")
    Call<List<Map<String, Object>>> getMobileMenus();

    @POST("comm/HA00_200S_STR")
    Call<List<Map<String, Object>>> getDynamicMenus(@Body Map<String, Object> params);

    @POST("comm/{procedure}")
    Call<List<Map<String, Object>>> executeCommProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("ha00/{procedure}")
    Call<List<Map<String, Object>>> executeHa00Procedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("haba/{procedure}")
    Call<List<Map<String, Object>>> executeHabaProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hsba/{procedure}")
    Call<List<Map<String, Object>>> executeHsbaProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hs00/{procedure}")
    Call<List<Map<String, Object>>> executeHs00Procedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hsio/{procedure}")
    Call<List<Map<String, Object>>> executeHsioProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hsio/HSIO_104U_SAVE")
    Call<List<Map<String, Object>>> saveBarcodeScanHistory(@Body Map<String, Object> payload);

    @POST("hsio/HSIO_104U_STR")
    Call<List<Map<String, Object>>> getBarcodeScanHistory(@Body Map<String, Object> payload);

    @POST("logout")
    Call<Void> logout();

    @GET("popup/pop-item")
    Call<List<ItemDto>> searchItems(
        @Query("cmpycd") String cmpycd,
        @Query("nacd") String nacd,
        @Query("sch_itemnm") String schItemNm,
        @Query("sch_astkind") String schAstKind
    );

    @GET("popup/pop-cust")
    Call<List<CustomerDto>> searchCustomers(
            @Query("cmpycd") String cmpycd,
            @Query("nacd") String nacd,
            @Query("sch_custnm") String sch_custnm
    );

    @GET("code")
    Call<List<CodeDto>> getCommonCode(
            @Query("cmpycd") String cmpycd,
            @Query("nacd") String nacd,
            @Query("cdtype") String cdtype
    );

    @GET("user/select")
    Call<List<Map<String, Object>>> searchEmployees(
            @Query("cmpycd") String cmpycd,
            @Query("nacd") String nacd,
            @Query("schusernm") String schusernm,
            @Query("schuseyn") String schuseyn
    );

    // --- 콜백 관리 (HGOA110U) ---
    @GET("crm/inbound/callback-list")
    Call<List<Map<String, Object>>> getCallbackList(@QueryMap Map<String, Object> params);

    @POST("crm/inbound/interaction/save-response")
    Call<Map<String, Object>> saveCallbackResponse(@Body Map<String, Object> payload);

    // --- 영업활동 (HSAA) ---
    @GET("hsaa/users")
    Call<ApiResponse<List<Map<String, Object>>>> getHsaaUsers();

    @GET("hsaa/codes/{group}")
    Call<ApiResponse<List<Map<String, Object>>>> getHsaaCodes(@Path("group") String group);

    @GET("hsaa/master")
    Call<ApiResponse<List<Map<String, Object>>>> getHsaaMaster(@QueryMap Map<String, Object> params);

    @GET("hsaa/detail")
    Call<ApiResponse<Map<String, Object>>> getHsaaDetail(@Query("salesid") String salesid);

    @POST("hsaa/master/save")
    Call<ApiResponse<String>> saveHsaaMaster(@Body Map<String, Object> payload);

    @DELETE("hsaa/master/delete")
    Call<ApiResponse<Object>> deleteHsaaMaster(@Query("salesid") String salesid);

    @GET("hsaa/keyman")
    Call<ApiResponse<List<Map<String, Object>>>> getHsaaKeyman(@Query("custcd") String custcd);

    @POST("hsaa/keyman/save")
    Call<ApiResponse<Object>> saveHsaaKeyman(@Body Map<String, Object> payload);

    @DELETE("hsaa/keyman/delete")
    Call<ApiResponse<Object>> deleteHsaaKeyman(@Query("custid") String custid);

    @GET("hsaa/diary")
    Call<ApiResponse<List<Map<String, Object>>>> getHsaaDiary(@Query("salesid") String salesid);

    @POST("hsaa/diary/save")
    Call<ApiResponse<Object>> saveHsaaDiary(@Body Map<String, Object> payload);

    @DELETE("hsaa/diary/delete")
    Call<ApiResponse<Object>> deleteHsaaDiary(@Query("salesid") String salesid, @Query("ser") String ser);

    @GET("hsaa/stages")
    Call<ApiResponse<List<Map<String, Object>>>> getHsaaStages(@Query("salesid") String salesid);

    @POST("hsaa/stages/save")
    Call<ApiResponse<Object>> saveHsaaStage(@Body Map<String, Object> payload);

    @GET("hsaa/docs")
    Call<ApiResponse<List<Map<String, Object>>>> getHsaaDocs(@Query("salesid") String salesid);

    @GET("hsaa/dashboard/stats")
    Call<ApiResponse<List<Map<String, Object>>>> getHsaaDashboardStats(@QueryMap Map<String, Object> params);

    @POST("hsaa/dashboard/list")
    Call<ApiResponse<List<Map<String, Object>>>> getHsaaDashboardList(@QueryMap Map<String, Object> params);

    @GET("comm/session")
    Call<Map<String, Object>> getMyInfo();

    @POST("haba/update-my-status")
    Call<Map<String, Object>> updateMyStatus(@Body Map<String, Object> params);

    @GET("hsaa/consultations/list")
    Call<ApiResponse<Map<String, Object>>> getHsaaConsultationList(@QueryMap Map<String, Object> params);

    @POST("hsaa/consultations/coaching")
    Call<ApiResponse<Object>> saveHsaaCoaching(@Body Map<String, Object> payload);

    @POST("hsaa/consultations/coaching/confirm")
    Call<ApiResponse<Object>> confirmHsaaCoaching(@Body Map<String, Object> payload);

    @Multipart
    @POST("hsaa/docs/save")
    Call<ApiResponse<Object>> saveHsaaDoc(
            @Part("doc") okhttp3.RequestBody doc,
            @Part okhttp3.MultipartBody.Part file
    );

    // --- 영업목표 관리 (MHSAA600U) ---
    @GET("hsaa/targets")
    Call<ApiResponse<List<Map<String, Object>>>> getSalesTargetList(@Query("yyyy") String yyyy, @Query("userid") String userid);

    @POST("hsaa/targets/save")
    Call<ApiResponse<Object>> saveSalesTargets(@Body Map<String, Object> payload);

    // --- 방문계획 자동수립 (MHSAA700U) ---
    @GET("hsaa/visit-plan")
    Call<ApiResponse<List<Map<String, Object>>>> getVisitPlan(@Query("ym") String ym, @Query("userid") String userid);

    // --- 자동 주간 영업활동 보고서 (MHSAA500U) ---
    @GET("hsaa/weekly-report")
    Call<ApiResponse<Map<String, Object>>> getWeeklyReport(@Query("weekid") String weekid, @Query("userid") String userid);

    @POST("hsaa/weekly-report/auto-generate")
    Call<ApiResponse<Map<String, Object>>> autoGenerateWeeklyReport(@Body Map<String, Object> payload);

    @POST("hsaa/weekly-report/save")
    Call<ApiResponse<Object>> saveWeeklyReport(@Body Map<String, Object> payload);

    // --- 회의록 관리 (HMET100U) ---
    @GET("hmet/master")
    Call<List<Map<String, Object>>> getHmetMaster(@QueryMap Map<String, Object> params);

    @GET("hmet/detail")
    Call<List<Map<String, Object>>> getHmetDetail(@QueryMap Map<String, Object> params);

    @GET("hmet/files")
    Call<List<Map<String, Object>>> getHmetFiles(@QueryMap Map<String, Object> params);

    @POST("hmet/save")
    Call<String> saveHmet(@Body Map<String, Object> payload);

    @POST("hmet/delete")
    Call<Object> deleteHmet(@Body Map<String, Object> params);

    @POST("hmet/stt-simulate")
    Call<Map<String, Object>> sttSimulate(@Body Map<String, Object> payload);

    // --- 견적서 등록 (MHSQT_100U) ---
    @GET("hsqt/master")
    Call<List<Map<String, Object>>> getHsqtMaster(@QueryMap Map<String, Object> params);

    @GET("hsqt/detail")
    Call<List<Map<String, Object>>> getHsqtDetail(@QueryMap Map<String, Object> params);

    @GET("hsqt/status")
    Call<List<Map<String, Object>>> getHsqtStatus(@QueryMap Map<String, Object> params);

    @POST("hsqt/save")
    Call<Object> saveHsqt(@Body Map<String, Object> payload);

    @POST("hsqt/delete")
    Call<Object> deleteHsqt(@Body Map<String, Object> params);
}

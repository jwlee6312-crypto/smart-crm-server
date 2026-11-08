package com.crmbank.erp.mobile;

import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
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

    @POST("hsio/{procedure}")
    Call<List<Map<String, Object>>> executeHsioProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hsod/{procedure}")
    Call<List<Map<String, Object>>> executeHsodProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hs00/{procedure}")
    Call<List<Map<String, Object>>> executeHs00Procedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hsst/{procedure}")
    Call<List<Map<String, Object>>> executeHsstProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hsip/{procedure}")
    Call<List<Map<String, Object>>> executeHsipProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hpio/{procedure}")
    Call<List<Map<String, Object>>> executeHpioProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hppl/{procedure}")
    Call<List<Map<String, Object>>> executeHpplProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hpba/{procedure}")
    Call<List<Map<String, Object>>> executeHpbaProcedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @POST("hp00/{procedure}")
    Call<List<Map<String, Object>>> executeHp00Procedure(@Path("procedure") String procedure, @Body Map<String, Object> params);

    @GET("product/pdplan/request-list")
    Call<List<Map<String, Object>>> getPdRequestList(@QueryMap Map<String, Object> params);

    @GET("product/pdplan/targetlist")
    Call<List<Map<String, Object>>> getPdPlanTargetList(@QueryMap Map<String, Object> params);

    @GET("product/pdplan/list")
    Call<List<Map<String, Object>>> getPdPlanList(@QueryMap Map<String, Object> params);

    @GET("product/insp-req/target-list")
    Call<List<Map<String, Object>>> getPdInspReqTargetList(@QueryMap Map<String, Object> params);

    @POST("hsio/HSIO_052U_SAVE")
    Call<ApiResponse<Map<String, Object>>> saveHsio052U(@Body Map<String, Object> payload);

    @POST("hsio/HSIO_010U_SAVE")
    Call<ApiResponse<Map<String, Object>>> saveHsio010U(@Body Map<String, Object> payload);

    @POST("hsio/HSIO_060U_SAVE")
    Call<ApiResponse<Map<String, Object>>> saveHsio060U(@Body Map<String, Object> payload);

    @POST("hsio/HSIO_190U_SAVE")
    Call<ApiResponse<Map<String, Object>>> saveHsio190U(@Body Map<String, Object> payload);

    @POST("hsio/HSIO_300U_SAVE")
    Call<ApiResponse<Map<String, Object>>> saveHsio300U(@Body Map<String, Object> payload);

    @POST("hsio/HSIO_510U_SAVE")
    Call<ApiResponse<Map<String, Object>>> saveHsio510U(@Body Map<String, Object> payload);

    @POST("hsio/HSIO_580U_SAVE")
    Call<ApiResponse<Map<String, Object>>> saveHsio580U(@Body Map<String, Object> payload);

    @POST("hsod/HSOD_100U_SAVE")
    Call<ApiResponse<Map<String, Object>>> saveHsod100U(@Body Map<String, Object> payload);

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

    @POST("purchase-order/save")
    Call<ApiResponse<String>> savePurchaseOrder(@Body PurchOrderRequest request);

    @DELETE("purchase-order/delete")
    Call<ApiResponse<Void>> deletePurchaseOrder(@Query("balno") String balno, @Query("balgb") String balgb);

    @POST("sales-order/save")
    Call<ApiResponse<String>> saveSalesOrderRequest(@Body SalesOrderRequest request);

    @POST("sales-order/deleteAll")
    Call<ApiResponse<String>> deleteSalesOrderProc(@Query("ordno") String ordno);

    @POST("sales-order-out/save")
    Call<ApiResponse<String>> saveOrderOut(@Body InoutRequest inoutRequest);

    @GET("purch-order-in/request")
    Call<ApiResponse<List<Map<String, Object>>>> getInboundStatus(@QueryMap Map<String, String> params);

    @GET("purch-order-in/mobile-mst")
    Call<ApiResponse<Map<String, Object>>> getPurchaseOrderInSearchMst(@Query("balno") String balno);

    @GET("purch-order-in/mobile-dtl")
    Call<ApiResponse<List<Map<String, Object>>>> getPurchaseOrderInSearchDtl(@Query("balno") String balno);

    @POST("purch-order-in/receive")
    Call<ApiResponse<String>> receivePurchOrderIn(@Body InoutRequest inoutRequest);

    @GET("purch-order-in-confirm/search-left")
    Call<ApiResponse<List<Map<String, Object>>>> getInboundConfirmList(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("schcfmyn") String schcfmyn
    );

    @POST("purch-order-in-confirm/confirm")
    Call<ApiResponse<String>> confirmInbound(@Body Map<String, Object> item);

    // ?낃퀬 痍⑥냼 ???議고쉶
    @GET("purch-order-in-cancel/mobile-cancel-list")
    Call<ApiResponse<List<Map<String, Object>>>> getInboundCancelList(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm
    );

    // ?낃퀬 痍⑥냼 ?ㅽ뻾
    @POST("purch-order-in-cancel/cancel")
    Call<ApiResponse<String>> cancelInbound(@Body List<Map<String, Object>> payload);

    // 異쒓퀬 痍⑥냼 ???議고쉶
    @GET("sales-order-out-cancel/mobile-cancel-list")
    Call<ApiResponse<List<Map<String, Object>>>> getOutboundCancelList(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm
    );

    // 異쒓퀬 痍⑥냼 ?ㅽ뻾
    @POST("sales-order-out-cancel/delete")
    Call<ApiResponse<String>> deleteOutboundCancel(@Body Map<String, Object> payload);

    @GET("sales-order-out-confirm/search-left")
    Call<ApiResponse<List<Map<String, Object>>>> getOutboundConfirmList(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("schcfmyn") String schcfmyn
    );

    @POST("sales-order-out-confirm/save")
    Call<ApiResponse<String>> confirmOutbound(@Body Map<String, Object> item);

    @GET("calc-out/customers")
    Call<List<Map<String, Object>>> getSettleCustomers(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm
    );

    @GET("calc-out/details")
    Call<List<Map<String, Object>>> getSettleDetails(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custcd") String custcd
    );

    @POST("calc-out/save")
    Call<ApiResponse<String>> saveSalesSettle(@Body CalcRequest request);

    @GET("calc-out-cancel/mobile-cancel-mst")
    Call<Map<String, Object>> getSettleCancelMst(
            @Query("cmpycd") String cmpycd,
            @Query("nacd") String nacd,
            @Query("jsanno") String jsanno
    );

    @GET("calc-out-cancel/mobile-cancel-dtl")
    Call<List<Map<String, Object>>> getSettleCancelDtl(
            @Query("cmpycd") String cmpycd,
            @Query("nacd") String nacd,
            @Query("jsanno") String jsanno
    );

    @GET("calc-out-cancel/search-settle-list")
    Call<List<Map<String, Object>>> getSettleSearchList(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm
    );

    @FormUrlEncoded
    @POST("calc-out-cancel/mobile-cancel")
    Call<Map<String, Object>> cancelSalesSettle(@Field("jsanno") String jsanno);

    @GET("calc-in/customers")
    Call<List<Map<String, Object>>> getPurchaseSettleCustomers(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm
    );

    @GET("calc-in/details")
    Call<List<Map<String, Object>>> getPurchaseSettleDetails(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custcd") String custcd
    );

    @POST("calc-in/save")
    Call<ApiResponse<String>> savePurchaseSettle(@Body CalcRequest request);

    @GET("calc-in-cancel/mobile-cancel-mst")
    Call<Map<String, Object>> getPurchaseSettleCancelMst(
            @Query("cmpycd") String cmpycd,
            @Query("nacd") String nacd,
            @Query("jsanno") String jsanno
    );

    @GET("calc-in-cancel/mobile-cancel-dtl")
    Call<List<Map<String, Object>>> getPurchaseSettleCancelDtl(
            @Query("cmpycd") String cmpycd,
            @Query("nacd") String nacd,
            @Query("jsanno") String jsanno
    );

    @GET("calc-in-cancel/search-settle-list")
    Call<List<Map<String, Object>>> getPurchaseSettleSearchList(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm
    );

    @GET("sales-order-out/mobile-order-out-mst")
    Call<ApiResponse<Map<String, Object>>> getSalesOrderOutSearchMst(@Query("ordno") String ordno);

    @GET("sales-order-out/mobile-order-out-dtl")
    Call<ApiResponse<List<Map<String, Object>>>> getSalesOrderOutSearchDtl(@Query("ordno") String ordno);

    @GET("sales-order-out/period-out-list")
    Call<ApiResponse<List<Map<String, Object>>>> getOutboundStatus(@QueryMap Map<String, String> params);

    @GET("purchase-order/balno")
    Call<PurchOrderRequest> getPurchaseOrder(@Query("balno") String balno);

    @GET("purchase-order/search-left")
    Call<ApiResponse<List<PurchOrderMstDto>>> getPurchaseOrderLeftSearch(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm,
            @Query("balgb") String balgb
    );

    @GET("purch-order-in/mobile-balno-list")
    Call<ApiResponse<List<Map<String, Object>>>> getPurchaseOrderInMobileList(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm
    );

    @GET("sales-order/search-left")
    Call<ApiResponse<List<SalesOrderMstDto>>> salesOrderSearchLeft(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm
    );

    @GET("sales-order-out/search-left")
    Call<ApiResponse<List<SalesOrderMstDto>>> getSalesOrderOutPopupList(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm
    );

    @GET("sales-order/search-right")
    Call<ApiResponse<SalesOrderRequest>> searchSalesOrderRequest(@Query("ordno") String ordno);

    @GET("deposit/search-left")
    Call<ApiResponse<List<Depo_MstDto>>> getDepositSearchLeft(
            @Query("fromdt") String fromdt,
            @Query("todt") String todt,
            @Query("custnm") String custnm
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

    @GET("purchase-order/request")
    Call<ApiResponse<List<Map<String, Object>>>> getPurchaseOrderStatus(@QueryMap Map<String, String> params);

    @POST("hsio/HSIO_080S_STR")
    Call<List<Map<String, Object>>> getPurchaseOrderStatusList(@Body Map<String, Object> params);

    @GET("calc-in/list")
    Call<ApiResponse<List<Map<String, Object>>>> getPurchaseUnsettledStatus(@QueryMap Map<String, String> params);

    @GET("sales-order/order-list")
    Call<ApiResponse<List<Map<String, Object>>>> getSalesOrderStatus(@QueryMap Map<String, String> params);

    @GET("report/deposit")
    Call<ApiResponse<List<Map<String, Object>>>> getDepositStatus(@QueryMap Map<String, String> params);

    @GET("report/cust-sales-depo")
    Call<ApiResponse<List<Map<String, Object>>>> getDailySalesReport(@QueryMap Map<String, String> params);

    @GET("report/getSalesLedgerMst")
    Call<ApiResponse<List<Map<String, Object>>>> getSalesLedger(@QueryMap Map<String, String> params);

    @GET("report/getSalesLedgerDtl")
    Call<ApiResponse<List<Map<String, Object>>>> getSalesLedgerDtl(@QueryMap Map<String, String> params);

    @GET("calc-out/uncalc-list")
    Call<List<Map<String, Object>>> getSalesUnsettledStatus(@QueryMap Map<String, String> params);

    @GET("inventory/wh_stock_list")
    Call<List<Map<String, Object>>> getInventoryProductStatus(@QueryMap Map<String, String> params);

    @GET("inventory/wh_io_list")
    Call<List<Map<String, Object>>> getInventoryHistory(@QueryMap Map<String, String> params);

    @POST("inout-move/save")
    Call<ApiResponse<String>> saveMoveOut(@Body InoutMoveRequest request);

    @GET("popup/pop-acctno")
    Call<List<Map<String, Object>>> searchAccounts(@Query("sch_acctno") String schAcctNo);

    @GET("deposit/mobile-depo-mst")
    Call<ApiResponse<Depo_MstDto>> getPaymentMst(@Query("imno") String imno);

    @GET("deposit/mobile-depo-dtl")
    Call<ApiResponse<List<Depo_DtlDto>>> getPaymentDtl(@Query("imno") String imno);

    @POST("deposit/save")
    Call<ApiResponse<String>> savePayment(@Body DepoRequest request);

    @FormUrlEncoded
    @POST("deposit/deleteAll")
    Call<ApiResponse<String>> deletePayment(@Field("imno") String imno);

    @GET("sales-order-out/search-top")
    Call<ApiResponse<List<Map<String, Object>>>> getTransactionStatement(@QueryMap Map<String, String> params);

    @POST("email/send-statement")
    Call<ApiResponse<String>> sendStatementEmail(@Body List<Map<String, Object>> payload);

    @POST("email/send-bal")
    Call<ApiResponse<String>> sendPurchaseOrderEmail(@Body Map<String, Object> payload);

    @POST("cust")
    Call<ApiResponse<String>> saveCust(@Body CustDto custDto);

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

    // --- HSIO104T_TBL 바코드 스캔 이력 ---
    @POST("hsio/HSIO_104U_SAVE")
    Call<List<Map<String, Object>>> saveBarcodeScanHistory(@Body Map<String, Object> payload);

    @POST("hsio/HSIO_104U_STR")
    Call<List<Map<String, Object>>> getBarcodeScanHistory(@Body Map<String, Object> payload);

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

    // --- 영업목표 관리 (MHSAA600U) ---
    @GET("hsaa/targets")
    Call<ApiResponse<List<Map<String, Object>>>> getSalesTargetList(@Query("yyyy") String yyyy, @Query("userid") String userid);

    @POST("hsaa/targets/save")
    Call<ApiResponse<Object>> saveSalesTargets(@Body Map<String, Object> payload);

    // --- 주간 보고 및 방문 계획 (MHSAA500U, MHSAA700U) ---
    @GET("hsaa/weekly-report")
    Call<ApiResponse<Map<String, Object>>> getWeeklyReport(@Query("weekid") String weekid, @Query("userid") String userid);

    @POST("hsaa/weekly-report/auto")
    Call<ApiResponse<Map<String, Object>>> autoGenerateWeeklyReport(@Body Map<String, Object> payload);

    @POST("hsaa/weekly-report/save")
    Call<ApiResponse<Object>> saveWeeklyReport(@Body Map<String, Object> payload);

    @GET("hsaa/visit-plan")
    Call<ApiResponse<List<Map<String, Object>>>> getVisitPlan(@Query("ym") String ym, @Query("userid") String userid);

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
}

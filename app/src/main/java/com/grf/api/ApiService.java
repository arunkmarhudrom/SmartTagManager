package com.grf.api;

import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.QueryMap;
import retrofit2.http.Url;

/**
 * ApiService — all write methods accept a raw RequestBody (application/json).
 * Build the RequestBody once in ApiHelper.toJson() and pass it here.
 */
public interface ApiService {

    // ── GET ──────────────────────────────────────────────────────────────────

    @GET
    Call<ResponseBody> getRequest(@Url String endpoint);

    @GET
    Call<ResponseBody> getRequestWithParams(
            @Url String endpoint,
            @QueryMap Map<String, String> params);

    // ── POST ─────────────────────────────────────────────────────────────────

    /** Body MUST be application/json — use ApiHelper.post() which calls toJson() */
    @POST
    Call<ResponseBody> postRequest(
            @Url String endpoint,
            @Body RequestBody body);

    // ── PUT ──────────────────────────────────────────────────────────────────

    @PUT
    Call<ResponseBody> putRequest(
            @Url String endpoint,
            @Body RequestBody body);

    // ── PATCH ────────────────────────────────────────────────────────────────

    @PATCH
    Call<ResponseBody> patchRequest(
            @Url String endpoint,
            @Body RequestBody body);

    // ── DELETE ───────────────────────────────────────────────────────────────

    @DELETE
    Call<ResponseBody> deleteRequest(@Url String endpoint);

    // ── FILE UPLOAD ──────────────────────────────────────────────────────────

    @Multipart
    @POST
    Call<ResponseBody> uploadFile(
            @Url String endpoint,
            @Part MultipartBody.Part file,
            @Part("description") RequestBody description);
}
package com.resumetailor.app.data.api

import com.resumetailor.app.data.models.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("/")
    suspend fun healthCheck(): Response<HealthResponse>

    @GET("/status/{device_id}")
    suspend fun getUserStatus(
        @Path("device_id") deviceId: String
    ): Response<UserStatus>

    @Multipart
    @POST("/tailor")
    suspend fun tailorWithFile(
        @Part file: MultipartBody.Part,
        @Part("job_description") jobDescription: RequestBody,
        @Part("device_id") deviceId: RequestBody,
        @Part("location") location: RequestBody,
    ): Response<TailorResponse>

    @FormUrlEncoded
    @POST("/tailor")
    suspend fun tailorWithText(
        @Field("resume_text") resumeText: String,
        @Field("job_description") jobDescription: String,
        @Field("device_id") deviceId: String,
        @Field("location") location: String = "",
    ): Response<TailorResponse>

    @FormUrlEncoded
    @POST("/cover-letter")
    suspend fun generateCoverLetter(
        @Field("resume_text") resumeText: String,
        @Field("job_description") jobDescription: String,
        @Field("device_id") deviceId: String,
        @Field("tone") tone: String = "professional",
        @Field("location") location: String = "",
    ): Response<CoverLetterResponse>

    @FormUrlEncoded
    @POST("/rewrite-bullet")
    suspend fun rewriteBullet(
        @Field("bullet") bullet: String,
        @Field("job_description") jobDescription: String,
        @Field("device_id") deviceId: String,
    ): Response<RewriteResponse>

    @Multipart
    @POST("/quick-match")
    suspend fun quickMatchWithFile(
        @Part file: MultipartBody.Part,
        @Part("job_description") jobDescription: RequestBody,
    ): Response<QuickMatchResponse>

    @FormUrlEncoded
    @POST("/quick-match")
    suspend fun quickMatchWithText(
        @Field("job_description") jobDescription: String,
        @Field("resume_text") resumeText: String,
    ): Response<QuickMatchResponse>

    @FormUrlEncoded
    @POST("/generate-docx")
    suspend fun generateDocx(
        @Field("device_id")              deviceId: String,
        @Field("bullets")                bullets: String,
        @Field("cover_letter")           coverLetter: String,
        @Field("template")               template: String = "professional",
        @Field("full_name")              fullName: String = "",
        @Field("current_title")          currentTitle: String = "",
        @Field("location")               location: String = "",
        @Field("education")              education: String = "",
        @Field("skills")                 skills: String = "[]",
        @Field("target_role")            targetRole: String = "",
        @Field("professional_summary")   professionalSummary: String = "",
        @Field("experience")             experience: String = "[]",
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("/upgrade")
    suspend fun upgradeToPro(
        @Header("X-Upgrade-Secret") secret: String,
        @Field("device_id") deviceId: String,
    ): Response<UpgradeResponse>

    @FormUrlEncoded
    @POST("/verify-purchase")
    suspend fun verifyPurchase(
        @Field("device_id")      deviceId: String,
        @Field("purchase_token") purchaseToken: String,
        @Field("product_id")     productId: String,
    ): Response<UpgradeResponse>

    @GET("/history/{device_id}")
    suspend fun getHistory(
        @Path("device_id") deviceId: String
    ): Response<List<HistoryItem>>
}

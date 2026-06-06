package com.eslamielectric.android.core.network

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Customer-facing API — see cursor-my-web-app/docs/mobile-api.md
 */
interface ApiService {

    @GET("api/categories")
    suspend fun getCategories(): List<CategoryDto>

    @GET("api/products")
    suspend fun getProducts(): List<ProductDto>

    @GET("api/locale-hint")
    suspend fun getLocaleHint(): LocaleHintResponse

    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @POST("api/users")
    suspend fun signup(@Body body: SignupRequest): SignupResponse

    @POST("api/auth/token")
    suspend fun exchangeAuthToken(@Body body: AuthTokenRequest): AuthTokenResponse

    @POST("api/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): ForgotPasswordResponse

    @POST("api/reset-password")
    suspend fun resetPassword(@Body body: ResetPasswordRequest): ResetPasswordResponse

    @GET("api/claim-account/{token}")
    suspend fun validateClaimAccount(@Path("token") token: String): ClaimAccountValidateResponse

    @POST("api/claim-account")
    suspend fun claimAccount(@Body body: ClaimAccountRequest): ClaimAccountResponse

    @GET("api/me")
    suspend fun getMe(): ProfileDto

    @PATCH("api/me")
    suspend fun patchMe(@Body body: ProfilePatchRequest): ProfileDto

    @POST("api/create-checkout-session")
    suspend fun createCheckoutSession(
        @Header("Authorization") authorization: String?,
        @Body body: CreateCheckoutSessionRequest
    ): CheckoutSessionResponse

    @GET("api/orders")
    suspend fun getOrders(@Header("Authorization") authorization: String): List<OrderDto>

    @GET("api/orders/{orderId}/basket-draft")
    suspend fun getBasketDraft(
        @Header("Authorization") authorization: String,
        @Path("orderId") orderId: String
    ): BasketDraftResponse

    @POST("api/orders/{orderId}/resume-checkout")
    suspend fun resumeCheckout(
        @Header("Authorization") authorization: String,
        @Path("orderId") orderId: String,
        @Body body: ResumeCheckoutRequest = ResumeCheckoutRequest()
    ): ResumeCheckoutResponse

    @POST("api/orders/{orderId}/cancel")
    suspend fun cancelOrder(
        @Header("Authorization") authorization: String,
        @Path("orderId") orderId: String
    ): OkResponse

    @GET("api/orders/guest/{token}")
    suspend fun getGuestOrder(@Path("token") token: String): OrderDto

    @GET("api/orders/guest-lookup")
    suspend fun guestOrderLookup(
        @Query("email") email: String,
        @Query("order_id") orderId: String
    ): OrderDto

    @POST("api/orders/guest-cancel")
    suspend fun guestCancel(@Body body: GuestOrderTokenRequest): OkResponse

    @POST("api/orders/guest-resume-checkout")
    suspend fun guestResumeCheckout(@Body body: GuestOrderTokenRequest): ResumeCheckoutResponse

    @GET("api/orders/by-session/{sessionId}")
    suspend fun getOrderBySession(@Path("sessionId") sessionId: String): OrderDto

    @POST("api/orders/confirm-by-session/{sessionId}")
    suspend fun confirmOrderBySession(
        @Path("sessionId") sessionId: String,
        @Body body: EmptyRequest = EmptyRequest
    ): ConfirmBySessionResponse

    // --- Push notifications (Firebase Cloud Messaging) ---
    @POST("api/me/push-tokens")
    suspend fun registerPushToken(@Body body: PushTokenRegisterRequest): PushTokenRegisterResponse

    @retrofit2.http.HTTP(method = "DELETE", path = "api/me/push-tokens", hasBody = true)
    suspend fun deletePushToken(@Body body: PushTokenDeleteRequest): OkResponse

    @GET("api/me/push-preferences")
    suspend fun getPushPreferences(): PushPreferencesDto

    @PATCH("api/me/push-preferences")
    suspend fun updatePushPreferences(@Body body: PushPreferencesPatchRequest): PushPreferencesDto

    // --- Basket activity sync (v2 abandoned-basket reminders) ---
    @PUT("api/me/basket-activity")
    suspend fun syncBasketActivity(@Body body: BasketActivityRequest): BasketActivityResponse

    @PUT("api/basket-activity")
    suspend fun syncGuestBasketActivity(
        @Header("X-Basket-Session") sessionId: String,
        @Body body: BasketActivityRequest
    ): BasketActivityResponse
}

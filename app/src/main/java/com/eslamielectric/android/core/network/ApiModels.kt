package com.eslamielectric.android.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class ApiErrorResponse(
    val error: String,
    val code: String? = null,
    val missing: List<String>? = null,
    val lockedUntil: String? = null
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LoginResponse(val ok: Boolean, val token: String, val user: AuthUserDto)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String? = null,
    val firstName: String? = null,
    val surname: String? = null
)

@Serializable
data class AuthTokenRequest(@SerialName("accessToken") val accessToken: String)

@Serializable
data class AuthTokenResponse(val ok: Boolean, val token: String, val user: AuthUserDto)

@Serializable
data class SignupRequest(
    val type: String,
    val firstName: String,
    val surname: String,
    val dob: String? = null,
    val mobile: String,
    val landline: String? = null,
    val email: String,
    val bankDetails: String? = null,
    val address: String,
    val companyName: String? = null,
    val companyNumber: String? = null,
    val companyContactNumber: String? = null,
    val companyPrincipalContact: String? = null,
    val password: String
)

@Serializable
data class SignupResponse(val ok: Boolean, val userId: String, val token: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class ForgotPasswordResponse(val ok: Boolean, val message: String)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String,
    val confirmPassword: String? = null
)

@Serializable
data class ResetPasswordResponse(val ok: Boolean, val message: String)

@Serializable
data class CategoryDto(
    val id: String,
    val name: String,
    @SerialName("name_fa") val nameFa: String = "",
    val products: List<ProductDto> = emptyList()
)

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    @SerialName("name_fa") val nameFa: String = "",
    val price: Double = 0.0,
    @SerialName("image_url") val imageUrl: String = "",
    val description: String = "",
    @SerialName("description_fa") val descriptionFa: String = "",
    @SerialName("image_alt_en") val imageAltEn: String = "",
    @SerialName("image_alt_fa") val imageAltFa: String = "",
    val category: String? = null,
    @SerialName("category_fa") val categoryFa: String? = null,
    @SerialName("categoryId") val categoryId: String? = null
)

@Serializable
data class ProfileDto(
    val id: String,
    val type: String,
    val firstName: String? = null,
    val surname: String? = null,
    val dob: String? = null,
    val mobile: String? = null,
    val landline: String? = null,
    val email: String? = null,
    val contactEmail: String? = null,
    val address: String? = null,
    val bankDetails: String? = null,
    val companyName: String? = null,
    val companyNumber: String? = null,
    val companyContactNumber: String? = null,
    val companyPrincipalContact: String? = null,
    val checkoutProfileComplete: Boolean? = null,
    val checkoutProfileMissing: List<String>? = null,
    val checkoutProfileRequired: Boolean? = null
)

@Serializable
data class ProfilePatchRequest(
    val firstName: String? = null,
    val surname: String? = null,
    val dob: String? = null,
    val mobile: String? = null,
    val landline: String? = null,
    val contactEmail: String? = null,
    val address: String? = null,
    val bankDetails: String? = null,
    val companyName: String? = null,
    val companyNumber: String? = null,
    val companyContactNumber: String? = null,
    val companyPrincipalContact: String? = null
)

@Serializable
data class CheckoutLineItemRequest(
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val productId: String? = null,
    val id: String? = null
)

@Serializable
data class ShippingAddressRequest(
    val line1: String,
    val city: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val line2: String? = null,
    val state: String? = null,
    val country: String? = null,
    @SerialName("additional_info") val additionalInfo: String? = null
)

@Serializable
data class CreateCheckoutSessionRequest(
    val lineItems: List<CheckoutLineItemRequest>? = null,
    val guestEmail: String? = null,
    val guestName: String? = null,
    val guestPhone: String? = null,
    val shippingAddress: ShippingAddressRequest? = null,
    val locale: String? = null,
    val fulfillmentType: String? = null,
    val pendingOrderId: String? = null
)

@Serializable
data class CheckoutSessionResponse(val url: String, val sessionId: String)

@Serializable
data class OrderDto(
    val id: String,
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("stripe_session_id") val stripeSessionId: String? = null,
    @SerialName("amount_total") val amountTotal: Int? = null,
    val currency: String? = null,
    val status: String? = null,
    @SerialName("line_items") val lineItems: List<OrderLineItemDto>? = null,
    @SerialName("tracking_number") val trackingNumber: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("fulfillment_type") val fulfillmentType: String? = null,
    @SerialName("shipping_address") val shippingAddress: JsonObject? = null,
    @SerialName("customer_email") val customerEmail: String? = null,
    @SerialName("customer_name") val customerName: String? = null,
    @SerialName("guest_access_token") val guestAccessToken: String? = null
)

@Serializable
data class OrderLineItemDto(
    val name: String? = null,
    val quantity: Int? = null,
    @SerialName("unit_amount") val unitAmount: Int? = null,
    @SerialName("amount_total") val amountTotal: Int? = null,
    @SerialName("product_id") val productId: String? = null
)

@Serializable
data class GuestOrderTokenRequest(val token: String, val locale: String? = null)

@Serializable
data class ResumeCheckoutRequest(val locale: String? = null)

@Serializable
data class ResumeCheckoutResponse(val url: String, val recreated: Boolean? = null)

@Serializable
data class BasketDraftResponse(
    val orderId: String,
    val orderNumber: String? = null,
    val basket: List<BasketDraftItemDto> = emptyList(),
    val fulfillmentType: String? = null,
    val shippingAddress: JsonObject? = null
)

@Serializable
data class BasketDraftItemDto(
    val id: String? = null,
    val name: String,
    @SerialName("name_fa") val nameFa: String? = null,
    val price: Double = 0.0,
    val quantity: Int = 1,
    @SerialName("image_url") val imageUrl: String? = null,
    val categoryId: String? = null
)

@Serializable
data class LocaleHintResponse(
    val country: String,
    val inIran: Boolean,
    val defaultLang: String,
    val defaultCurrency: String,
    val usdToToman: Int
)

@Serializable
object EmptyRequest

@Serializable
data class OkResponse(val ok: Boolean)

@Serializable
data class ConfirmBySessionResponse(
    val updated: Boolean? = null,
    val status: String? = null
)

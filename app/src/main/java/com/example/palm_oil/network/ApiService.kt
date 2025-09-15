package com.example.palm_oil.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class FormRequest(
    val treeId: String,
    val plotId: String?,
    val numberOfFruits: Int?,
    val harvestDays: Int?
)

data class FormResponse(
    val formId: Int
)

data class ImageRequest(
    val url: String,
    val filename: String?,
    val timestamp: Long? = null
)

data class ImageListRequest(
    val images: List<ImageRequest>
)

data class ImageListResponse(
    val processed: Int,
    val errors: List<Map<String, Any>>
)

interface ApiService {
    @POST("api/forms")
    suspend fun createForm(
        @Header("X-API-Key") apiKey: String,
        @Body request: FormRequest
    ): Response<FormResponse>

    @POST("api/image-list")
    suspend fun uploadImageList(
        @Header("X-API-Key") apiKey: String,
        @Body request: ImageListRequest
    ): Response<ImageListResponse>
}
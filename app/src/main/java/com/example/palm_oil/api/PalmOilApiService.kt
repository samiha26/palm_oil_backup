package com.example.palm_oil.api

import retrofit2.Response
import retrofit2.http.*

// Data models for API requests/responses
data class ReconFormRequest(
    val treeId: String,
    val plotId: String?,
    val numberOfFruits: Int?,
    val harvestDays: Int?,
    val clientId: String?
)

data class ApiResponse(
    val formId: Long? = null,
    val status: String,
    val timestamp: Long,
    val error: String? = null
)

data class ImageItem(
    val url: String,
    val filename: String?,
    val timestamp: Long?,
    val checksum: String?
)

data class ImageListRequest(
    val images: List<ImageItem>
)

data class ImageUploadResponse(
    val processed: Int,
    val errors: List<Map<String, Any>>,
    val associations: List<Map<String, Any>>
)

// Retrofit API interface
interface PalmOilApiService {
    
    @POST("api/forms")
    suspend fun createReconForm(
        @Header("X-API-Key") apiKey: String,
        @Body form: ReconFormRequest
    ): Response<ApiResponse>
    
    @POST("api/image-list")
    suspend fun uploadImageList(
        @Header("X-API-Key") apiKey: String,
        @Body imageList: ImageListRequest
    ): Response<ImageUploadResponse>
    
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
}

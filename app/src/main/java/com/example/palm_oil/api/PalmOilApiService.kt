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

data class PlotItem(
    val id: String
)

data class PlotsResponse(
    val plots: List<PlotItem>,
    val total: Int
)

data class HarvesterProofRequest(
    val treeId: String,
    val plotId: String,
    val imageUrl: String?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Double?,
    val notes: String?,
    val harvesterId: String?,
    val clientId: String?
)

data class TreeLocationRequest(
    val treeId: String,
    val plotId: String,
    val xCoordinate: Double?,
    val yCoordinate: Double?,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String?,
    val clientId: String?
)

data class TreeLocationResponse(
    val id: Long,
    val tree_id: String,
    val plot_id: String,
    val x_coordinate: Double,
    val y_coordinate: Double,
    val latitude: Double?,
    val longitude: Double?,
    val notes: String?,
    val created_at: Long,
    val updated_at: Long
)

data class TreeLocationsResponse(
    val locations: List<TreeLocationResponse>,
    val total: Int,
    val hasMore: Boolean?
)

data class ReconFormResponse(
    val id: Long,
    val tree_id: String,
    val plot_id: String?,
    val number_of_fruits: Int?,
    val harvest_days: Int?,
    val created_at: Long?,
    val images: List<Map<String, Any>>? = null
)

data class FormsResponse(
    val forms: List<ReconFormResponse>,
    val total: Int,
    val hasMore: Boolean?
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

    @GET("api/plots")
    suspend fun getPlots(
        @Header("X-API-Key") apiKey: String
    ): Response<PlotsResponse>

    @POST("api/harvester-proofs")
    suspend fun createHarvesterProof(
        @Header("X-API-Key") apiKey: String,
        @Body request: HarvesterProofRequest
    ): Response<ApiResponse>

    @POST("api/tree-locations")
    suspend fun createTreeLocation(
        @Header("X-API-Key") apiKey: String,
        @Body request: TreeLocationRequest
    ): Response<ApiResponse>

    @GET("api/tree-locations/{plot_id}")
    suspend fun getTreeLocationsByPlot(
        @Header("X-API-Key") apiKey: String,
        @Path("plot_id") plotId: String
    ): Response<TreeLocationsResponse>
    
    @GET("api/forms/{plot_id}")
    suspend fun getFormsByPlot(
        @Header("X-API-Key") apiKey: String,
        @Path("plot_id") plotId: String,
        @Query("include_images") includeImages: Boolean = false
    ): Response<FormsResponse>
}

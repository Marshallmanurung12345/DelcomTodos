package org.delcom.pam_p5_ifs23021.network.todos.service

import okhttp3.MultipartBody
import org.delcom.pam_p5_ifs23021.network.data.ResponseMessage
import org.delcom.pam_p5_ifs23021.network.todos.data.*
import retrofit2.http.*

interface TodoApiService {

    // ----------------------------------
    // Auth
    // ----------------------------------

    @POST("auth/register")
    suspend fun postRegister(
        @Body request: RequestAuthRegister
    ): ResponseMessage<ResponseAuthRegister?>

    @POST("auth/login")
    suspend fun postLogin(
        @Body request: RequestAuthLogin
    ): ResponseMessage<ResponseAuthLogin?>

    @POST("auth/logout")
    suspend fun postLogout(
        @Body request: RequestAuthLogout
    ): ResponseMessage<String?>

    @POST("auth/refresh-token")
    suspend fun postRefreshToken(
        @Body request: RequestAuthRefreshToken
    ): ResponseMessage<ResponseAuthLogin?>

    // ----------------------------------
    // Users
    // ----------------------------------

    @GET("users/me")
    suspend fun getUserMe(
        @Header("Authorization") authToken: String
    ): ResponseMessage<ResponseUser?>

    @PUT("users/me")
    suspend fun putUserMe(
        @Header("Authorization") authToken: String,
        @Body request: RequestUserChange
    ): ResponseMessage<String?>

    @PUT("users/me/password")
    suspend fun putUserMePassword(
        @Header("Authorization") authToken: String,
        @Body request: RequestUserChangePassword
    ): ResponseMessage<String?>

    @Multipart
    @PUT("users/me/photo")
    suspend fun putUserMePhoto(
        @Header("Authorization") authToken: String,
        @Part file: MultipartBody.Part
    ): ResponseMessage<String?>

    // ----------------------------------
    // Todos
    // ----------------------------------

    @GET("todos")
    suspend fun getTodos(
        @Header("Authorization") authToken: String,
        @Query("search") search: String? = null,
        @Query("is_done") isDone: Boolean? = null,
        @Query("priority") priority: String? = null,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null
    ): ResponseMessage<ResponseTodos?>

    @POST("todos")
    suspend fun postTodo(
        @Header("Authorization") authToken: String,
        @Body request: RequestTodo
    ): ResponseMessage<ResponseTodoAdd?>

    @GET("todos/{todo_id}")
    suspend fun getTodoById(
        @Header("Authorization") authToken: String,
        @Path("todo_id") todoId: String
    ): ResponseMessage<ResponseTodo?>

    @PUT("todos/{todo_id}")
    suspend fun putTodo(
        @Header("Authorization") authToken: String,
        @Path("todo_id") todoId: String,
        @Body request: RequestTodo
    ): ResponseMessage<String?>

    @Multipart
    @PUT("todos/{todo_id}/cover")
    suspend fun putTodoCover(
        @Header("Authorization") authToken: String,
        @Path("todo_id") todoId: String,
        @Part file: MultipartBody.Part
    ): ResponseMessage<String?>

    @DELETE("todos/{todo_id}")
    suspend fun deleteTodo(
        @Header("Authorization") authToken: String,
        @Path("todo_id") todoId: String
    ): ResponseMessage<String?>
}








package com.example.greenhouse.api;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface CloudinaryApi {

    @Multipart
    @POST("v1_1/{cloudName}/image/upload")
    Call<CloudinaryResponse> uploadImage(
            @Path("cloudName") String cloudName,
            @Part MultipartBody.Part file,
            @Part("upload_preset") RequestBody uploadPreset
    );
}
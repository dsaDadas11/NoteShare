package com.example.noteshare.core.common

import com.example.noteshare.core.network.ApiResponse
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SafeApiCallTest {

    @Test
    fun safeApiCall_httpExceptionWithApiResponseBody_returnsBusinessError() = runTest {
        val result = safeApiCall<String>("网络请求失败") {
            throw httpException(
                statusCode = 400,
                body = """{"code":40000,"message":"username: 用户名只能包含字母、数字、下划线","data":null}"""
            )
        }

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals(ErrorCode.PARAM_INVALID, error.code)
        assertEquals("username: 用户名只能包含字母、数字、下划线", error.message)
    }

    @Test
    fun safeApiCall_httpExceptionWithUnparseableBody_returnsNetworkError() = runTest {
        val result = safeApiCall<String>("网络请求失败") {
            throw httpException(statusCode = 500, body = "Internal Server Error")
        }

        assertTrue(result is Result.Error)
        val error = result as Result.Error
        assertEquals(ErrorCode.NETWORK_ERROR, error.code)
        assertTrue(error.message.contains("网络请求失败"))
    }

    @Test
    fun safeApiCall_successResponse_returnsData() = runTest {
        val result = safeApiCall {
            ApiResponse(code = ErrorCode.SUCCESS, message = "ok", data = "payload")
        }

        assertTrue(result is Result.Success)
        assertEquals("payload", (result as Result.Success).data)
    }

    private fun httpException(statusCode: Int, body: String): HttpException {
        val rawResponse = okhttp3.Response.Builder()
            .code(statusCode)
            .message("HTTP $statusCode")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/api/test").build())
            .build()
        val response = Response.error<ApiResponse<String?>>(
            body.toResponseBody("application/json".toMediaType()),
            rawResponse
        )
        return HttpException(response)
    }
}

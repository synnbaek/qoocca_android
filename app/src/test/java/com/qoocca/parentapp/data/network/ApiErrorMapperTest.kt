package com.qoocca.parentapp.data.network

import com.qoocca.parentapp.domain.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class ApiErrorMapperTest {

    @Test
    fun map401ToUnauthorized() {
        val error = ApiErrorMapper.from(ApiResult.HttpError(401, null))
        assertEquals(AppError.Unauthorized, error)
    }

    @Test
    fun map403ToForbidden() {
        val error = ApiErrorMapper.from(ApiResult.HttpError(403, null))
        assertEquals(AppError.Forbidden, error)
    }

    @Test
    fun map500ToServer() {
        val error = ApiErrorMapper.from(ApiResult.HttpError(500, null))
        assertTrue(error is AppError.Server)
        assertEquals(500, (error as AppError.Server).code)
    }

    @Test
    fun mapNetworkToNetwork() {
        val error = ApiErrorMapper.from(ApiResult.NetworkError(IOException("offline")))
        assertEquals(AppError.Network, error)
    }
}

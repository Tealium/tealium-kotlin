package com.tealium.visitorservice.momentsapi

import android.app.Application
import com.tealium.core.Environment
import com.tealium.core.TealiumConfig
import com.tealium.core.TealiumContext
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class MomentsApiServiceTest {

    @MockK
    private lateinit var mockApplication: Application

    @MockK
    private lateinit var mockFile: File

    @MockK
    private lateinit var mockContext: TealiumContext

    @MockK
    private lateinit var networkClient: NetworkClient

    private lateinit var apiService: MomentsApiService
    private lateinit var listener: ResponseListener<EngineResponse>
    private lateinit var config: TealiumConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { mockApplication.filesDir } returns mockFile

        config = TealiumConfig(
            mockApplication,
            "test",
            "test",
            Environment.DEV
        ).apply { momentsApiRegion = MomentsApiRegion.US_EAST }

        every { mockContext.config } returns config
        every { mockContext.visitorId } returns "abc123"

        apiService = MomentsApiService(mockContext, networkClient)
        listener = mockk()
    }

    @Test
    fun fetchEngineResponseForSuccessfulNetworkData() {
        val engineId = "testEngine"

        coEvery { networkClient.get(any(), any<ResponseListener<String>>()) } answers {
            secondArg<ResponseListener<String>>().success("{ \"audiences\": [\"VIP\", \"Women's Apparel\", \"Lifetime visit count\"] }")
        }

        apiService.fetchEngineResponse(engineId, listener)

        verify { listener.success(any<EngineResponse>()) }
    }

    @Test
    fun fetchEngineResponseForFailedNetworkData() {
        val engineId = "testEngine"

        coEvery { networkClient.get(any(), any<ResponseListener<String>>()) } answers {
            secondArg<ResponseListener<String>>().failure(ErrorCode.UNKNOWN_ERROR, "Network Error")
        }

        apiService.fetchEngineResponse(engineId, listener)

        verify {
            listener.failure(
                ErrorCode.UNKNOWN_ERROR,
                "Network Error"
            )
        }
    }

    @Test
    fun fetchEngineResponseForInvalidJsonData() {
        val engineId = "testEngine"

        coEvery { networkClient.get(any(), any<ResponseListener<String>>()) } answers {
            secondArg<ResponseListener<String>>().success("invalid_json")
        }

        apiService.fetchEngineResponse(engineId, listener)

        verify {
            listener.failure(
                ErrorCode.INVALID_JSON,
                "Invalid JSON EngineResponse"
            )
        }
    }
}

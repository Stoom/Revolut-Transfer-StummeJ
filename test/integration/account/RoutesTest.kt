package account

import com.google.gson.Gson
import io.ktor.application.Application
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import uk.stumme.models.TransferRequest
import uk.stumme.module
import kotlin.test.Test
import kotlin.test.assertEquals


class RoutesTest {
    val gson = Gson()

    @Test
    fun testPostTransferShouldReturn200() {
        testRequest(
            HttpMethod.Post,
            "/accounts/source/transfer/destination",
            setJsonBody(TransferRequest(100.00))
        ) {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    @Test
    fun testGetTransfersShouldReturn200() {

    }

    @Test
    fun testGetAccountShouldReturn200() {
        testRequest(HttpMethod.Get, "/accounts/source") {
            assertEquals(HttpStatusCode.OK, response.status())
        }
    }

    private fun testRequest(
        method: HttpMethod, uri: String,
        setup: suspend TestApplicationRequest.() -> Unit = {},
        check: suspend TestApplicationCall.() -> Unit
    ) {
        httpBinTest {
            val req = handleRequest(method, uri) { runBlocking { setup() } }
            check(req)
        }
    }

    private fun httpBinTest(callback: suspend TestApplicationEngine.() -> Unit) {
        withTestApplication(Application::module) {
            runBlocking { callback() }
        }
    }


    private fun setJsonBody(value: Any): suspend TestApplicationRequest.() -> Unit {
        return {
            setBody(gson.toJson(value))
            addHeader("Content-Type", "application/json")
            addHeader("Accept", "application/json")
        }
    }
}
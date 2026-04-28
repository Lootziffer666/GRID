package com.painkiller.domain.lfs

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LfsBatchModelsSerializationTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun batchRequest_serializesExpectedShape() {
        val request = LfsBatchRequest(
            operation = "upload",
            objects = listOf(LfsBatchObjectRequest(oid = "abc", size = 10L)),
            ref = LfsBatchRef("refs/heads/main"),
        )

        val encoded = json.encodeToString(LfsBatchRequest.serializer(), request)

        assertEquals(
            "{\"operation\":\"upload\",\"transfers\":[\"basic\"],\"objects\":[{\"oid\":\"abc\",\"size\":10}],\"ref\":{\"name\":\"refs/heads/main\"}}",
            encoded,
        )
    }

    @Test
    fun batchResponse_parsesActionsAndHeaders() {
        val payload = """
            {
              "transfer":"basic",
              "objects":[
                {
                  "oid":"abc",
                  "size":10,
                  "actions":{
                    "upload":{"href":"https://u","header":{"Authorization":"Basic x"}},
                    "verify":{"href":"https://v"}
                  }
                }
              ]
            }
        """.trimIndent()

        val decoded = json.decodeFromString(LfsBatchResponse.serializer(), payload)

        assertEquals("basic", decoded.transfer)
        assertNotNull(decoded.objects.first().actions?.upload)
        assertEquals("https://u", decoded.objects.first().actions?.upload?.href)
    }
}

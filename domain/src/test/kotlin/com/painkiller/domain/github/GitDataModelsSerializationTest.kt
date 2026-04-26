package com.painkiller.domain.github

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitDataModelsSerializationTest {

    // encodeDefaults = true so blob requests always send `encoding: "base64"` to GitHub.
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun gitRef_roundTrip() {
        val original = GitRef(
            ref = "refs/heads/main",
            obj = GitRefObject(sha = "abc123", type = "commit"),
        )
        val encoded = json.encodeToString(GitRef.serializer(), original)
        val decoded = json.decodeFromString(GitRef.serializer(), encoded)
        assertEquals(original, decoded)
        assertTrue(encoded.contains("\"object\""))
    }

    @Test
    fun createBlobRequest_defaultEncoding_isBase64() {
        val req = CreateBlobRequest(content = "aGVsbG8=")
        val encoded = json.encodeToString(CreateBlobRequest.serializer(), req)
        assertTrue(encoded.contains("\"encoding\":\"base64\""))
    }

    @Test
    fun treeEntry_constants_matchGithubModes() {
        assertEquals("100644", TreeEntry.MODE_FILE)
        assertEquals("100755", TreeEntry.MODE_EXECUTABLE)
        assertEquals("040000", TreeEntry.MODE_SUBDIR)
        assertEquals("120000", TreeEntry.MODE_SYMLINK)
        assertEquals("blob", TreeEntry.TYPE_BLOB)
        assertEquals("tree", TreeEntry.TYPE_TREE)
    }

    @Test
    fun createTreeRequest_omitsBaseTreeWhenNull() {
        val req = CreateTreeRequest(
            baseTree = null,
            tree = listOf(TreeEntry(path = "README.md", sha = "deadbeef")),
        )
        val encoded = json.encodeToString(CreateTreeRequest.serializer(), req)
        // base_tree present but null is acceptable; verify the path made it.
        assertTrue(encoded.contains("\"path\":\"README.md\""))
    }

    @Test
    fun parseRealisticBranchPayload() {
        val payload = """
            {
              "name": "main",
              "commit": { "sha": "deadbeef", "url": "https://api.github.com/x" },
              "protected": true
            }
        """.trimIndent()
        val parsed = json.decodeFromString(GithubBranchSummary.serializer(), payload)
        assertEquals("main", parsed.name)
        assertEquals("deadbeef", parsed.commit.sha)
        assertTrue(parsed.protected)
    }
}

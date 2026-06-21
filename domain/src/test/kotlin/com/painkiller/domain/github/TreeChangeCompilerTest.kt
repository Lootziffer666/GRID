package com.painkiller.domain.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TreeChangeCompilerTest {

    private val existingTree = listOf(
        TreeEntry(path = "src/main.kt", mode = TreeEntry.MODE_FILE, type = TreeEntry.TYPE_BLOB, sha = "abc123"),
        TreeEntry(path = "src/util.kt", mode = TreeEntry.MODE_FILE, type = TreeEntry.TYPE_BLOB, sha = "def456"),
        TreeEntry(path = "docs/readme.md", mode = TreeEntry.MODE_FILE, type = TreeEntry.TYPE_BLOB, sha = "ghi789"),
        TreeEntry(path = "lib/helper.kt", mode = TreeEntry.MODE_EXECUTABLE, type = TreeEntry.TYPE_BLOB, sha = "jkl012"),
    )

    // ─── MOVE ─────────────────────────────────────────────────────────────────

    @Test
    fun move_producesDeleteAndAddEntries() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.MOVE,
                sourcePath = "src/main.kt",
                targetPath = "archive/main.kt",
            )
        )

        val result = TreeChangeCompiler.compile(changes, existingTree)

        assertEquals(2, result.size)
        // Delete old path
        assertEquals("src/main.kt", result[0].path)
        assertNull(result[0].sha)
        // Add at new path with original SHA
        assertEquals("archive/main.kt", result[1].path)
        assertEquals("abc123", result[1].sha)
    }

    @Test
    fun move_preservesOriginalFileMode() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.MOVE,
                sourcePath = "lib/helper.kt",
                targetPath = "tools/helper.kt",
            )
        )

        val result = TreeChangeCompiler.compile(changes, existingTree)

        assertEquals(2, result.size)
        // New entry preserves the executable mode
        assertEquals(TreeEntry.MODE_EXECUTABLE, result[1].mode)
        assertEquals("jkl012", result[1].sha)
    }

    // ─── RENAME ───────────────────────────────────────────────────────────────

    @Test
    fun rename_producesDeleteAndAddEntries() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.RENAME,
                sourcePath = "docs/readme.md",
                targetPath = "docs/README.md",
            )
        )

        val result = TreeChangeCompiler.compile(changes, existingTree)

        assertEquals(2, result.size)
        // Delete old
        assertEquals("docs/readme.md", result[0].path)
        assertNull(result[0].sha)
        // Add new
        assertEquals("docs/README.md", result[1].path)
        assertEquals("ghi789", result[1].sha)
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    @Test
    fun delete_producesEntryWithNullSha() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.DELETE,
                sourcePath = "src/util.kt",
                targetPath = null,
            )
        )

        val result = TreeChangeCompiler.compile(changes, existingTree)

        assertEquals(1, result.size)
        assertEquals("src/util.kt", result[0].path)
        assertNull(result[0].sha)
        assertEquals(TreeEntry.MODE_FILE, result[0].mode)
        assertEquals(TreeEntry.TYPE_BLOB, result[0].type)
    }

    // ─── CREATE_FOLDER ────────────────────────────────────────────────────────

    @Test
    fun createFolder_producesGitkeepEntry() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.CREATE_FOLDER,
                sourcePath = null,
                targetPath = "new-folder",
            )
        )

        val result = TreeChangeCompiler.compile(changes, existingTree)

        assertEquals(1, result.size)
        assertEquals("new-folder/.gitkeep", result[0].path)
        assertEquals(TreeEntry.MODE_FILE, result[0].mode)
        assertEquals(TreeEntry.TYPE_BLOB, result[0].type)
        assertNull(result[0].sha)
        assertEquals("", result[0].content)
    }

    // ─── UPLOAD ───────────────────────────────────────────────────────────────

    @Test
    fun upload_withBlobSha_producesEntryWithSha() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.UPLOAD,
                sourcePath = null,
                targetPath = "assets/logo.png",
                blobSha = "existing-blob-sha",
            )
        )

        val result = TreeChangeCompiler.compile(changes, existingTree)

        assertEquals(1, result.size)
        assertEquals("assets/logo.png", result[0].path)
        assertEquals("existing-blob-sha", result[0].sha)
        assertEquals(TreeEntry.MODE_FILE, result[0].mode)
        assertNull(result[0].content)
    }

    @Test
    fun upload_withNewContentBase64_producesEntryWithContent() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.UPLOAD,
                sourcePath = null,
                targetPath = "data/config.json",
                newContentBase64 = "eyJrZXkiOiJ2YWx1ZSJ9",
            )
        )

        val result = TreeChangeCompiler.compile(changes, existingTree)

        assertEquals(1, result.size)
        assertEquals("data/config.json", result[0].path)
        assertNull(result[0].sha)
        assertEquals("eyJrZXkiOiJ2YWx1ZSJ9", result[0].content)
    }

    @Test
    fun upload_withNeitherBlobShaOrContent_throws() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.UPLOAD,
                sourcePath = null,
                targetPath = "data/empty.txt",
                blobSha = null,
                newContentBase64 = null,
            )
        )

        try {
            TreeChangeCompiler.compile(changes, existingTree)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("blobSha or newContentBase64"))
        }
    }

    // ─── path safety ──────────────────────────────────────────────────────────

    @Test
    fun unsafeSourcePath_dotDot_throws() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.DELETE,
                sourcePath = "../etc/passwd",
                targetPath = null,
            )
        )

        try {
            TreeChangeCompiler.compile(changes, existingTree)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unsafe"))
        }
    }

    @Test
    fun unsafeTargetPath_absolutePath_throws() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.UPLOAD,
                sourcePath = null,
                targetPath = "/etc/passwd",
                blobSha = "some-sha",
            )
        )

        try {
            TreeChangeCompiler.compile(changes, existingTree)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unsafe"))
        }
    }

    @Test
    fun unsafeTargetPath_windowsDriveLetter_throws() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.MOVE,
                sourcePath = "src/main.kt",
                targetPath = "C:/windows/system32/evil.exe",
            )
        )

        try {
            TreeChangeCompiler.compile(changes, existingTree)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unsafe"))
        }
    }

    @Test
    fun nullSourcePath_forDelete_throws() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.DELETE,
                sourcePath = null,
                targetPath = null,
            )
        )

        try {
            TreeChangeCompiler.compile(changes, existingTree)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("must not be null"))
        }
    }

    @Test
    fun blankTargetPath_forUpload_throws() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.UPLOAD,
                sourcePath = null,
                targetPath = "  ",
                blobSha = "sha",
            )
        )

        try {
            TreeChangeCompiler.compile(changes, existingTree)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("must not be blank"))
        }
    }

    @Test
    fun move_sourceNotInTree_throws() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.MOVE,
                sourcePath = "nonexistent/file.kt",
                targetPath = "dest/file.kt",
            )
        )

        try {
            TreeChangeCompiler.compile(changes, existingTree)
            fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("not found in existing tree"))
        }
    }

    // ─── multiple changes ─────────────────────────────────────────────────────

    @Test
    fun multipleChanges_allCompiled() {
        val changes = listOf(
            PendingChange(
                type = PendingChangeType.DELETE,
                sourcePath = "src/util.kt",
                targetPath = null,
            ),
            PendingChange(
                type = PendingChangeType.CREATE_FOLDER,
                sourcePath = null,
                targetPath = "new-dir",
            ),
            PendingChange(
                type = PendingChangeType.UPLOAD,
                sourcePath = null,
                targetPath = "uploads/data.bin",
                blobSha = "upload-sha",
            ),
        )

        val result = TreeChangeCompiler.compile(changes, existingTree)

        // DELETE (1) + CREATE_FOLDER (1) + UPLOAD (1) = 3 entries
        assertEquals(3, result.size)
        assertEquals("src/util.kt", result[0].path)
        assertNull(result[0].sha)
        assertEquals("new-dir/.gitkeep", result[1].path)
        assertEquals("uploads/data.bin", result[2].path)
        assertEquals("upload-sha", result[2].sha)
    }
}

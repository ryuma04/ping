package com.inception.android.lantern

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.inception.android.lantern.domain.LanternQueryClassifier
import com.inception.android.lantern.mesh.LanternMeshCoordinator
import com.inception.android.lantern.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LanternTest {

    private val gson = Gson()

    // =========================================================================
    // 1. ASSET MANUALS INTEGRITY & SCHEMA TESTING
    // =========================================================================

    @Test
    fun testAssetManualsIntegrityAndNoDuplicateIds() {
        val assetsDir = File("src/main/assets/lantern")
        assertTrue("Assets directory should exist", assetsDir.exists())

        val jsonFiles = assetsDir.listFiles { _, name -> name.endsWith(".json") }
        assertNotNull("JSON files should be present", jsonFiles)
        assertTrue("At least 3 manual files should exist", jsonFiles!!.size >= 3)

        val seenIds = mutableSetOf<String>()
        var totalChunks = 0
        val listType = object : TypeToken<List<LanternChunk>>() {}.type

        for (file in jsonFiles) {
            val content = file.readText()
            val chunks: List<LanternChunk> = gson.fromJson(content, listType)
            assertTrue("File ${file.name} should contain chunks", chunks.isNotEmpty())

            for (chunk in chunks) {
                totalChunks++
                // ID checks
                assertTrue("Chunk ID must not be blank in ${file.name}", chunk.id.isNotBlank())
                assertFalse("Duplicate ID detected: ${chunk.id}", seenIds.contains(chunk.id))
                seenIds.add(chunk.id)

                // Content checks
                assertTrue("Title must not be blank in ${chunk.id}", chunk.title.isNotBlank())
                assertTrue("Summary must not be blank in ${chunk.id}", chunk.summary.isNotBlank())
                assertTrue("ContentMarkdown must not be blank in ${chunk.id}", chunk.contentMarkdown.isNotBlank())
                assertTrue("Source manual must not be blank in ${chunk.id}", chunk.sourceManual.isNotBlank())
                assertTrue("Steps should not be empty for action guide ${chunk.id}", chunk.steps.isNotEmpty())
                assertTrue("Tags should not be empty for search indexing in ${chunk.id}", chunk.tags.isNotEmpty())

                // Verify category enum validity
                assertNotNull("Category must be valid for ${chunk.id}", chunk.category)
            }
        }

        assertTrue("Should have indexed at least 10 emergency chunks total", totalChunks >= 10)
    }

    // =========================================================================
    // 2. QUERY CLASSIFIER STRESS & EDGE CASE TESTING
    // =========================================================================

    @Test
    fun testQueryClassifierTrauma() {
        assertEquals(LanternCategory.TRAUMA_CPR, LanternQueryClassifier.classify("how to do cpr on adult"))
        assertEquals(LanternCategory.TRAUMA_CPR, LanternQueryClassifier.classify("severe bleeding on arm"))
        assertEquals(LanternCategory.TRAUMA_CPR, LanternQueryClassifier.classify("tourniquet placement"))
        assertEquals(LanternCategory.TRAUMA_CPR, LanternQueryClassifier.classify("HEMORRHAGE IN THIGH"))
        assertEquals(LanternCategory.TRAUMA_CPR, LanternQueryClassifier.classify("cardiac arrest pulse check"))
    }

    @Test
    fun testQueryClassifierWater() {
        assertEquals(LanternCategory.WATER_SANITATION, LanternQueryClassifier.classify("how to purify flood water"))
        assertEquals(LanternCategory.WATER_SANITATION, LanternQueryClassifier.classify("bleach ratio for clean water"))
        assertEquals(LanternCategory.WATER_SANITATION, LanternQueryClassifier.classify("boil drinking water"))
        assertEquals(LanternCategory.WATER_SANITATION, LanternQueryClassifier.classify("solar sodis uv water"))
        assertEquals(LanternCategory.WATER_SANITATION, LanternQueryClassifier.classify("diy charcoal sand filter"))
    }

    @Test
    fun testQueryClassifierDisasters() {
        assertEquals(LanternCategory.DISASTER_PREP, LanternQueryClassifier.classify("what to do in earthquake"))
        assertEquals(LanternCategory.DISASTER_PREP, LanternQueryClassifier.classify("flash flood warning escape"))
        assertEquals(LanternCategory.DISASTER_PREP, LanternQueryClassifier.classify("tremor drop cover hold"))
        assertEquals(LanternCategory.DISASTER_PREP, LanternQueryClassifier.classify("hurricane storm evacuation"))
    }

    @Test
    fun testQueryClassifierSignaling() {
        assertEquals(LanternCategory.SIGNALING, LanternQueryClassifier.classify("emergency sos signal mirror"))
        assertEquals(LanternCategory.SIGNALING, LanternQueryClassifier.classify("whistle distress codes"))
        assertEquals(LanternCategory.SIGNALING, LanternQueryClassifier.classify("signal aircraft with ground markers"))
    }

    @Test
    fun testQueryClassifierFirstAid() {
        assertEquals(LanternCategory.FIRST_AID, LanternQueryClassifier.classify("choking heimlich maneuver"))
        assertEquals(LanternCategory.FIRST_AID, LanternQueryClassifier.classify("second degree burn treatment"))
        assertEquals(LanternCategory.FIRST_AID, LanternQueryClassifier.classify("hypothermia cold exposure rewarming"))
        assertEquals(LanternCategory.FIRST_AID, LanternQueryClassifier.classify("frostbite fingers numbness"))
    }

    @Test
    fun testQueryClassifierEdgeCases() {
        // Blank or whitespace
        assertNull(LanternQueryClassifier.classify(""))
        assertNull(LanternQueryClassifier.classify("   "))

        // Special characters and symbols
        assertNull(LanternQueryClassifier.classify("!@#$%^&*()_+"))
        assertEquals(LanternCategory.TRAUMA_CPR, LanternQueryClassifier.classify("!!!BLEEDING???"))

        // Numbers and noise
        assertNull(LanternQueryClassifier.classify("12345 67890"))

        // Unrelated queries
        assertNull(LanternQueryClassifier.classify("what is the weather in Paris"))
        assertNull(LanternQueryClassifier.classify("recipe for chocolate cake"))

        // Very long string
        val longQuery = "a".repeat(1000) + " water " + "b".repeat(1000)
        assertEquals(LanternCategory.WATER_SANITATION, LanternQueryClassifier.classify(longQuery))
    }

    // =========================================================================
    // 3. MODEL TIERS INTEGRITY TESTING
    // =========================================================================

    @Test
    fun testModelTiersConfiguration() {
        val tiers = LanternModelTier.entries

        val tierIds = mutableSetOf<String>()
        val fileNames = mutableSetOf<String>()

        for (tier in tiers) {
            // Unique IDs
            assertFalse("Duplicate tier ID: ${tier.tierId}", tierIds.contains(tier.tierId))
            tierIds.add(tier.tierId)

            // Unique File names
            assertFalse("Duplicate file name: ${tier.fileName}", fileNames.contains(tier.fileName))
            fileNames.add(tier.fileName)

            // Valid properties
            assertTrue("Display name must be set", tier.displayName.isNotBlank())
            assertTrue("Description must be set", tier.description.isNotBlank())
            assertTrue("Parameter size must be set", tier.parameterSize.isNotBlank())
            assertTrue("Download URL must be HTTPS", tier.downloadUrl.startsWith("https://"))
            assertTrue("Download size must be positive", tier.downloadSizeBytes > 0)
            assertTrue("RAM requirement must be realistic (>100MB)", tier.ramRequirementMb >= 100)
        }
    }

    // =========================================================================
    // 4. MESH QUERY & RESPONSE PAYLOAD TESTING
    // =========================================================================

    @Test
    fun testMeshPayloadFormatting() {
        val query = LanternMeshQuery(
            queryId = "q-123456",
            senderPeerId = "peer-alpha",
            queryText = "purify water with bleach",
            category = LanternCategory.WATER_SANITATION,
            timestamp = 1700000000000L
        )

        val json = gson.toJson(query)
        val deserialized = gson.fromJson(json, LanternMeshQuery::class.java)

        assertEquals(query.queryId, deserialized.queryId)
        assertEquals(query.senderPeerId, deserialized.senderPeerId)
        assertEquals(query.queryText, deserialized.queryText)
        assertEquals(LanternCategory.WATER_SANITATION, deserialized.category)
        assertEquals(1700000000000L, deserialized.timestamp)

        val response = LanternMeshResponse(
            queryId = "q-123456",
            responderPeerId = "peer-beta",
            responderNickname = "RescueMedic",
            matchedTitle = "Chemical Disinfection",
            guidanceSnippet = "Add 2 drops per Liter of clear water",
            steps = listOf("Step 1: Check label", "Step 2: Add 2 drops"),
            timestamp = 1700000005000L
        )

        val responseJson = gson.toJson(response)
        val deserializedResp = gson.fromJson(responseJson, LanternMeshResponse::class.java)

        assertEquals("RescueMedic", deserializedResp.responderNickname)
        assertEquals(2, deserializedResp.steps.size)
    }

    // =========================================================================
    // 5. FTS SEARCH TERM TOKENIZATION LOGIC TESTING
    // =========================================================================

    @Test
    fun testFtsTermTokenization() {
        val query = "how to stop severe   bleeding fast!!"
        val sanitized = query.replace("[^a-zA-Z0-9 ]".toRegex(), " ")
        val terms = sanitized.trim().split("\\s+".toRegex())
            .filter { it.length >= 2 }
            .map { "$it*" }

        assertEquals(listOf("how*", "to*", "stop*", "severe*", "bleeding*", "fast*"), terms)
        val ftsMatchQuery = terms.joinToString(" OR ")
        assertEquals("how* OR to* OR stop* OR severe* OR bleeding* OR fast*", ftsMatchQuery)
    }
}

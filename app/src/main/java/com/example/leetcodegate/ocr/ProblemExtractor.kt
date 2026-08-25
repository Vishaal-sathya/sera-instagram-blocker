package com.example.leetcodegate.ocr

import com.google.mlkit.vision.text.Text
import kotlin.math.sqrt

data class ExtractedProblem(val id: String, val title: String? = null, val isSolved: Boolean = false)

class ProblemExtractor {
    
    fun extractProblemDetails(visionText: Text): ExtractedProblem? {
        val rawText = visionText.text
        // Use word boundaries to avoid matching "This can be solved..." or "accepted" inside a description
        val isSolved = Regex("(?i)\\b(Accepted|Success)\\b").containsMatchIn(rawText)
        
        // Relaxed whitespace requirement after the period to accommodate OCR glitches
        val pattern1 = Regex("^\\s*(Q?\\d{1,5})\\.\\s*([A-Z].*)", RegexOption.MULTILINE)
        val pattern2 = Regex("^\\s*(Q?\\d{1,5})\\s+[-|]?\\s*([A-Z].*)", RegexOption.MULTILINE)
        
        // 1. Contextual Anchors (Search for "Description" or "Editorial")
        var anchorBlock: Text.TextBlock? = null
        for (block in visionText.textBlocks) {
            val text = block.text.lowercase()
            if (text.contains("description") || text.contains("editorial")) {
                anchorBlock = block
                break
            }
        }

        if (anchorBlock != null) {
            val anchorRect = anchorBlock.boundingBox
            if (anchorRect != null) {
                // Find blocks that are below the anchor
                for (block in visionText.textBlocks) {
                    val rect = block.boundingBox
                    if (rect != null && rect.top >= anchorRect.bottom - 20) {
                        val match = pattern1.find(block.text) ?: pattern2.find(block.text)
                        if (match != null) {
                            return ExtractedProblem(match.groupValues[1].uppercase(), match.groupValues[2].trim(), isSolved)
                        }
                        // Check if it's a standalone ID near the anchor
                        if (block.text.trim().matches(Regex("(?i)^Q?\\d{1,5}$"))) {
                            return ExtractedProblem(block.text.trim().uppercase(), null, isSolved)
                        }
                    }
                }
            }
        }

        // 2. Strict Regex for standard LeetCode headers on a per-block basis
        for (block in visionText.textBlocks) {
            val match = pattern1.find(block.text) ?: pattern2.find(block.text)
            if (match != null) {
                return ExtractedProblem(match.groupValues[1].uppercase(), match.groupValues[2].trim(), isSolved)
            }
        }
        
        // 3. Spatial / Block-based Fallback
        // Prioritize large, top-left text
        var bestMatch: ExtractedProblem? = null
        var bestScore = Double.MAX_VALUE

        for (block in visionText.textBlocks) {
            val text = block.text.trim()
            val match = Regex("(?i)^Q?\\d{1,5}$").find(text) ?: Regex("(?i)^(Q?\\d{1,5})").find(text)
            
            if (match != null) {
                val extractedId = match.groupValues[1].ifEmpty { match.value }
                val rect = block.boundingBox
                if (rect != null) {
                    // Lower score is better (closer to top-left, but larger height heavily reduces the score)
                    val distance = sqrt((rect.top * rect.top + rect.left * rect.left).toDouble())
                    val heightBonus = rect.height() * 5.0
                    val score = distance - heightBonus
                    
                    if (score < bestScore) {
                        bestScore = score
                        bestMatch = ExtractedProblem(extractedId.uppercase(), null, isSolved)
                    }
                } else if (bestMatch == null) {
                    bestMatch = ExtractedProblem(extractedId.uppercase(), null, isSolved)
                }
            }
        }

        return bestMatch
    }
    
    // Legacy method for compatibility with existing VerificationViewModel code
    fun extractProblemId(visionText: Text): String? {
        return extractProblemDetails(visionText)?.id
    }
}

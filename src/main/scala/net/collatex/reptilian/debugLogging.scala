package net.collatex.reptilian

import org.slf4j.LoggerFactory
import scala.collection.immutable.MultiSet

// Initialize the logger for your specific class
private val logger = LoggerFactory.getLogger(this.getClass)

def validateTokens(rootTokens: MultiSet[Int], gTaTokens: MultiSet[Int]): Unit = {
  // 1. Find Duplicates in gTa
  val duplicates = gTaTokens.occurrences.filter(_._2 > 1).keySet

  // 2. Find Missing from gTa (since root contains unique tokens)
  val missing = rootTokens.occurrences.keySet.filterNot(gTaTokens.contains)

  // 3. Logic-based Logging
  if (duplicates.isEmpty && missing.isEmpty) {
    logger.info("Token validation successful: No missing or duplicate tokens found.")
  } else {
    if (duplicates.nonEmpty) {
      // Log as WARN or DEBUG depending on how critical this is
      logger.warn(s"Duplicate tokens detected in gTa: ${duplicates.mkString(", ")}")
    }

    if (missing.nonEmpty) {
      logger.warn(s"Tokens present in root but missing in gTa: ${missing.mkString(", ")}")
    }
  }
}

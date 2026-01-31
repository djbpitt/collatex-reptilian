package net.collatex.reptilian

import ch.qos.logback.classic.{Level, Logger}
import org.slf4j.LoggerFactory

import scala.collection.immutable.MultiSet

/*Create logger for sanity-check: no missing or duplicate tokens */
val loggerSanity = LoggerFactory.getLogger("sanity-check")
  .asInstanceOf[Logger]   // cast to Logback's Logger
val effectiveLevel: Level = loggerSanity.getEffectiveLevel // Used in guard below
//val x: Unit = loggerSanity.setLevel(Level.WARN)

def sanityLogging(root: AlignmentRibbon, gTa: Vector[TokenEnum]): Unit = {
  val rootTokens: MultiSet[WitId] =
    root.children
      .flatMap(_.asInstanceOf[AlignmentPoint].witnessReadings.flatMap((_, v) => Range(v.start, v.until)))
      .to(MultiSet)
  val gValues: Vector[WitId] = gTa map {
    case t: TokenEnum.Token => t.g
    case _ => -1
  }
  val gTaTokens = gValues.to(MultiSet).filter(_ != -1)
  val duplicateTokens = rootTokens.occurrences.collect {
    case (item, count) if count > 1 => item
  }
  val missingTokens = gTaTokens.occurrences.keySet.filterNot(rootTokens.contains)

  if duplicateTokens.isEmpty && missingTokens.isEmpty then
    loggerSanity.debug("No duplicate or missing tokens")
  else
    if duplicateTokens.nonEmpty then
      loggerSanity.debug(s"Duplicate tokens: $duplicateTokens")
    if missingTokens.nonEmpty then
      loggerSanity.debug(s"Missing tokens: $missingTokens")
}

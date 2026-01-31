package net.collatex.reptilian

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.core.read.ListAppender
import net.collatex.reptilian.TokenEnum.{Token, TokenSep}
import org.scalatest.*
import org.scalatest.funsuite.AnyFunSuite
import org.slf4j.LoggerFactory

import scala.collection.mutable.ListBuffer

class sanityCheckTest extends AnyFunSuite:
  val loggerSanity: Logger = LoggerFactory
    .getLogger("sanity-check")
    .asInstanceOf[Logger] // cast to Logback's Logger
  assert(loggerSanity != null)
  val listAppender: ListAppender[ILoggingEvent] = // log to memory for testing
    loggerSanity.getAppender("LIST").asInstanceOf[ListAppender[ILoggingEvent]]
  val effectiveLevel: Level = loggerSanity.getEffectiveLevel // Used in guard

  test("Correctly logs no duplicate or missing tokens in alignment") {
    val gTa: Vector[TokenEnum] = Vector(
      Token("a", "a", 0, 0, Map()),
      TokenSep("s", "s", 0, 1),
      Token("a", "a", 1, 2, Map())
    )
    val root = AlignmentRibbon(
      ListBuffer[_root_.net.collatex.reptilian.AlignmentUnit](
        AlignmentPoint(
          Map(0 -> TokenRange(0, 1, gTa), 1 -> TokenRange(2, 3, gTa)),
          Set(Map(0 -> TokenRange(0, 1, gTa), 1 -> TokenRange(2, 3, gTa)))
        )
      )
    )
    sanityLogging(root, gTa)
    val lastEventMessage = listAppender.list.getLast.getMessage
    assert(lastEventMessage == "No duplicate or missing tokens")
  }

  test("Correctly logs missing (no duplicate) tokens in alignment") {
    val gTa: Vector[TokenEnum] = Vector(
      Token("a", "a", 0, 0, Map()),
      TokenSep("s", "s", 0, 1),
      Token("a", "a", 1, 2, Map()),
      Token("b", "b", 1, 3, Map()) // Token 3 not part of alignment
    )
    val root = AlignmentRibbon(
      ListBuffer[_root_.net.collatex.reptilian.AlignmentUnit](
        AlignmentPoint(
          Map(0 -> TokenRange(0, 1, gTa), 1 -> TokenRange(2, 3, gTa)),
          Set(Map(0 -> TokenRange(0, 1, gTa), 1 -> TokenRange(2, 3, gTa)))
        )
      )
    )
    sanityLogging(root, gTa)
    val lastEventMessage = listAppender.list.getLast.getMessage
    assert(lastEventMessage == "Missing tokens: Set(3)")
  }

  test("Correctly logs duplicate (no missing) tokens in alignment") {
    val gTa: Vector[TokenEnum] = Vector(
      Token("a", "a", 0, 0, Map()),
      Token("ab", "ab", 0, 1, Map()),
      Token("b", "b", 0, 2, Map()),
      TokenSep("s", "s", 0, 3),
      Token("a", "a", 1, 4, Map()),
      Token("ab", "ab", 1, 5, Map()),
      Token("ab", "ab", 1, 6, Map()),
      Token("b", "b", 1, 7, Map())
    )
    val root = AlignmentRibbon(
      ListBuffer[_root_.net.collatex.reptilian.AlignmentUnit](
        AlignmentPoint(
          Map(0 -> TokenRange(0, 2, gTa), 1 -> TokenRange(4, 6, gTa)),
          Set(Map(0 -> TokenRange(0, 2, gTa), 1 -> TokenRange(4, 6, gTa)))
        ),
        AlignmentPoint(
          Map(0 -> TokenRange(1, 3, gTa), 1 -> TokenRange(6, 8, gTa)),
          Set(Map(0 -> TokenRange(1, 3, gTa), 1 -> TokenRange(6, 8, gTa)))
        )
      )
    )
    sanityLogging(root, gTa)
    val lastEventMessage = listAppender.list.getLast.getMessage
    assert(lastEventMessage == "Duplicate tokens: List(1)")
  }

  test("Correctly logs both duplicate and missing tokens in alignment") {
    val gTa: Vector[TokenEnum] = Vector(
      Token("a", "a", 0, 0, Map()),
      Token("ab", "ab", 0, 1, Map()),
      Token("b", "b", 0, 2, Map()),
      TokenSep("s", "s", 0, 3),
      Token("a", "a", 1, 4, Map()),
      Token("ab", "ab", 1, 5, Map()),
      Token("ab", "ab", 1, 6, Map()),
      Token("b", "b", 1, 7, Map()),
      Token("c", "c", 1, 8, Map())
    )
    val root = AlignmentRibbon(
      ListBuffer[_root_.net.collatex.reptilian.AlignmentUnit](
        AlignmentPoint(
          Map(0 -> TokenRange(0, 2, gTa), 1 -> TokenRange(4, 6, gTa)),
          Set(Map(0 -> TokenRange(0, 2, gTa), 1 -> TokenRange(4, 6, gTa)))
        ),
        AlignmentPoint(
          Map(0 -> TokenRange(1, 3, gTa), 1 -> TokenRange(6, 8, gTa)),
          Set(Map(0 -> TokenRange(1, 3, gTa), 1 -> TokenRange(6, 8, gTa)))
        )
      )
    )
    sanityLogging(root, gTa)
    // We log duplicates before missings
    // Removes and returns; parentheses required by Java (not Scala)
    System.err.println(s"list: ${listAppender.list}")
    val lastEventMessage = listAppender.list.removeLast().getMessage
    assert(lastEventMessage == "Missing tokens: Set(8)")
    val penultimateEventMessage = listAppender.list.removeLast().getMessage
    assert(penultimateEventMessage == "Duplicate tokens: List(1)")
  }

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
    assert(true)
  }

  test("Correctly logs duplicate (no missing) tokens in alignment") {
    assert(true)
  }

  test("Correctly logs both duplicate and missing tokens in alignment") {
    assert(true)
  }

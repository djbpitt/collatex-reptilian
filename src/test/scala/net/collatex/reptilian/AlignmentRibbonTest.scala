package net.collatex.reptilian

import org.scalatest.*
import org.scalatest.funsuite.AnyFunSuite
import fs2.{Pure, Stream}

import scala.collection.mutable.ListBuffer

class AlignmentRibbonTest extends AnyFunSuite:
  test("Empty alignment ribbon emits empty stream") {
    val ar: AlignmentRibbon = AlignmentRibbon()
    val expected: Stream[Pure, AlignmentPoint] = Stream()
    val result = ar.streamChildren
    assert(result == expected)
  }
  test("Non-empty alignment ribbon emits populated stream") {
    val gTa = Vector[TokenEnum]()
    val aps: List[AlignmentPoint] = List(
      AlignmentPoint(gTa, Map(0 -> TokenRange(0, 1, gTa))),
      AlignmentPoint(gTa, Map(0 -> TokenRange(1, 2, gTa))),
      AlignmentPoint(gTa, Map(0 -> TokenRange(2, 3, gTa)))
    )
    val ar: AlignmentRibbon = AlignmentRibbon(
      ListBuffer.from(aps)
    )
    val expected: Stream[Pure, AlignmentPoint] = Stream.emits(aps)
    val result: Stream[Pure, AlignmentPoint] = ar.streamChildren
    assert(result.toList == expected.toList) // Can't compare streams directly
  }

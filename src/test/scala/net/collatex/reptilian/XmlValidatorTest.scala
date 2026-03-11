package net.collatex.reptilian

import org.scalatest.funsuite.AnyFunSuite

class XmlValidatorTest extends AnyFunSuite:
  test("Allow valid characters") {
    val testString = "abc"
    val result = XmlValidator.isValid(testString)
    assert(result) // Must be true
  }

  test("Disallow invalid characters") {
    val invalidChar = 1.toChar // U+0001
    val invalidString = s"a${invalidChar}b"
    val result = XmlValidator.isValid(invalidString)
    assert(!result) // Must be false
  }

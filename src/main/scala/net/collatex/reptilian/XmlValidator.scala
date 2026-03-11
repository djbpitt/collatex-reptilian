package net.collatex.reptilian

import scala.util.matching.Regex

object XmlValidator {
  // Regex matching characters NOT allowed in XML 1.0
  // Note: This covers the BMP; does not handle supplementary planes
  // (emojis, etc.) or surrogate pairs (NB: this may turn out to be an
  // issue).
  // Automatically compiled for reuse
  private val InvalidXml10Regex: Regex = "[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD]".r

  def isValid(input: String): Boolean = {
    // Returns true if no invalid characters are found
    InvalidXml10Regex.findFirstIn(input).isEmpty
  }
}

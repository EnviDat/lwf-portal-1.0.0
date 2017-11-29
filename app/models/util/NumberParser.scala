package models.util

import scala.util.{Failure, Success, Try}


object NumberParser {

  def parseNumber(word: String) = {
    val numberTry = for {
      statNr <- Try(Integer.parseInt(word))
    } yield {
      statNr
    }
    val parsedNumber = numberTry match {
      case Failure(thrown) => {
        None
      }
      case Success(s) => {
        Some(s)
      }
    }
    parsedNumber
  }

  def parseBigDecimal(word: String) = {
    val value = if (word.toUpperCase.toString == "NAN") "-9999" else word
    val numberTry = for {
      statNr <- Try(BigDecimal.apply(value))
    } yield {
      statNr
    }
    val parsedNumber = numberTry match {
      case Failure(thrown) => {
        None
      }
      case Success(s) => {
        Some(s)
      }
    }
    parsedNumber
  }
}

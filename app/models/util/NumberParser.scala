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

  def parseBigInt(word: String) = {
    val numberTry = for {
      statNr <- Try(BigInt.apply(word))
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
        None      }
      case Success(s) => {
        Some(s)
      }
    }
    parsedNumber
  }

  def parseBigDecimalWithLessThanSign(word: String) = {
    val value = if (word.trim.startsWith("<"))
      "-8888"
    else if (word.trim.contains("WS"))
      word.substring(word.indexOf("WS")).stripPrefix("WS").trim
    else
      word

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

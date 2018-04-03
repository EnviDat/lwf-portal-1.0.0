package models.util

import java.text.SimpleDateFormat
import java.time.format.DateTimeParseException

import models.domain
import models.domain.{CR1000Exceptions, CR1000InvalidDateException}
import org.joda.time.{DateTime, DateTimeZone}
import play.Logger


object StringToDate {

  import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
  val oracleDateFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")
  val oracleMetaBlagDateFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss.SSS")


  val formatDate: DateTimeFormatter = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss")

  val formatCR1000Date: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")


  def stringToDateConvert(date: String) = {
      formatDate.withZone(DateTimeZone.UTC)parseDateTime(date)
  }

  def stringToDateConvertCR1000(date: String): Option[CR1000Exceptions] = {
    try {
    formatCR1000Date.withZone(DateTimeZone.UTC)parseDateTime(date)
    None
    }
    catch {
      case err: DateTimeParseException => {
        Logger.info(s" is not parsable${date} errror Message: ${err}")
        Some(CR1000InvalidDateException(4, s"date is not parsable${date} errror Message: ${err}"))
      }
      case err: IllegalArgumentException => {
        Logger.info(s" is not parsable${date} errror Message: ${err}")
        Some(CR1000InvalidDateException(4, s"date is not parsable${date} errror Message: ${err}"))
      }
    }

  }

}

object CurrentSysDateInSimpleFormat {
  def dateNow = new SimpleDateFormat("yyyyMMddHHmmss").format(new  java.util.Date())
}
object Joda {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}



package models.util

import java.text.SimpleDateFormat
import java.time.format.DateTimeParseException
import java.util.{Date, Locale, TimeZone}

import models.domain
import models.domain.{CR1000Exceptions, CR1000InvalidDateException}
import models.ozone.{OzoneExceptions, OzoneInvalidDateException}
import models.util.StringToDate.formatOzoneDate
import org.joda.time.{DateTime, DateTimeZone}
import play.Logger


object StringToDate {

  import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
  val oracleDateFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")
  val oracleMetaBlagDateFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss.SSS")

  val oracleDateNoTimeFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy")

  val formatDate: DateTimeFormatter = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss")
  val formatDateTime: DateTimeFormatter = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss.SSSSSSSSS")

  val formatCR1000Date: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
  val formatOzoneDate: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.YYYY HH:mm:ss")

  val formatOzoneDateWithNoTime: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.YYYY")
  val formatOzoneDateWithNoTimeWithDash: DateTimeFormatter = DateTimeFormat.forPattern("dd-MM-YYYY")

  val formatFromDateToStringDefaultJava = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy",Locale.ENGLISH)
  def stringToDateConvert(date: String): DateTime = {
      formatDate.withZone(DateTimeZone.UTC).parseDateTime(date)
  }

  def stringToDateConvertWithTime(date: String): DateTime = {
    formatDateTime.withZone(DateTimeZone.UTC).parseDateTime(date)
  }

  def stringToDateConvertCR1000(date: String, lineToValidate: String): Option[CR1000Exceptions] = {
    try {
    formatCR1000Date.withZone(DateTimeZone.UTC).parseDateTime(date.substring(0, 19))
    None
    }
    catch {
      case err: DateTimeParseException => {
        Logger.info(s" is not parsable${date} errror Message: ${err}")
        Some(CR1000InvalidDateException(4, s"date is not parsable${date} errror Message: ${err}, ${lineToValidate}"))
      }
      case err: IllegalArgumentException => {
        Logger.info(s" is not parsable${date} errror Message: ${err}")
        Some(CR1000InvalidDateException(4, s"date is not parsable${date} errror Message: ${err},  ${lineToValidate}"))
      }
    }

  }

  def stringToDateConvertoZONE(date: String, lineToValidate: String): Option[OzoneExceptions] = {
    try {
      formatOzoneDate.withZone(DateTimeZone.UTC).parseDateTime(date)
      None
    }
    catch {
      case err: DateTimeParseException => {
        Logger.info(s" is not parsable${date} errror Message: ${err}")
        Some(OzoneInvalidDateException(4, s"date is not parsable${date} errror Message: ${err}, ${lineToValidate}"))
      }
      case err: IllegalArgumentException => {
        Logger.info(s" is not parsable${date} errror Message: ${err}")
        Some(OzoneInvalidDateException(4, s"date is not parsable${date} errror Message: ${err},  ${lineToValidate}"))
      }
    }

  }
}

object CurrentSysDateInSimpleFormat {
  def dateNow = new SimpleDateFormat("yyyyMMddHHmmss").format(new  java.util.Date())
  def dateNowOnlyDay = new SimpleDateFormat("yyyy-MM-dd").format(new  java.util.Date())

  def dateRegex = raw"(\d{4})-(\d{2})-(\d{2})\s(\d{2}):(\d{2}):(\d{2})".r

  val systemDateForEinfdat = s"${StringToDate.formatOzoneDate.print(new DateTime())}"
  val sysdateDateInOracleformat = s"TO_date('${systemDateForEinfdat}', 'DD.MM.YYYY HH24:MI:SS')"

  def changeFormatDateTimeForFileName(datum: DateTime) = {
    new SimpleDateFormat("yyyyMMddHHmmss").format(datum.toDate)
  }

  def changeFormatDateTimeForFileNameWithUTC(datum: DateTime): String = {
   val simpleDate =  new SimpleDateFormat("yyyyMMddHHmmss")
    simpleDate.setTimeZone(TimeZone.getTimeZone("UTC"))
    simpleDate.format(datum.toDate)
  }

  def changeFormatOfDateForSeparators(dateInputFormat: String): String = {
    dateInputFormat.split("-").mkString("")
  }

}
object Joda {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)


}



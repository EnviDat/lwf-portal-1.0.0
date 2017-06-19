package models.util

import java.text.SimpleDateFormat

import org.joda.time.DateTime


object StringToDate {

  import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
  val oracleDateFormat: DateTimeFormatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm:ss")

  val formatDate: DateTimeFormatter = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss")

  def stringToDateConvert(date: String) = {
    //Logger.debug(s"input Date: $date")
    //Logger.debug(s"converted Date: ${formatDate.parseDateTime(date)}")
    formatDate.parseDateTime(date)

  }

}

object CurrentSysDateInSimpleFormat {
  def dateNow = new SimpleDateFormat("yyyyMMddHHmmss").format(new  java.util.Date())
}
object Joda {
  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
}



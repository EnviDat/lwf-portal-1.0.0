package models.util

import org.joda.time.DateTime
import play.api.Logger


object StringToDate {

  import org.joda.time.format.DateTimeFormat
  import org.joda.time.format.DateTimeFormatter

  val formatDate: DateTimeFormatter = DateTimeFormat.forPattern("dd-MM-yyyy HH:mm:ss")
 def stringToDateConvert(date: String) =  {
   //Logger.debug(s"input Date: $date")
   //Logger.debug(s"converted Date: ${formatDate.parseDateTime(date)}")
   formatDate.parseDateTime(date)

 }



}

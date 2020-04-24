package models.services

import java.time._
import java.util.Date

import models.domain._
import models.domain.unibasel.HexenRubiStationAbbrevations
import models.util.StringToDate.formatCR1000Date
import models.util.{NumberParser, StringToDate}
import org.joda.time.{DateTime, DateTimeZone, Hours}
import play.api.Logger

import scala.collection.immutable


object CR10XFileParser {

  def parseAndSaveData(cr100FileData: List[String], meteoService: MeteoService, fileName: String, stationNr: Int, projectNr: Int, duration: Int): Option[CR1000OracleError] = {
    val allMessWerts: Seq[MessArtRow] = meteoService.getAllMessArts
    val allStationConfigs: List[MeteoStationConfiguration] = meteoService.getStatKonfForStation().filter(sk => allMessWerts.map(_.code).contains( sk.messArt))
    val dtz = DateTimeZone.forID("CET")
    val allRowsToBeInserted = cr100FileData.par.flatMap(line => {
      val words = line.split(",")
      val dayOfYear = words(4).toInt
      val yearInLine = Year.of( words(3).toInt )
      val dateFromDayOfYear: LocalDate = yearInLine.atDay( dayOfYear )

      val (hrs, mins) = words(5).length match {
        case 1 => (0,0)
        case 4 => getHourAndMinuteFromWord(words(5))
        case _ => (99, 99)
      }
      //Logger.info(s"line parsed: ${line}")

      //val dateTimeFromDataLine = ZonedDateTime.of(dateFromDayOfYear.getYear,dateFromDayOfYear.getMonth.getValue,dateFromDayOfYear.getDayOfMonth,hrs,mins,0,0, ZoneId.of("UTC")).toInstant
      //val dateInstance = dateTimeFromDataLine.withZoneSameInstant(ZoneId.of("UTC")).toInstant
      //val messDatum = Date.from(dateTimeFromDataLine)


     val localMessDatum = new org.joda.time.LocalDateTime(dtz).withYear(dateFromDayOfYear.getYear).withMonthOfYear(dateFromDayOfYear.getMonth.getValue).withDayOfMonth(dateFromDayOfYear.getDayOfMonth)
       .withHourOfDay(hrs).withMinuteOfHour(mins)
      val messDatum = if( dtz.isLocalDateTimeGap(localMessDatum)) dateFromDayOfYear.getDayOfMonth.toString  + "." + dateFromDayOfYear.getMonth.getValue.toString + "." + dateFromDayOfYear.getYear.toString + " " + (hrs + 1) .toString  + ":" + mins.toString + ":00"
      else dateFromDayOfYear.getDayOfMonth.toString  + "." + dateFromDayOfYear.getMonth.getValue.toString + "." + dateFromDayOfYear.getYear.toString + " " + hrs.toString  + ":" + mins.toString + ":00"

      val stationNumber =  Some(stationNr)
      val projectNumber =  Some(projectNr)
      val periode = Some(duration)

      val valuesToBeInserted =  for {
          statNr <- stationNumber
          projNr <- projectNumber
          period <- periode
          messWerts = allMessWerts.filter(mt => mt.pDauer == period && mt.messProjNr.contains(projNr))
          statConfig = allStationConfigs.filter(sk => sk.station == statNr && messWerts.map(_.code).contains(sk.messArt)).sortBy(_.folgeNr)
          folgnr: Seq[(Option[Int], Int, Int)] =  statConfig.map(sk => (sk.folgeNr, sk.messArt,sk.configNumber))
          elements = words.drop(6).zipWithIndex
          rangeFolgnr = List.range(1,elements.length).toSet
          sortedFolgnr = folgnr.flatMap(_._1).sorted.toSet
          missingFolgnr = rangeFolgnr diff sortedFolgnr
          filterUnrequiredFolgnr: Array[String] = elements.filter(e => !missingFolgnr.contains(e._2 + 1)).map(_._1)
          valuesToInsert: Map[(Option[Int], Int, Int), String] = (folgnr zip filterUnrequiredFolgnr).toMap
          values = valuesToInsert.flatMap(element => {
            val messart: Int = element._1._2
            val multi = allMessWerts.find(_.code == messart).map(_.multi)
            val configNum = element._1._3
            val messartValueToSave = if(element._2.trim == "") None else { NumberParser.parseBigDecimal(element._2) match {
              case Some(x) if x != BigDecimal(-9999) &&  x != BigDecimal(-7999) && x != BigDecimal(-6999)  => Some(x)
              case _ => None
            }}
            for {
              valueMessart <- messartValueToSave
              messDate = s"to_date('${messDatum}', 'DD.MM.YYYY HH24:MI:SS')"
              einfDate = s"TO_TIMESTAMP('${StringToDate.oracleMetaBlagDateFormat.print(new DateTime())}', 'DD.MM.YYYY HH24:MI:SS.FF3')"
              mrow = MeteoDataRowTableInfo(MeteoDataRow(statNr,messart,configNum,messDate,valueMessart,einfDate,1,Some(1)),multi, fileName)
            } yield mrow
          })
        } yield values
      valuesToBeInserted
    }).flatten.toList

    // Aggregation not required for CR10X data as these are 10 minutes
    /*val aggregatedLinesWithTimestamp = allRowsToBeInserted.map(al => {
      (StringToDate.formatOzoneDate.withOffsetParsed.parseDateTime(al.meteoDataRow.dateReceived.stripPrefix("to_date('").replaceAll("'", "").stripSuffix(", DD.MM.YYYY HH24:MI:SS)")), al)
    })
    val maximumDateDataWasRead: Option[DateTime] = meteoService.findMaxMeasurementDateForAStation(stationNr).headOption.map(maxDate => StringToDate.formatOzoneDate.withZoneUTC().parseDateTime(maxDate.stripPrefix("to_date('").replaceAll("'", "").stripSuffix(", DD.MM.YYYY HH24:MI:SS)")))
    val filteredAggregatedLinesToInsert: immutable.Iterable[MeteoDataRowTableInfo] = maximumDateDataWasRead match {
      case None => allRowsToBeInserted
      case Some(maxDate) => aggregatedLinesWithTimestamp.filter(_._1.isAfter(maxDate)).map(_._2)
    }*/

    meteoService.insertMeteoDataCR1000(allRowsToBeInserted.toList)
  }

  //ToDo: This method would work only if length of the word is 4 letters. Modify it for extracting the hours and minutes if length is greated than 4 letters
  private def getHourAndMinuteFromWord(word: String) = {
    val hoursString = word.dropRight(2)
    val minutes = word.takeRight(2)
    (hoursString.toInt, minutes.toInt)

  }

  private def getMappingOfFolgeNrToMessArt(confForStation: List[MeteoStationConfiguration]) = {
    confForStation.map(cf => (cf.folgeNr.getOrElse(-1), cf.messArt)).sortBy(_._1)
  }

}

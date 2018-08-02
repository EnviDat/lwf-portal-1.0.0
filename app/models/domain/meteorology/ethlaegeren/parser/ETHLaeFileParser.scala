package models.domain.meteorology.ethlaegeren.parser

import models.domain._
import models.domain.meteorology.ethlaegeren.domain.{MeasurementParameter, StationConfiguration}
import models.services.MeteorologyService
import models.util.StringToDate.formatCR1000Date
import models.util.{NumberParser, StringToDate}
import org.joda.time.{DateTime, DateTimeZone}


object ETHLaeFileParser {

  def parseAndSaveData(ethLaeFileData: List[String], meteoService: MeteorologyService, fileName: String): Option[CR1000OracleError] = {
    val allMessWerts: Seq[MeasurementParameter] = meteoService.getAllMessArts
    val allStationConfigs: List[StationConfiguration] = meteoService.getStatKonfForStation().filter(sk => allMessWerts.map(_.code).contains( sk.measurementParam))

    val allRowsToBeInserted = ethLaeFileData.flatMap(line => {
      val words = line.split(",")
      val date = formatCR1000Date.withZone(DateTimeZone.UTC).parseDateTime(words(0).replace("\"", ""))
      val recordNumber = NumberParser.parseNumber(words(1))
      val stationNumber =  Some(124)
      val projectNumber =  Some(1)
      val periode = Some(10)

      val valuesToBeInserted =  for {
          statNr <- stationNumber
          projNr <- projectNumber
          period <- periode
          messWerts = allMessWerts.filter(mt => mt.period == period)
          statConfig: Seq[StationConfiguration] = allStationConfigs.filter(sk => (sk.station == statNr && messWerts.map(_.code).contains(sk.measurementParam) && sk.projectNr.contains(projNr))).sortBy(_.folgeNr)
          folgnr: Seq[(Option[Int], Int, Int)] =  statConfig.map(sk => (sk.folgeNr, sk.measurementParam,sk.configNumber))
          elements = words.drop(2).zipWithIndex
          rangeFolgnr = List.range(1,elements.length).toSet
          sortedFolgnr = folgnr.flatMap(_._1).sorted.toSet
          missingFolgnr = rangeFolgnr diff sortedFolgnr
          filterUnrequiredFolgnr: Array[String] = elements.filter(e => !missingFolgnr.contains(e._2 + 1)).map(_._1)
          valuesToInsert: Map[(Option[Int], Int, Int), String] = (folgnr zip filterUnrequiredFolgnr).toMap
          values = valuesToInsert.flatMap(element => {
            val messart: Int = element._1._2
            val multi = allMessWerts.find(_.code == messart).map(_.multi)
            val configNum = element._1._3
            val messartValueToSave = NumberParser.parseBigDecimal(element._2) match {
              case Some(x) if x != BigDecimal(-9999) &&  x != BigDecimal(-7999) && x != BigDecimal(-6999)  => Some(x)
              case _ => None
            }
            for {
              valueMessart <- messartValueToSave
              messDate = s"to_date('${StringToDate.oracleDateFormat.print(date)}', 'DD.MM.YYYY HH24:MI:SS')"
              einfDate = s"TO_TIMESTAMP('${StringToDate.oracleMetaBlagDateFormat.print(new DateTime())}', 'DD.MM.YYYY HH24:MI:SS.FF3')"
              mrow = MeteoDataRowTableInfo(MeteoDataRow(statNr,messart,configNum,messDate,valueMessart,einfDate,1,Some(1)),multi, fileName)
            } yield mrow
          })
        } yield values
      valuesToBeInserted
    }).flatten.toList
    meteoService.insertMeteoDataETHLae(allRowsToBeInserted)
  }

  private def getMappingOfFolgeNrToMessArt(confForStation: List[MeteoStationConfiguration]) = {
    confForStation.map(cf => (cf.folgeNr.getOrElse(-1), cf.messArt)).sortBy(_._1)
  }

}

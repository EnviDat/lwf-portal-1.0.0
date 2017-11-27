package models.services

import models.domain._
import models.util.{NumberParser, StringToDate}
import models.util.StringToDate.formatCR1000Date
import org.joda.time.DateTime

import scala.collection.immutable


object CR1000FileParser {

  def parseAndSaveData(cr100FileData: List[String], meteoService: MeteoService, fileName: String) = {
    val allMessWerts: Seq[MessArtRow] = meteoService.getAllMessArts
    val allStationConfigs: List[MeteoStationConfiguration] = meteoService.getStatKonfForStation().filter(sk => allMessWerts.map(_.code).contains( sk.messArt))

    val allRowsToBeInserted = cr100FileData.flatMap(line => {
      val words = line.split(",")
      val date = formatCR1000Date.parseDateTime(words(0).replace("\"", ""))
      val recordNumber = NumberParser.parseNumber(words(1))
      val stationNumber =  NumberParser.parseNumber(words(2))
      val projectNumber =  NumberParser.parseNumber(words(3))
      val periode = NumberParser.parseNumber(words(4))

      val valuesToBeInserted =  for {
          statNr <- stationNumber
          projNr <- projectNumber
          period <- periode
          messWerts = allMessWerts.filter(mt => mt.pDauer == period && mt.messProjNr.contains(projNr))
          statConfig = allStationConfigs.filter(sk => sk.station == statNr && messWerts.map(_.code).contains(sk.messArt)).sortBy(_.folgeNr)
          folgnr =  statConfig.map(sk => (sk.folgeNr, sk.messArt,sk.configNumber))
          elements = words.drop(5).zipWithIndex
          rangeFolgnr = List.range(1,elements.length).toSet
          sortedFolgnr = folgnr.flatMap(_._1).sorted.toSet
          missingFolgnr = rangeFolgnr diff sortedFolgnr
          filterUnrequiredFolgnr = elements.filter(e => !missingFolgnr.contains(e._2 + 1)).map(_._1)
          valuesToInsert = (folgnr zip filterUnrequiredFolgnr).toMap
          values = valuesToInsert.flatMap(element => {
            val messart = element._1._2
            val multi = allMessWerts.find(_.code == messart).map(_.multi)
            val configNum = element._1._3
            val messartValueToSave = NumberParser.parseBigDecimal(element._2) match {
              case Some(x) if x != BigDecimal(-9999) => Some(x)
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
    meteoService.insertMeteoDataCR1000(allRowsToBeInserted)
  }

  private def getMappingOfFolgeNrToMessArt(confForStation: List[MeteoStationConfiguration]) = {
    confForStation.map(cf => (cf.folgeNr.getOrElse(-1), cf.messArt)).sortBy(_._1)
  }

}

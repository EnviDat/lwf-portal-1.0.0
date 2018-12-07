package models.domain.meteorology.ethlaegeren.parser

import models.domain._
import models.domain.meteorology.ethlaegeren.domain.{MeasurementParameter, StationConfiguration}
import models.services.{MeteoService, MeteorologyService}
import models.util.StringToDate.formatCR1000Date
import models.util.{NumberParser, StringToDate}
import org.joda.time.{DateTime, DateTimeZone}
import schedulers.StationKonfig

import scala.collection.immutable


object ETHLaeFileParser {

  def parseAndSaveData(timestampToWriteData: DateTime, nrOfLines: Int, ethLaeFileDataLines: List[String], meteoService: MeteoService, fileName: String, maxDateInFile: DateTime, stationKonfigs: List[StationKonfig]) = {
    val mapStationKonfig: Option[StationKonfig] = stationKonfigs.find(sk => fileName.startsWith(sk.fileName))

    val validLinesToInsert =
    mapStationKonfig.map(statKonfig => {
      val statNr = statKonfig.statNr

    val projects = mapStationKonfig.map(_.projs.map(_.projNr)).toList.flatten

    val allMessWerts: Seq[MessArtRow] = meteoService.getAllMessArts.filter(mArt => mArt.messProjNr match {
      case Some(x) => projects.contains(x)
      case None => false
    })


    val allStationConfigs: List[MeteoStationConfiguration] = meteoService.getStatKonfForStation().filter(sk => sk.station == statKonfig.statNr).filter(sk => allMessWerts.map(_.code).contains( sk.messArt)).sortBy(_.folgeNr)
     val valuesToBeInserted = ethLaeFileDataLines.map(line => {
        val words = line.split(",")
        val elements = words.drop(2).zipWithIndex

      //val folgnr: Seq[((Option[Int], Int), Int, Option[Int], Option[String])] =  allStationConfigs.map(sk => ((sk.folgeNr,sk.configNumber), sk.messArt, sk.completeness, sk.methode))
      val folgnr: Seq[(Option[Int], Int, Int)] =  allStationConfigs.map(sk => (sk.folgeNr, sk.messArt,sk.configNumber))

      val rangeFolgnr = List.range(1,elements.length).toSet
      val sortedFolgnr = folgnr.flatMap(_._1).sorted.toSet
      val missingFolgnr = rangeFolgnr diff sortedFolgnr
      val filterUnrequiredFolgnr = elements.filter(e => !missingFolgnr.contains(e._2 + 1)).map(_._1)
      val valuesToInsert: Map[(Option[Int], Int, Int), String] = (folgnr zip filterUnrequiredFolgnr).toMap
      val  values = valuesToInsert.flatMap(element => {
          val messart: Int = element._1._2
          val multi = allMessWerts.find(_.code == messart).map(_.multi)
          val configNum = element._1._3
          val messartValueToSave = NumberParser.parseBigDecimal(element._2) match {
            case Some(x) if x != BigDecimal(-9999) &&  x != BigDecimal(-7999) && x != BigDecimal(-6999)  => Some(x)
            case _ => None
          }
          for {
            valueMessart <- messartValueToSave
            messDate = s"to_date('${StringToDate.oracleDateFormat.print(timestampToWriteData)}', 'DD.MM.YYYY HH24:MI:SS')"
            einfDate = s"TO_TIMESTAMP('${StringToDate.oracleMetaBlagDateFormat.print(new DateTime())}', 'DD.MM.YYYY HH24:MI:SS.FF3')"
            mrow = MeteoDataRowTableInfo(MeteoDataRow(statNr,messart,configNum,messDate,valueMessart,einfDate,1,Some(1)),multi, fileName)
          } yield mrow
        })
       values
      })

      val groupByKonf = valuesToBeInserted.flatten.toList.groupBy(_.meteoDataRow.configuration)
     val aggregatedMessartValuesForEachConfig= groupByKonf.flatMap(valueForKonf => {
        val completenessRequired = allStationConfigs.filter(_.configNumber == valueForKonf._1).flatMap(_.completeness).headOption
        val methodToApply = allStationConfigs.filter(_.configNumber == valueForKonf._1).flatMap(_.methode).headOption
        val aggregatedMeasurementValue = for {
          completeness <- completenessRequired
          method <- methodToApply
          messartValue =  if(valueForKonf._2.map(_.meteoDataRow).size >= completeness/10) {
             aggregateAccordingToMethod(method, valueForKonf._2.map(_.meteoDataRow.valueOfMeasurement))
          } else BigDecimal(-9999)
        } yield messartValue
        val aggregatedValueToEnter = valueForKonf._2.headOption.map(_.meteoDataRow.copy(valueOfMeasurement = aggregatedMeasurementValue.getOrElse(BigDecimal(-9999))))
        aggregatedValueToEnter
      })
      aggregatedMessartValuesForEachConfig
    })




    /*val allMessWerts: Seq[MeasurementParameter] = meteoService.getAllMessArts
    val allStationConfigs: List[StationConfiguration] = meteoService.getStatKonfForStation().filter(sk => allMessWerts.map(_.code).contains( sk.measurementParam))

    val allRowsToBeInserted = ethLaeFileData.flatMap(line => {
      val words = line.split(",")
      val date = formatCR1000Date.withZone(DateTimeZone.UTC).parseDateTime(words(0).replace("\"", ""))
      val recordNumber = NumberParser.parseNumber(words(1))
      val stationNumber =  Some(124)
      val projectNumber =  projNrForFile
      val periode = Some(10)

      val valuesToBeInserted =  for {
          statNr <- stationNumber
          projNr <- projectNumber
          period <- periode
          messWerts = allMessWerts.filter(mt => mt.period == period)
          statConfig: Seq[StationConfiguration] = allStationConfigs.filter(sk => (sk.station == statNr && messWerts.map(_.code).contains(sk.measurementParam) && sk.projectNr.contains(projNr))).sortBy(_.folgeNr)
          folgnr: Seq[(Option[Int], Int, Int)] =  statConfig.map(sk => (sk.folgeNr, sk.measurementParam,sk.configNumber))
          elements = words.map(_.replace("\"", "")).drop(2).zipWithIndex
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
    meteoService.insertMeteoDataETHLae(allRowsToBeInserted)*/
    None
  }

  private def getMappingOfFolgeNrToMessArt(confForStation: List[MeteoStationConfiguration]) = {
    confForStation.map(cf => (cf.folgeNr.getOrElse(-1), cf.messArt)).sortBy(_._1)
  }

  private def aggregateAccordingToMethod(method: String, measurements: List[BigDecimal]): BigDecimal = {
    method match {
      case "avg" => (measurements.sum/measurements.size)
      case "sum" => measurements.sum
      case _ => BigDecimal(-9999)
    }

  }

}

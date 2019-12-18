package models.domain.meteorology.ethlaegeren.parser

import models.domain._
import models.domain.meteorology.ethlaegeren.domain.{MeasurementParameter, StationConfiguration}
import models.services.{MeteoService, MeteorologyService}
import models.util.StringToDate.{formatCR1000Date, formatOzoneDate}
import models.util.{NumberParser, StringToDate}
import org.joda.time.{DateTime, DateTimeZone}
import schedulers.{SpecialParamKonfig, StationKonfig}

import scala.collection.immutable


object ETHLaeFileParser {

  def parseAndSaveData(timestampToWriteData: DateTime, nrOfLines: Int, ethLaeFileDataLines: List[String], meteoService: MeteoService, fileName: String, statKonfig: StationKonfig, specialParamKonfig: Seq[SpecialParamKonfig]) = {

    val statNr = statKonfig.statNr

    val projects: Seq[Int] = statKonfig.projs.map(_.projNr)

    val allMessWerts: Seq[MessArtRow] = meteoService.getAllMessArts.filter(mArt => mArt.messProjNr match {
      case Some(x) => projects.contains(x)
      case None => false
    })


    val allStationConfigs: List[MeteoStationConfiguration] = meteoService.getStatKonfForStation().filter(sk => sk.station == statKonfig.statNr).filter(sk => allMessWerts.map(_.code).contains( sk.messArt)).sortBy(_.folgeNr)
     val valuesToBeInserted = ethLaeFileDataLines.map(line => {
        val words = line.split(",")
       val windSpeedCol = words(17)
        val elements = (words :+ windSpeedCol).drop(2).zipWithIndex

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
    val windSpeedValues: scala.collection.immutable.Iterable[((String, Int), BigDecimal)] = valuesToBeInserted.flatten.toList.groupBy(_.meteoDataRow.dateReceived).map(valueMessart => valueMessart._2.map(l => (valueMessart._1,l.meteoDataRow.configuration) -> l.meteoDataRow.valueOfMeasurement)).flatten

    val aggregatedMessartValuesForEachConfig = groupByKonf.flatMap(valueForKonf => {
        val completenessRequired = allStationConfigs.filter(_.configNumber == valueForKonf._1).flatMap(_.completeness).headOption
        val methodToApply = allStationConfigs.filter(_.configNumber == valueForKonf._1).flatMap(_.methode).headOption
        val windSpeedKonfNr = specialParamKonfig.find(_.measurementParameter == "windSpeed").map(_.konfNr)
        val windDirectionKonfNr = specialParamKonfig.find(_.measurementParameter == "windDirection").map(_.konfNr)

      val aggregatedMeasurementValue = for {
          completeness <- completenessRequired
          method <- methodToApply
          windSpeedKonf <- windSpeedKonfNr
          windDirectionKonf <- windDirectionKonfNr
          messartValue =  if(valueForKonf._2.map(_.meteoDataRow).size >= completeness/10) {
             aggregateAccordingToMethod(method, valueForKonf._2.map(_.meteoDataRow.valueOfMeasurement), windSpeedValues, windSpeedKonf, windDirectionKonf)
          } else BigDecimal(-9999)
        } yield messartValue
        val aggregatedValueToEnter = valueForKonf._2.headOption.map(_.meteoDataRow.copy(valueOfMeasurement = aggregatedMeasurementValue.getOrElse(BigDecimal(-9999))))
        aggregatedValueToEnter
      })

    val aggregatedDataLinesToInsert: immutable.Iterable[MeteoDataRowTableInfo] = aggregatedMessartValuesForEachConfig.filter(_.valueOfMeasurement != BigDecimal(-9999)).map(lineToInsert => {
        MeteoDataRowTableInfo(lineToInsert, Some(1), fileName)
      })

    val aggregatedLinesWithTimestamp = aggregatedDataLinesToInsert.map(al => {
      (StringToDate.formatOzoneDate.withZoneUTC().parseDateTime(al.meteoDataRow.dateReceived.stripPrefix("to_date('").replaceAll("'", "").stripSuffix(", DD.MM.YYYY HH24:MI:SS)")), al)
    })

    val maximumDateDataWasRead: Option[DateTime] = meteoService.findMaxMeasurementDateForAStation(statNr).headOption.map(maxDate => StringToDate.formatOzoneDate.withZoneUTC().parseDateTime(maxDate.stripPrefix("to_date('").replaceAll("'", "").stripSuffix(", DD.MM.YYYY HH24:MI:SS)")))
    val filteredAggregatedLinesToInsert: immutable.Iterable[MeteoDataRowTableInfo] = maximumDateDataWasRead match {
      case None => aggregatedDataLinesToInsert
      case Some(maxDate) => aggregatedLinesWithTimestamp.filter(_._1.isAfter(maxDate)).map(_._2)
    }

      meteoService.insertMeteoDataCR1000(filteredAggregatedLinesToInsert.toSeq)
    //meteoService.insertMeteoDataCR1000(aggregatedLinesWithTimestamp.map(_._2).toSeq)
  }

  private def getMappingOfFolgeNrToMessArt(confForStation: List[MeteoStationConfiguration]) = {
    confForStation.map(cf => (cf.folgeNr.getOrElse(-1), cf.messArt)).sortBy(_._1)
  }

  private def aggregateAccordingToMethod(method: String, measurements: List[BigDecimal], windSpeedValues: scala.collection.immutable.Iterable[((String, Int), BigDecimal)], windSpeedKonf: Int, windDirectionKonf: Int): BigDecimal = {
    method match {
      case "avg" =>{
        val sumOfMeasurements = measurements.sum
        if(sumOfMeasurements != BigDecimal(0)) (sumOfMeasurements/ measurements.size).setScale(5, BigDecimal.RoundingMode.HALF_UP) else BigDecimal(0)
      }
      case "sum" => measurements.sum
      case "windFormula" => computeWindDirectionFromMeasurements(measurements, windSpeedValues, windSpeedKonf, windDirectionKonf)
      case "max" => measurements.max
      case _ => {
        val sumOfMeasurements = measurements.sum
        if(sumOfMeasurements != BigDecimal(0)) (sumOfMeasurements/ measurements.size).setScale(5, BigDecimal.RoundingMode.HALF_UP) else BigDecimal(0)
      }
    }
  }

  private def computeWindDirectionFromMeasurements(measurements: List[BigDecimal], windSpeedValues: scala.collection.immutable.Iterable[((String, Int), BigDecimal)], windSpeedKonf: Int, windDirectionKonf: Int): BigDecimal = {
    val sinCosComponents = windSpeedValues.groupBy(_._1._1).map(values => {
      val windSpeed = values._2.find(v => v._1._2 == windSpeedKonf).map(_._2).getOrElse(BigDecimal(-9999))
      val windDirection = values._2.find(v => v._1._2 == windDirectionKonf).map(_._2).getOrElse(BigDecimal(-9999))
      val sinCosW = if(windSpeed != -9999 && windDirection != -9999) {
        val wx = Math.sin(windDirection.toDouble * (3.14/180)) * windSpeed
        val wy = Math.cos(windDirection.toDouble * (3.14/180)) * windSpeed
        (wx, wy)
      } else (BigDecimal(-9999), BigDecimal(-9999))
      sinCosW
    }
    ).toList

    val sumSinW = sinCosComponents.map(_._1).sum
    val sumCosW = sinCosComponents.map(_._2).sum
    val d = if(sumSinW != 0 && sumCosW != 0) {
      (180/3.14) * Math.atan(sumCosW.toDouble/sumSinW.toDouble)
    } else 0

    val wd = if(sumSinW == 0 && sumCosW > 0) 0
    else if(sumSinW == 0 && sumCosW < 0) 180
    else if(sumSinW > 0) 90 - d
    else if(sumSinW < 0) 270 - d
    else -9999
    wd
  }
}

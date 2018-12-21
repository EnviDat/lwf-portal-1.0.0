package models.services

import java.io
import java.math.MathContext

import models.domain._
import models.domain.meteorology.ethlaegeren.parser.ETHLaeFileParser.aggregateAccordingToMethod
import models.repositories.StationAbbrevationsList
import models.util.GroupByOrderedImplicit._
import models.util.StringToDate.formatCR1000Date
import models.util.{CurrentSysDateInSimpleFormat, Joda, StringToDate}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import schedulers.SpecialParamKonfig

import scala.collection.immutable.Range
import scala.collection.parallel.immutable
import scala.collection.parallel.immutable.ParIterable
import scala.math.BigDecimal.RoundingMode





class FileGeneratorMeteoSchweizHEXFixedAggregatedFormat(meteoService: MeteoService) {

  val allOrganisations: Seq[Organisation] = meteoService.getAllOrganisations()
  Logger.info(s"All Organisations where data should be sent out: ${allOrganisations.size}")

  val allStations: Seq[Station] = meteoService.getAllStations
  Logger.info(s"Number of stations found: ${allStations.size}")
  Logger.info(s"Name of stations found are: ${allStations.map(_.stationsName).mkString(",")}")

  val allAbbrevations: List[StationAbbrevations] = meteoService.getAllStatAbbrevations()
  Logger.info(s"Abbrevations for the stations found are: ${allAbbrevations.map(_.kurzName).mkString(",")}")

  val allMessWerts: Seq[OrgStationParamMapping] = meteoService.getAllMessartsForOrgFixedAggFormat
  Logger.info(s"All Messwerts for the stations found are: ${allMessWerts.mkString(",")}")

  val lastTimeDataSentForStations: Seq[MeteoDataFileLogInfo] = meteoService.getLastDataSentInformation() //List(MeteoDataFileLogInfo(190,1,"Rawdata",StringToDate.stringToDateConvert("01-12-2018 00:00:00"),StringToDate.stringToDateConvert("07-12-2018 00:00:00"),0,StringToDate.stringToDateConvert("07-12-2018 00:00:00")))

  Logger.info(s"All information about the station when data was sent out: ${lastTimeDataSentForStations.mkString(",")}")

  val stationOrganisationMappings: Seq[OrganisationStationMapping] = meteoService.getAllOrganisationStationsMappings()
  Logger.info(s"All information about the organisation station configuration: ${stationOrganisationMappings.mkString(",")}")

  val allStationConfigs: List[MeteoStationConfiguration] = meteoService.getStatKonfForStation().filter(sk => allMessWerts.map(_.paramId).contains( sk.messArt))
  Logger.info(s"All Configurations Loaded for the stations found are: ${allStationConfigs.mkString(",")}")

  val fixedFormatHeader = """StationsID, Datum[DD.MM.YYYY HH24:MI:SS"],"""


  def generateFiles(specialParamKonfig: Seq[SpecialParamKonfig]): List[FileInfo] = {
    import org.joda.time.Days
    import Joda._

    val timeStampForFileName = CurrentSysDateInSimpleFormat.dateNow

    allOrganisations.filter(_.organisationNr == 1).flatMap(o => {

      val configuredStationsForOrganisation = stationOrganisationMappings.filter(so => so.orgNr == o.organisationNr && so.shouldSendData == 1 && so.fileFormat == "FixedAggFormat")

      val stationNumbersConfigured = configuredStationsForOrganisation.map(_.statNr)
      val allFilesDataGenerated = allStations.filter(st => stationNumbersConfigured.contains(st.stationNumber)).map(station => {


        val confForStation = allStationConfigs.filter(_.station == station.stationNumber)
        Logger.info(s"All config Loaded for the station: ${confForStation.mkString(",")}")


        val allMessArtsForStation = allMessWerts.filter(messart => messart.orgNr == o.organisationNr && messart.statNr == station.stationNumber).sortBy(_.columnNr)
        val folgeNrForStations = allMessArtsForStation.map(_.columnNr).sorted
        val trailor = allMessArtsForStation.map(_.shortName)
        Logger.debug(s"header line of the file is: ${fixedFormatHeader + trailor.mkString(",")}")

        val abbrevationForStation: String = allAbbrevations.find(_.code == station.stationNumber).map(_.kurzName).getOrElse(StationAbbrevationsList.stationAbbrevationsMeteoSchweiz.find(_.code == station.stationNumber).map(_.kurzName).getOrElse(o.prefix + station.stationsName))
        Logger.info(s"All abbrevations for the station: ${abbrevationForStation.mkString(",")}")

        val mapFolgNrToMessArt: Seq[(Int, Int)] = allMessArtsForStation.map(m => (m.columnNr, m.paramId)).sortBy(_._1)
        Logger.info(s"mapping folgenr to messart details are: ${mapFolgNrToMessArt.mkString("\n")}")

        val lastDateTimeDataWasSent: Option[DateTime] = lastTimeDataSentForStations.find(lt => lt.orgNr == o.organisationNr && lt.stationNr== station.stationNumber).map(_.lastEinfDat)

        val latestMeteoDataForStation: Seq[MeteoDataRow] = meteoService.getLatestMeteoDataToWrite(station.stationNumber, lastDateTimeDataWasSent).sortBy(_.dateReceived)

        //Logger.debug(s"All data Loaded for the station: ${latestMeteoDataForStation.mkString(",")}")
        val dataForRequiredMessartsToBeSentOut: Seq[(DateTime, MeteoDataRow)] = latestMeteoDataForStation.filter(lt => allMessArtsForStation.map(_.paramId).contains(lt.messArt)).map(dt => StringToDate.stringToDateConvert(dt.dateReceived) -> dt).sortBy(_._1)

        val groupedDataHourly =  groupValidMeasurementsForTimeStampsWithOneHour(dataForRequiredMessartsToBeSentOut)

       val valuesToWrite = groupedDataHourly.flatMap(dt => {

        val groupByKonf = dt._2.toList.groupBy(_.configuration)
         val windSpeedValues: scala.collection.immutable.Iterable[((String, Int), BigDecimal)] = dt._2.groupBy(_.dateReceived).map(valueMessart => valueMessart._2.map(l => (valueMessart._1, l.configuration) -> l.valueOfMeasurement)).flatten
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
            messartValue =  if(valueForKonf._2.size == 2) {
              aggregateAccordingToMethod(method, valueForKonf._2.map(_.valueOfMeasurement).toList, windSpeedValues, windSpeedKonf, windDirectionKonf)
            } else BigDecimal(-9999)
          } yield messartValue
          val aggregatedValueToEnter = valueForKonf._2.headOption.map(_.copy(valueOfMeasurement = aggregatedMeasurementValue.getOrElse(BigDecimal(-9999)), dateReceived = StringToDate.formatDate.print(dt._1._1._2).toString))
          aggregatedValueToEnter
        }).toList
          aggregatedMessartValuesForEachConfig
        }).toList



      val groupMeteoDataDauer =
        valuesToWrite.filter(lt => allMessArtsForStation.map(_.paramId).contains(lt.messArt)).map(dt => (StringToDate.stringToDateConvert(dt.dateReceived) -> dt)).groupByOrdered(_._1)

      Logger.debug(s"All data for messarts is: ${groupMeteoDataDauer.map(_._2.mkString("\n")).mkString("\n")}")

      val valuesToBeWritten = getFixedFormatDataLines(groupMeteoDataDauer, mapFolgNrToMessArt, station).toList.sortBy(_.measurementDate)

      val dataHeaderToBeWritten = fixedFormatHeader + trailor.mkString(",")

      val fileName = abbrevationForStation + timeStampForFileName

      val dataLinesToBeWrittenFixedFormat = valuesToBeWritten.map(dl => dl.stationId + "," + dl.measurementTime + "," + dl.measurementValues.map(_.setScale(3,RoundingMode.HALF_DOWN)).mkString(",")).toList

      Logger.debug(s"Data lines to be written for Fixed Format stations: ${dataLinesToBeWrittenFixedFormat.mkString("\n")}")
        import Joda._
        val allDates = latestMeteoDataForStation.map(lt => StringToDate.stringToDateConvert(lt.dateReceived)).sorted
        val numberOfLinesSent = latestMeteoDataForStation.size
        val allEinfDates = latestMeteoDataForStation.map(lt => StringToDate.stringToDateConvert(lt.dateOfInsertion)).sorted

        val fromDate = if(allDates.nonEmpty) allDates.min.toDateTime() else new DateTime()

        val toDate = if(allDates.nonEmpty) allDates.max.toDateTime() else new DateTime()

        val einfDate = if(allDates.nonEmpty) allEinfDates.max.toDateTime() else new DateTime()

        val logInformation = MeteoDataFileLogInfo(station.stationNumber, o.organisationNr, fileName, fromDate, toDate,numberOfLinesSent, new DateTime())

      FileInfo(fileName, dataHeaderToBeWritten, dataLinesToBeWrittenFixedFormat, logInformation)
    })
    Logger.info(s"All data and file names for the stations are: ${allFilesDataGenerated.filter(_.meteoData.nonEmpty).toList.mkString("\n")}")

    allFilesDataGenerated.filter(_.meteoData.nonEmpty).toList
    }).toList
  }

  private def insert[BigDecimal](list: List[BigDecimal], i: Int, value: BigDecimal) :List[BigDecimal]= {
    list.take(i) ++ List(value) ++ list.drop(i)
  }

  private def getFixedFormatDataLines(groupMeteoDataDauer: scala.collection.mutable.Map[DateTime, collection.mutable.LinkedHashSet[(DateTime, MeteoDataRow)]],
                                      mapFolgNrToMessArt: Seq[(Int, Int)],
                                      station: Station
                                        ): Iterable[FixedLinesFormatWithDate] = {
    groupMeteoDataDauer.map(dt => {
       val measurementValues = mapFolgNrToMessArt.map(fl => {
          dt._2.find(m => m._2.messArt == fl._2).map(_._2.valueOfMeasurement).getOrElse(BigDecimal(-999))
        }).toList
      FixedLinesFormatWithDate(station.stationNumber, StringToDate.oracleDateFormat.print(dt._1), measurementValues, dt._1)
      })
    }

  def saveLogInfoOfGeneratedFiles(fileInfos: List[MeteoDataFileLogInfo]) = {
    Logger.info(s"saving log information in database: ${fileInfos.mkString(",")}")
    meteoService.insertLogInformation(fileInfos)
  }

  private def groupValidMeasurementsForTimeStampsWithOneHour(dataForRequiredMessartsToBeSentOut: Seq[(DateTime, MeteoDataRow)]): immutable.ParMap[((DateTime, DateTime), Int), Seq[MeteoDataRow]] = {
    import org.joda.time.Days
    import Joda._
    val minDateInFile = dataForRequiredMessartsToBeSentOut.map(_._1).min
    val maxDateInFile = dataForRequiredMessartsToBeSentOut.map(_._1).max
    val allDays = Iterator.iterate(minDateInFile.withTimeAtStartOfDay()) {
      _.plusMinutes(60)
    }.takeWhile(_.isBefore(maxDateInFile)).toList
    val allDaysTimeStamps = allDays.map(d => (d.minusMinutes(60), d)).sorted
    /*
    val allLinesGrouped: Map[((DateTime, DateTime), Int), List[String]] = allDaysTimeStamps.map(dt => {
      val linesBetweenTimePeriod = validLines.filter(l => {
        val dateTimeStampInLine = formatCR1000Date.withZone(DateTimeZone.UTC).parseDateTime(l.split(",")(0).replace("\"", ""))
        dateTimeStampInLine.isAfter(dt._1) && (dateTimeStampInLine.isBefore(dt._2) || dateTimeStampInLine.isEqual(dt._2))
      })
      ((dt, linesBetweenTimePeriod.length), linesBetweenTimePeriod)
    }).toMap
    */
    val allLinesGrouped: immutable.ParMap[((DateTime, DateTime), Int), Seq[MeteoDataRow]] = allDaysTimeStamps.par.map(dt => {
      val linesBetweenTimePeriod = dataForRequiredMessartsToBeSentOut.filter(l => {
        val dateTimeStampInLine = l._1
        dateTimeStampInLine.isAfter(dt._1) && (dateTimeStampInLine.isBefore(dt._2) || dateTimeStampInLine.isEqual(dt._2))
      }).map(_._2)
      Logger.info(s"grouping is done for time stamp ${dt._1.toString() + dt._2.toString()}")
      ((dt, linesBetweenTimePeriod.length), linesBetweenTimePeriod)
    }).toMap
    Logger.info("grouping task finished")

    allLinesGrouped

    //.getOrElse(Map.empty[((DateTime, DateTime), Int), List[String]])
  }

  private def aggregateAccordingToMethod(method: String, measurements: List[BigDecimal], windSpeedValues: scala.collection.immutable.Iterable[((String, Int), BigDecimal)], windSpeedKonf: Int, windDirectionKonf: Int): BigDecimal = {
    method match {
      case "avg" => (measurements.sum/measurements.size)
      case "sum" => measurements.sum
      case "windFormula" => computeWindDirectionFromMeasurements(measurements, windSpeedValues, windSpeedKonf, windDirectionKonf)
      case "max" => measurements.max
      case _ => (measurements.sum/measurements.size)    }
  }

  private def computeWindDirectionFromMeasurements(measurements: List[BigDecimal], windSpeedValues: scala.collection.immutable.Iterable[((String, Int), BigDecimal)], windSpeedKonf: Int, windDirectionKonf: Int): BigDecimal = {
   val sinCosComponents = windSpeedValues.groupBy(_._1._1).map(values => {
      val windSpeed = values._2.find(v => v._1._2 == windSpeedKonf).map(_._2).getOrElse(BigDecimal(-9999))
      val windDirection = values._2.find(v => v._1._2 == windDirectionKonf).map(_._2).getOrElse(BigDecimal(-9999))
      val sinCosW = if(windSpeed != -9999 && windDirection != -9999) {
        val wx = Math.sin(windDirection.toDouble * (Math.PI/180)) * windSpeed
        val wy = Math.cos(windDirection.toDouble * (Math.PI/180)) * windSpeed
        (wx, wy)
      } else (BigDecimal(-9999), BigDecimal(-9999))
      sinCosW
    }
    ).toList

    val sumSinW = sinCosComponents.map(_._1).sum
    val sumCosW = sinCosComponents.map(_._2).sum
    val d = if(sumSinW != 0 && sumCosW != 0) {
      (180/Math.PI) * Math.atan(sumCosW.toDouble/sumSinW.toDouble)
    } else 0

    val wd = if(sumSinW == 0 && sumCosW > 0) 0
    else if(sumSinW == 0 && sumCosW < 0) 180
    else if(sumSinW > 0) 90 - d
    else if(sumSinW < 0) 270 - d
    else -9999
   wd
  }

}
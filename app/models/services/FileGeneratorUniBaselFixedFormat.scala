package models.services

import models.domain._
import models.util.GroupByOrderedImplicit._
import models.util.{CurrentSysDateInSimpleFormat, Joda, StringToDate}
import org.joda.time.DateTime
import play.api.Logger

import scala.collection.mutable


class FileGeneratorUniBaselFixedFormat(meteoService: MeteoService) extends FileGenerator {

  val allOrganisations: Seq[Organisation] = meteoService.getAllOrganisations()
  Logger.info(s"All Organisations where data should be sent out: ${allOrganisations.size}")

  val allStations: Seq[Station] = meteoService.getAllStations
  Logger.info(s"Number of stations found: ${allStations.size}")
  Logger.info(s"Name of stations found are: ${allStations.map(_.stationsName).mkString(",")}")

  val allAbbrevations: List[StationAbbrevations] = meteoService.getAllStatAbbrevations()
  Logger.info(s"Abbrevations for the stations found are: ${allAbbrevations.map(_.kurzName).mkString(",")}")

  val allMessWerts: Seq[OrgStationParamMapping] = meteoService.getAllMessartsForOrgFixedFormat
  Logger.info(s"All Messwerts for the stations found are: ${allMessWerts.mkString(",")}")

  val lastTimeDataSentForStations: Seq[MeteoDataFileLogInfo] = meteoService.getLastDataSentInformation()
  Logger.info(s"All information about the station when data was sent out: ${lastTimeDataSentForStations.mkString(",")}")

  val stationOrganisationMappings: Seq[OrganisationStationMapping] = meteoService.getAllOrganisationStationsMappings()
  Logger.info(s"All information about the organisation station configuration: ${stationOrganisationMappings.mkString(",")}")

  val allStationConfigs: List[MeteoStationConfiguration] = meteoService.getStatKonfForStation().filter(sk => allMessWerts.map(_.paramId).contains( sk.messArt))
  Logger.info(s"All Configurations Loaded for the stations found are: ${allStationConfigs.mkString(",")}")

  val fixedFormatHeader = """StationsID, Datum[DD.MM.YYYY HH24:MI:SS"],"""


  def generateFiles(): List[FileInfo] = {
    val timeStampForFileName = CurrentSysDateInSimpleFormat.dateNow

    allOrganisations.filter(_.organisationNr == 3).flatMap(o => {

      val configuredStationsForOrganisation = stationOrganisationMappings.filter(so => so.orgNr == o.organisationNr && so.shouldSendData == 1 && so.fileFormat == "FixedFormat")

      val stationNumbersConfigured = configuredStationsForOrganisation.map(_.statNr)
      val allFilesDataGenerated = allStations.filter(st => stationNumbersConfigured.contains(st.stationNumber)).map(station => {


        val confForStation = allStationConfigs.filter(_.station == station.stationNumber)
        Logger.info(s"All config Loaded for the station: ${confForStation.mkString(",")}")


        val allMessArtsForStation = allMessWerts.filter(messart => messart.orgNr == o.organisationNr && messart.statNr == station.stationNumber).toList
        val folgeNrForStations = allMessArtsForStation.map(_.columnNr).sorted
        val trailor = folgeNrForStations.map(fl => allMessWerts.find(_.columnNr == fl).map(_.shortName))
        Logger.debug(s"header line of the file is: ${fixedFormatHeader + trailor.map(_.getOrElse(",")).mkString("\n")}")

        val abbrevationForStation = allAbbrevations.find(_.code == station.stationNumber)
        Logger.info(s"All abbrevations for the station: ${abbrevationForStation.mkString(",")}")

        val mapFolgNrToMessArt: Seq[(Int, Int)] = allMessArtsForStation.map(m => (m.columnNr, m.paramId)).sortBy(_._1)
        Logger.info(s"mapping folgenr to messart details are: ${mapFolgNrToMessArt.mkString("\n")}")

        val lastDateTimeDataWasSent: Option[DateTime] = lastTimeDataSentForStations.find(lt => lt.orgNr == o.organisationNr && lt.stationNr== station.stationNumber).map(_.lastEinfDat)

        val latestMeteoDataForStation = meteoService.getLatestMeteoDataToWrite(station.stationNumber, lastDateTimeDataWasSent).sortBy(_.dateReceived)

        //Logger.debug(s"All data Loaded for the station: ${latestMeteoDataForStation.mkString(",")}")

      val groupMeteoDataDauer: mutable.Map[DateTime, mutable.LinkedHashSet[(DateTime, MeteoDataRow)]] =
        latestMeteoDataForStation.filter(lt => allMessArtsForStation.map(_.paramId).contains(lt.messArt)).map(dt => (StringToDate.stringToDateConvert(dt.dateReceived) -> dt)).groupByOrdered(_._1)

      Logger.debug(s"All data for messarts is: ${groupMeteoDataDauer.map(_._2.mkString("\n")).mkString("\n")}")

      val valuesToBeWritten = getFixedFormatDataLines(groupMeteoDataDauer, mapFolgNrToMessArt, station)

      val dataHeaderToBeWritten = fixedFormatHeader + trailor.map(_.getOrElse(",")).mkString(",")

      val fileName = abbrevationForStation.map(ab =>  ab.kurzName + timeStampForFileName)

      val dataLinesToBeWrittenFixedFormat = valuesToBeWritten.map(dl => dl.stationId + "," + dl.measurementTime + "," + dl.measurementValues.mkString(",")).toList

      Logger.debug(s"Data lines to be written for Fixed Format stations: ${dataLinesToBeWrittenFixedFormat.mkString("\n")}")
        import Joda._
        val allDates = latestMeteoDataForStation.map(lt => StringToDate.stringToDateConvert(lt.dateReceived)).sorted
        val numberOfLinesSent = latestMeteoDataForStation.size
        val allEinfDates = latestMeteoDataForStation.map(lt => StringToDate.stringToDateConvert(lt.dateOfInsertion)).sorted

        val fromDate = if(allDates.nonEmpty) allDates.min.toDateTime() else new DateTime()

        val toDate = if(allDates.nonEmpty) allDates.max.toDateTime() else new DateTime()

        val einfDate = if(allDates.nonEmpty) allEinfDates.max.toDateTime() else new DateTime()

        val logInformation = MeteoDataFileLogInfo(station.stationNumber, o.organisationNr, fileName.getOrElse(o.prefix + station.stationsName + timeStampForFileName).toString, fromDate, toDate,numberOfLinesSent, new DateTime())

      FileInfo(fileName.getOrElse(o.prefix + station.stationsName + timeStampForFileName).toString, dataHeaderToBeWritten, dataLinesToBeWrittenFixedFormat, logInformation)
    })
    Logger.info(s"All data and file names for the stations are: ${allFilesDataGenerated.filter(_.meteoData.nonEmpty).toList.mkString("\n")}")

    allFilesDataGenerated.filter(_.meteoData.nonEmpty).toList
    }).toList
  }

  private def insert[BigDecimal](list: List[BigDecimal], i: Int, value: BigDecimal) :List[BigDecimal]= {
    list.take(i) ++ List(value) ++ list.drop(i)
  }

  private def getFixedFormatDataLines(groupMeteoDataDauer: mutable.Map[DateTime, mutable.LinkedHashSet[(DateTime, MeteoDataRow)]],
                                      mapFolgNrToMessArt: Seq[(Int, Int)],
                                         station: Station
                                        ): mutable.Iterable[FixedLinesFormat] = {
    groupMeteoDataDauer.map(dt => {
       val measurementValues = mapFolgNrToMessArt.map(fl => {
          dt._2.find(m => m._2.messArt == fl._2).map(_._2.valueOfMeasurement).getOrElse(BigDecimal(-999))
        }).toList
        FixedLinesFormat(station.stationNumber, StringToDate.oracleDateFormat.print(dt._1), measurementValues)
      })
    }

  def saveLogInfoOfGeneratedFiles(fileInfos: List[MeteoDataFileLogInfo]) = {
    Logger.info(s"saving log information in database: ${fileInfos.mkString(",")}")
    meteoService.insertLogInformation(fileInfos)
  }

}
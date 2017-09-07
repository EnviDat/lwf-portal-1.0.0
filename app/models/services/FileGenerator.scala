package models.services

import models.domain._
import models.util.GroupByOrderedImplicit._
import models.util.{CurrentSysDateInSimpleFormat, Joda, StringToDate}
import org.joda.time.DateTime
import play.api.Logger
import java.text.SimpleDateFormat

import scala.collection.mutable

trait FileGenerator {
  def generateFiles(): List[FileInfo]
  def saveLogInfoOfGeneratedFiles(fileInfos: List[MeteoDataFileLogInfo])
}

class FileGeneratorFromDB(meteoService: MeteoService) extends FileGenerator {

  val allOrganisations: Seq[Organisation] = meteoService.getAllOrganisations()
  Logger.info(s"All Organisations where data should be sent out: ${allOrganisations.size}")

  val allStations: Seq[Station] = meteoService.getAllStations
  Logger.info(s"Number of stations found: ${allStations.size}")
  Logger.info(s"Name of stations found are: ${allStations.map(_.stationsName).mkString(",")}")

  val allAbbrevations: List[StationAbbrevations] = meteoService.getAllStatAbbrevations()
  Logger.info(s"Abbrevations for the stations found are: ${allAbbrevations.map(_.kurzName).mkString(",")}")

  val allMessWerts: Seq[MessArtRow] = meteoService.getAllMessArts.filter(mw => {
    val mProjNr = mw.messProjNr.getOrElse(0)
    mProjNr == 1 || mProjNr == 4 || mProjNr == 5
  })
  Logger.info(s"All Messwerts for the stations found are: ${allMessWerts.mkString(",")}")

  val lastTimeDataSentForStations: Seq[MeteoDataFileLogInfo] = meteoService.getLastDataSentInformation()
  Logger.info(s"All information about the station when data was sent out: ${lastTimeDataSentForStations.mkString(",")}")

  val stationOrganisationMappings: Seq[OrganisationStationMapping] = meteoService.getAllOrganisationStationsMappings()
  Logger.info(s"All information about the organisation station configuration: ${stationOrganisationMappings.mkString(",")}")


  val listOfCR1000Stations: Seq[Int] = stationOrganisationMappings.filter(_.fileFormat == "CR1000").map(_.statNr)

  val allStationConfigs: List[MeteoStationConfiguration] = meteoService.getStatKonfForStation().filter(sk => allMessWerts.map(_.code).contains( sk.messArt))
  Logger.info(s"All Configurations Loaded for the stations found are: ${allStationConfigs.mkString(",")}")

  val cr10Header = """Messperiode[Minuten], StationsID, ProjektID, Jahr[JJJJ], Tag im Jahr[TTT], Uhrzeit(UTC)[HH24]"""
  val toa5Header = """Datum[JJJJ.MM.DD HH24:MI:SS"], RecordID, StationsID, ProjektID, Messperiode[Minuten],"""

  def generateFiles(): List[FileInfo] = {
    val timeStampForFileName = CurrentSysDateInSimpleFormat.dateNow

    allOrganisations.flatMap(o => {

      val configuredStationsForOrganisation = stationOrganisationMappings.filter(so => so.orgNr == o.organisationNr && so.shouldSendData == 1)

      val stationNumbersConfigured = configuredStationsForOrganisation.map(_.statNr)
      val allFilesDataGenerated = allStations.filter(st => stationNumbersConfigured.contains(st.stationNumber)).map(station => {


        val confForStation = allStationConfigs.filter(_.station == station.stationNumber)
        Logger.info(s"All config Loaded for the station: ${confForStation.mkString(",")}")

        val folgeNrForStations = getFolegNrForStations(confForStation)
        val trailor = folgeNrForStations.map(fl => allMessWerts.find(_.code == fl._2).map(_.text))
        Logger.info(s"header line of the file is: ${cr10Header + trailor.map(_.getOrElse(",")).mkString("\n")}")

        val abbrevationForStation = allAbbrevations.find(_.code == station.stationNumber)
        Logger.info(s"All abbrevations for the station: ${abbrevationForStation.mkString(",")}")

        val sortedMessArts: mutable.Map[Int, mutable.LinkedHashSet[MessArtRow]] = allMessWerts.filter(ma => confForStation.map(_.messArt).contains(ma.code)).groupByOrdered(_.pDauer)

        Logger.info(s"All messarts for the station: ${sortedMessArts.mkString(",")}")

        val mapFolgNrToMessArt: Seq[(Int, Int)] = getMappingOfFolgeNrToMessArt(confForStation)
        Logger.info(s"mapping folgenr to messart details are: ${mapFolgNrToMessArt.mkString("\n")}")

        val lastDateTimeDataWasSent: Option[DateTime] = lastTimeDataSentForStations.find(lt => lt.orgNr == o.organisationNr && lt.stationNr== station.stationNumber).map(_.lastEinfDat)

        val latestMeteoDataForStation = meteoService.getLatestMeteoDataToWrite(station.stationNumber, lastDateTimeDataWasSent).sortBy(_.dateReceived)

        //Logger.debug(s"All data Loaded for the station: ${latestMeteoDataForStation.mkString(",")}")

      val groupMeteoDataDauer = sortedMessArts.map(smt => {
        (smt._1,
        latestMeteoDataForStation.filter(lt => smt._2.map(_.code).contains(lt.messArt)).groupByOrdered(mdat => StringToDate.stringToDateConvert(mdat.dateReceived)))
      })
      Logger.info(s"All data for messarts is: ${groupMeteoDataDauer.map(_._2.mkString("\n")).mkString("\n")}")

      val valuesToBeWrittenForCR10 = if(!listOfCR1000Stations.contains(station.stationNumber)) getCR10FormattedDataLines(groupMeteoDataDauer, sortedMessArts, station, mapFolgNrToMessArt).flatten else Seq()
      val valuesToBeWrittenForCR1000 = if(listOfCR1000Stations.contains(station.stationNumber)) getTR05FormattedDataLines(groupMeteoDataDauer, sortedMessArts, station, mapFolgNrToMessArt).flatten else Seq()

      Logger.info(s"All data To be written for the  CR1000 stations: ${valuesToBeWrittenForCR1000.toList.sortBy(_.duration).mkString("\n")}")
      Logger.info(s"All data To be written for the  CR10 stations: ${valuesToBeWrittenForCR10.toList.sortBy(_.duration).mkString("\n")}")

      val dataHeaderToBeWritten =
        if(listOfCR1000Stations.contains(station.stationNumber))
            toa5Header + trailor.map(_.getOrElse(",")).mkString(",")
          else
            cr10Header + trailor.map(_.getOrElse(",")).mkString(",")

      val fileName = abbrevationForStation.map(ab =>  ab.kurzName + timeStampForFileName)

      val cr1000DataSortedDuration = valuesToBeWrittenForCR1000.toList
      val cr10DataSortedDuration = valuesToBeWrittenForCR10.toList.sortBy(_.duration)

      val dataLinesToBeWrittenCR1000 = cr1000DataSortedDuration.map(dl => dl.measurementTime + "," + cr1000DataSortedDuration.indexOf(dl) + "," + dl.stationId + "," + dl.projectId + "," + dl.duration + "," + dl.measurementValues.mkString(","))
      val dataLinesToBeWrittenCR10 = cr10DataSortedDuration.map(dl => dl.duration + "," + dl.stationId + "," + dl.projectId + "," + dl.year + "," + dl.yearToDate + "," + dl.time + "," + dl.measurementValues.mkString(","))

      Logger.info(s"Data lines to be written for CR1000 stations: ${dataLinesToBeWrittenCR1000.mkString("\n")}")
      Logger.info(s"Data lines to be written for CR10x stations: ${dataLinesToBeWrittenCR10.mkString("\n")}")
        import Joda._
        val allDates = latestMeteoDataForStation.map(lt => StringToDate.stringToDateConvert(lt.dateReceived)).sorted
        val numberOfLinesSent = latestMeteoDataForStation.size
        val allEinfDates = latestMeteoDataForStation.map(lt => StringToDate.stringToDateConvert(lt.dateOfInsertion)).sorted

        val fromDate = if(allDates.nonEmpty) allDates.min.toDateTime() else new DateTime()

        val toDate = if(allDates.nonEmpty) allDates.max.toDateTime() else new DateTime()

        val einfDate = if(allDates.nonEmpty) allEinfDates.max.toDateTime() else new DateTime()


        val logInformation = MeteoDataFileLogInfo(station.stationNumber, o.organisationNr, fileName.getOrElse(o.prefix + station.stationsName + timeStampForFileName).toString, fromDate, toDate,numberOfLinesSent, einfDate)

      FileInfo(fileName.getOrElse(o.prefix + station.stationsName + timeStampForFileName).toString, dataHeaderToBeWritten, dataLinesToBeWrittenCR1000 ::: dataLinesToBeWrittenCR10, logInformation)
    })
    Logger.info(s"All data and file names for the stations are: ${allFilesDataGenerated.filter(_.meteoData.nonEmpty).toList.mkString("\n")}")

    allFilesDataGenerated.filter(_.meteoData.nonEmpty).toList
    }).toList
  }

  private def getCR10FormattedDataLines(groupMeteoDataDauer: mutable.Map[Int, mutable.Map[DateTime, mutable.LinkedHashSet[MeteoDataRow]]],
                                        sortedMessArts: mutable.Map[Int, mutable.LinkedHashSet[MessArtRow]],
                                        station: Station,
                                        mapFolgNrToMessArt: Seq[(Int, Int)]) = {
    groupMeteoDataDauer.map(dataLine => {
      dataLine._2.map(dt => {
        Logger.info(s"dataline: ${dt._1}${dt._2.mkString(",")}")
        val messArtFound = sortedMessArts.find(_._1 == dataLine._1)
        Logger.info(s"messartfound: ${messArtFound.mkString(",")}")

        val messartsForOnlyThisDuration = sortedMessArts.filter(_._1 == dataLine._1).values.flatMap(_.map(_.code)).toList
        val numberOfMessarts = messartsForOnlyThisDuration.size

        val folgNrToBeUsed = mapFolgNrToMessArt.filter(fNr => messartsForOnlyThisDuration.contains(fNr._2))
        val folgenrset = folgNrToBeUsed.map(_._1).toSet
        Logger.info(s"folgenumber to be used: ${folgNrToBeUsed}")

        val listSeq  = 1 to folgNrToBeUsed.map(_._1).max
        val missingFolgNr = listSeq.toSet.diff(folgenrset).toList.sorted
        Logger.info(s"missing folgenumber: ${missingFolgNr}")

        val messArtRowFound = messArtFound.flatMap(_._2.find(_.pDauer == dataLine._1).flatMap(_.messProjNr))
        Logger.info(s"messartrowfound: ${messArtRowFound}")

        val messProjNr: Int = messArtRowFound.getOrElse(0)
        val year = dt._1.year().get()
        val yearToDate = dt._1.dayOfYear().get()
        val hourOfDay = dt._1.getHourOfDay
        val minOfTheHour = dt._1.getMinuteOfHour
        val measurementValues = folgNrToBeUsed.map(fl => {
          val valuetowrite = dt._2.find(m => m.messArt == fl._2).map(_.valueOfMeasurement).getOrElse(BigDecimal(-999))
          valuetowrite
        }).toList
        var listVar = measurementValues
        Logger.info(s"list initial ${listVar}")
        val listWithMissingValues = missingFolgNr.map(mNr => {
          val newListWithInsertion = insert(listVar, mNr-1, BigDecimal(-999)) //this magic number is to represent that this data was not read
           listVar = newListWithInsertion
          newListWithInsertion
        })
       val finalMeasurementValues =  if(missingFolgNr.isEmpty) measurementValues else if(listWithMissingValues.size > 1) listWithMissingValues.last else  listWithMissingValues.flatten

        Logger.info(s"fimalMeasurementValues: ${finalMeasurementValues}")

        Cr10LinesFormat(dataLine._1,
            station.stationNumber,
            messProjNr,
            year,
            yearToDate,
            f"${hourOfDay}%02d" + f"${minOfTheHour}%02d",
            finalMeasurementValues
            )
      })
    })
  }

  def insert[BigDecimal](list: List[BigDecimal], i: Int, value: BigDecimal) :List[BigDecimal]= {
    list.take(i) ++ List(value) ++ list.drop(i)
  }

  private def getTR05FormattedDataLines(groupMeteoDataDauer: mutable.Map[Int, mutable.Map[DateTime, mutable.LinkedHashSet[MeteoDataRow]]],
                                        sortedMessArts: mutable.Map[Int, mutable.LinkedHashSet[MessArtRow]],
                                        station: Station,
                                        mapFolgNrToMessArt: Seq[(Int, Int)]) = {
    groupMeteoDataDauer.map(dataLine => {
      dataLine._2.map(dt => {
        val messArtFound = sortedMessArts.find(_._1 == dataLine._1)
        val messArtRowFound = messArtFound.flatMap(_._2.find(_.pDauer == dataLine._1).flatMap(_.messProjNr))
        val messProjNr: Int = messArtRowFound.getOrElse(0)
        val measurementValues = mapFolgNrToMessArt.flatMap(fl => {
          dt._2.find(m => m.messArt == fl._2).map(_.valueOfMeasurement)
        }).toList
          TOA5LinesFormat(dt._1.toString,0,station.stationNumber,messProjNr,dataLine._1, measurementValues)
      })
    })
  }


  private def getMappingOfFolgeNrToMessArt(confForStation: List[MeteoStationConfiguration]) = {
    confForStation.map(cf => (cf.folgeNr.getOrElse(-1), cf.messArt)).sortBy(_._1)
  }

  private def getFolegNrForStations(confForStation: List[MeteoStationConfiguration]): Seq[(Option[Int], Int)] = {
    confForStation.map(conf => (conf.folgeNr, conf.messArt)).sortBy(c => c._1)
  }

  def saveLogInfoOfGeneratedFiles(fileInfos: List[MeteoDataFileLogInfo]) = {
    Logger.info(s"saving log information in database: ${fileInfos.mkString(",")}")
    meteoService.insertLogInformation(fileInfos)
  }

}
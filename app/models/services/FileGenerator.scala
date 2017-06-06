package models.services

import models.domain._
import models.util.GroupByOrderedImplicit._
import models.util.StringToDate
import org.joda.time.DateTime
import play.api.Logger
import java.text.SimpleDateFormat

import scala.collection.mutable



trait FileGenerator {
  def generateFiles(): List[FileInfo]
}

class FileGeneratorFromDB(meteoService: MeteoService) extends FileGenerator {
  val listOfCR1000Stations = List(121,120,189,191,35,69,192,122)

  val allStations = meteoService.getAllStations
  Logger.debug(s"Number of stations found: ${allStations.size}")
  Logger.debug(s"Name of stations found are: ${allStations.map(_.stationsName).mkString(",")}")
  val allAbbrevations = meteoService.getAllStatAbbrevations()
  Logger.debug(s"Abbrevations for the stations found are: ${allAbbrevations.map(_.kurzName).mkString(",")}")
  val allMessWerts = meteoService.getAllMessArts.filter(mw => {
    val mProjNr = mw.messProjNr.getOrElse(0)
    (mProjNr == 1 || mProjNr == 4 || mProjNr == 5)
  })
  Logger.debug(s"All Messwerts for the stations found are: ${allMessWerts.mkString(",")}")

  val allStationConfigs = meteoService.getStatKonfForStation().filter(sk => allMessWerts.map(_.code).contains( sk.messArt))
  Logger.debug(s"All Configurations Loaded for the stations found are: ${allStationConfigs.mkString(",")}")

  val cr10Header = """Messperiode[Minuten], StationsID, ProjektID, Jahr[JJJJ], Tag im Jahr[TTT], Uhrzeit(UTC)[HH24]"""
  val toa5Header = """Datum[JJJJ.MM.DD HH24:MI:SS"], RecordID, StationsID, ProjektID, Messperiode[Minuten],"""

  def generateFiles() = {

    val allFilesDataGenerated = allStations.map(station => {
      val confForStation = allStationConfigs.filter(_.station == station.stationNumber)
      Logger.debug(s"All config Loaded for the station: ${confForStation.mkString(",")}")

      val folgeNrForStations = getFolegNrForStations(confForStation)
      val trailor = folgeNrForStations.map(fl => allMessWerts.find(_.code == fl._2).map(_.text))
      Logger.debug(s"header line of the file is: ${cr10Header + trailor.map(_.getOrElse(",")).mkString("\n")}")

      val mapFolgNrToMessArt: Seq[(Int, Int)] = getMappingOfFolgeNrToMessArt(confForStation)
      Logger.debug(s"mapping folgenr to messart details are: ${mapFolgNrToMessArt.mkString("\n")}")

      val abbrevationForStation = allAbbrevations.find(_.code == station.kurzNameCode.getOrElse("NoMatch"))
      Logger.debug(s"All abbrevations for the station: ${abbrevationForStation.mkString(",")}")

      val sortedMessArts: mutable.Map[Int, mutable.LinkedHashSet[MessArtRow]] = allMessWerts.filter(ma => confForStation.map(_.messArt).contains(ma.code)).groupByOrdered(_.pDauer)

      Logger.debug(s"All messarts for the station: ${sortedMessArts.mkString(",")}")

      val latestMeteoDataForStation = meteoService.getLatestMeteoDataToWrite(station.stationNumber).sortBy(_.dateReceived)
      //Logger.debug(s"All data Loaded for the station: ${latestMeteoDataForStation.mkString(",")}")

      val groupMeteoDataDauer = sortedMessArts.map(smt => {
        (smt._1,
        latestMeteoDataForStation.filter(lt => smt._2.map(_.code).contains(lt.messArt)).groupByOrdered(mdat => StringToDate.stringToDateConvert(mdat.dateReceived)))
      })
      Logger.debug(s"All data for messarts is: ${groupMeteoDataDauer.map(_._2.mkString("\n")).mkString("\n")}")

      val valuesToBeWrittenForCR10 = if(!listOfCR1000Stations.contains(station.stationNumber)) getCR10FormattedDataLines(groupMeteoDataDauer, sortedMessArts, station, mapFolgNrToMessArt).flatten else Seq()
      val valuesToBeWrittenForCR1000 = if(listOfCR1000Stations.contains(station.stationNumber)) getTR05FormattedDataLines(groupMeteoDataDauer, sortedMessArts, station, mapFolgNrToMessArt).flatten else Seq()

      Logger.debug(s"All data To be written for the  CR1000 stations: ${valuesToBeWrittenForCR1000.toList.sortBy(_.duration).mkString("\n")}")
      Logger.debug(s"All data To be written for the  CR10 stations: ${valuesToBeWrittenForCR10.toList.sortBy(_.duration).mkString("\n")}")

      val dataHeaderToBeWritten =
        if(listOfCR1000Stations.contains(station.stationNumber))
            cr10Header + trailor.map(_.getOrElse(",")).mkString(",")
          else
            toa5Header + trailor.map(_.getOrElse(",")).mkString(",")

      val fileName = abbrevationForStation.map(_.kurzName +
      new SimpleDateFormat("yyyyMMddHHmmss").format(new  java.util.Date()))

      val cr1000DataSortedDuration = valuesToBeWrittenForCR1000.toList
      val cr10DataSortedDuration = valuesToBeWrittenForCR10.toList.sortBy(_.duration)

      val dataLinesToBeWrittenCR1000 = cr1000DataSortedDuration.map(dl => dl.measurementTime + "," + cr1000DataSortedDuration.indexOf(dl) + "," + dl.stationId + "," + dl.projectId + "," + dl.duration + "," + dl.measurementValues.mkString(","))
      val dataLinesToBeWrittenCR10 = cr10DataSortedDuration.map(dl => dl.duration + "," + dl.stationId + "," + dl.projectId + "," + dl.year + "," + dl.yearToDate + "," + dl.time + "," + dl.measurementValues.mkString(","))

      Logger.debug(s"Data lines to be written for CR1000 stations: ${dataLinesToBeWrittenCR1000.mkString("\n")}")
      Logger.debug(s"Data lines to be written for CR1000 stations: ${dataLinesToBeWrittenCR10.mkString("\n")}")

      FileInfo(fileName.getOrElse("NewFile").toString, dataHeaderToBeWritten, dataLinesToBeWrittenCR1000 ::: dataLinesToBeWrittenCR10)
    })
    Logger.debug(s"All data and file names for the stations are: ${allFilesDataGenerated.filter(_.meteoData.nonEmpty).toList.mkString("\n")}")

    allFilesDataGenerated.filter(_.meteoData.nonEmpty).toList
  }

  private def getCR10FormattedDataLines(groupMeteoDataDauer: mutable.Map[Int, mutable.Map[DateTime, mutable.LinkedHashSet[MeteoDataRow]]],
                                        sortedMessArts: mutable.Map[Int, mutable.LinkedHashSet[MessArtRow]],
                                        station: Station,
                                        mapFolgNrToMessArt: Seq[(Int, Int)]) = {
    groupMeteoDataDauer.map(dataLine => {
      dataLine._2.map(dt => {
        val messArtFound = sortedMessArts.find(_._1 == dataLine._1)
        val messArtRowFound = messArtFound.flatMap(_._2.find(_.pDauer == dataLine._1).flatMap(_.messProjNr))
        val messProjNr: Int = messArtRowFound.getOrElse(0)
        val year = dt._1.year().get()
        val yearToDate = dt._1.dayOfYear().get()
        val hourOfDay = dt._1.getHourOfDay
        val minOfTheHour = dt._1.getMinuteOfHour
        val measurementValues = mapFolgNrToMessArt.flatMap(fl => {
          dt._2.find(m => m.messArt == fl._2).map(_.valueOfMeasurement)
        }).toList
          Cr10LinesFormat(dataLine._1,
            station.stationNumber,
            messProjNr,
            year,
            yearToDate,
            f"${hourOfDay}%02d" + f"${minOfTheHour}%02d",
            measurementValues
            )
      })
    })
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

  private def getFolegNrForStations(confForStation: List[MeteoStationConfiguration]) = {
    confForStation.map(conf => (conf.folgeNr, conf.messArt)).sortBy(c => c._1)
  }
}
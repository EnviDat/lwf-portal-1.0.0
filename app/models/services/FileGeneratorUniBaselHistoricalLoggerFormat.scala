package models.services

import models.domain._
import models.domain.unibasel.UniBaselStationAbbrevations
import models.util.GroupByOrderedImplicit._
import models.util.{CurrentSysDateInSimpleFormat, Joda, StringToDate}
import org.joda.time.DateTime
import play.api.Logger

import scala.collection.mutable

class FileGeneratorUniBaselHistoricalLoggerFormat(meteoService: MeteoService) extends FileGenerator {
  val listOfProjects = UniBaselStationAbbrevations.listOfProjects
  val allOrganisations: Seq[Organisation] = meteoService.getAllOrganisations()
  Logger.info(s"All Organisations where data should be sent out: ${allOrganisations.size}")

  val allStations: Seq[Station] = meteoService.getAllStations
  Logger.info(s"Number of stations found: ${allStations.size}")
  Logger.info(s"Name of stations found are: ${allStations.map(_.stationsName).mkString(",")}")

  val allAbbrevations: List[StationAbbrevations] = UniBaselStationAbbrevations.listOfAllStationsAndAbbrvations //meteoService.getAllStatAbbrevations()
  Logger.info(s"Abbrevations for the stations found are: ${allAbbrevations.map(_.kurzName).mkString(",")}")

  val allMessArts: Seq[MessArtRow] = meteoService.getAllMessArts
  Logger.info(s"All Messwerts for the stations found are: ${allMessArts.mkString(",")}")

  val lastTimeDataSentForStations: Seq[MeteoDataFileLogInfo] = meteoService.getLastDataSentInformation()

  Logger.info(s"All information about the station when data was sent out: ${lastTimeDataSentForStations.mkString(",")}")

  val stationOrganisationMappings: Seq[OrganisationStationMapping] = meteoService.getAllOrganisationStationsMappings()
  Logger.info(s"All information about the organisation station configuration: ${stationOrganisationMappings.mkString(",")}")


  val listOfCR1000Stations: Seq[Int] = stationOrganisationMappings.filter(_.fileFormat == "CR1000").map(_.statNr)

  val allStationConfigs: List[MeteoStationConfiguration] = meteoService.getStatKonfForStation().filter(sk => allMessArts.filter(_.messProjNr.isDefined).filter(m => listOfProjects.contains(m.messProjNr.get)).map(_.code).contains(sk.messArt))
  Logger.info(s"All Configurations Loaded for the stations found are: ${allStationConfigs.mkString(",")}")

  val cr10Header = """Messperiode[Minuten], StationsID, ProjektID, Jahr[JJJJ], Tag im Jahr[TTT], Uhrzeit(UTC)[HH24]"""
  val toa5Header = """Datum[JJJJ-MM-DDTHH24:MI:SS.FFFZ],StationsID,ProjektID,Messperiode[Minuten],"""

  def generateFiles(): List[FileInfo] = {
    listOfProjects.flatMap(mprojNr => {

      val allMessWerts: Seq[MessArtRow] = allMessArts.filter(mw => {
        val mProjNr = mw.messProjNr.getOrElse(0)
        mProjNr == mprojNr
      })
      allOrganisations.filter(_.organisationNr == UniBaselStationAbbrevations.organisationNr).flatMap(o => {

        val configuredStationsForOrganisation = stationOrganisationMappings.filter(so => so.orgNr == o.organisationNr && so.shouldSendData == 1 && so.statNr != 192) //To Do: change this to 1

        val stationNumbersConfigured = configuredStationsForOrganisation.map(_.statNr)
        val allFilesDataGenerated = allStations.filter(st => stationNumbersConfigured.contains(st.stationNumber)).flatMap(station => {
          val confForStation = allStationConfigs.filter(_.station == station.stationNumber)
          Logger.info(s"All config Loaded for the station: ${confForStation.mkString(",")}")

          val messartsGroupedByDuration: Map[Int, Seq[MessArtRow]] = allMessWerts.filter(m => confForStation.map(_.messArt).contains(m.code)).groupBy(_.pDauer)
          Logger.info(s"All messarts for station for project for durations: ${messartsGroupedByDuration.mkString(",")}")
          val allEinfDatesForStation = meteoService.getAllDaysBetweenDates(StringToDate.stringToDateConvert("01-01-2018 00:00:00"), StringToDate.stringToDateConvert("12-12-2018 00:00:00"))
          allEinfDatesForStation.flatMap(einfDatForData => {
            messartsGroupedByDuration.map(sortedMessarts => {


              val t = confForStation.filter(confSk => sortedMessarts._2.map(_.code).contains(confSk.messArt))
              val folgeNrForStations = getFolegNrForStations(confForStation.filter(confSk => sortedMessarts._2.map(_.code).contains(confSk.messArt)))
              val trailor = folgeNrForStations.map(fl => sortedMessarts._2.find(m => m.code == fl._2._2 && m.messProjNr.contains(mprojNr)).map(m => m.text + "[" + m.einheit + "]" + "(" + fl._2._1 + ")"))
              Logger.debug(s"header line of the file is: ${cr10Header + trailor.map(_.getOrElse(",")).mkString("\n")}")

              val abbrevationForStation = allAbbrevations.find(_.code == station.stationNumber)
              Logger.info(s"All abbrevations for the station: ${abbrevationForStation.mkString(",")}")

              //val sortedMessArts: mutable.Map[Int, mutable.LinkedHashSet[MessArtRow]] = sortedMessarts._2.filter(ma => confForStation.map(_.messArt).contains(ma.code) && ma.messProjNr.contains(mprojNr)).groupByOrdered(_.pDauer)

              val mapFolgNrToMessArt: Seq[(Int, (Int, Int))] = getMappingOfFolgeNrToMessArt(confForStation.filter(confSk => sortedMessarts._2.map(_.code).contains(confSk.messArt)))
              Logger.info(s"mapping folgenr to messart details are: ${mapFolgNrToMessArt.mkString("\n")}")


              val lastDateTimeDataWasSent: Option[DateTime] = lastTimeDataSentForStations.filter(_.orgNr == UniBaselStationAbbrevations.organisationNr).find(lt => lt.orgNr == o.organisationNr && lt.stationNr == station.stationNumber).map(_.lastEinfDat)

              val latestMeteoDataForStation: Seq[MeteoDataRow] = meteoService.getLastMeteoDataForStationBetweenDates(station.stationNumber, einfDatForData._1, einfDatForData._2).sortBy(_.dateReceived)

              //Logger.debug(s"All data Loaded for the station: ${latestMeteoDataForStation.mkString(",")}")

              val meteoDataForDuration: mutable.Map[DateTime, mutable.LinkedHashSet[MeteoDataRow]] =
                latestMeteoDataForStation.filter(lt => sortedMessarts._2.map(_.code).contains(lt.messArt)).groupByOrdered(mdat => StringToDate.stringToDateConvert(mdat.dateReceived))
              if(meteoDataForDuration.nonEmpty) {
                //val valuesToBeWrittenForCR10 = if(!listOfCR1000Stations.contains(station.stationNumber)) getCR10FormattedDataLines(meteoDataForDuration, sortedMessarts, station, mapFolgNrToMessArt).flatten else Seq()
                val valuesToBeWrittenForCR1000 = if (listOfCR1000Stations.contains(station.stationNumber)) getTR05FormattedDataLines(meteoDataForDuration, sortedMessarts._2, station, mapFolgNrToMessArt, sortedMessarts._1, mprojNr) else Seq()

                //Logger.debug(s"All data To be written for the  CR1000 stations: ${valuesToBeWrittenForCR1000.toList.sortBy(_.measurementTime).mkString("\n")}")

                val dataHeaderToBeWritten =
                  if (listOfCR1000Stations.contains(station.stationNumber))
                    toa5Header + trailor.map(_.getOrElse(",")).mkString(",")
                  else
                    cr10Header + trailor.map(_.getOrElse(",")).mkString(",")

                val timeStampForFileName = CurrentSysDateInSimpleFormat.changeFormatOfDateForSeparators(einfDatForData._1) + "_" + CurrentSysDateInSimpleFormat.changeFormatOfDateForSeparators(einfDatForData._2)
                /*
            if(latestMeteoDataForStation.map(_.dateOfInsertion).nonEmpty) {
              CurrentSysDateInSimpleFormat.changeFormatDateTimeForFileName(StringToDate.stringToDateConvert(latestMeteoDataForStation.map(_.dateOfInsertion).max))
            } else {
              CurrentSysDateInSimpleFormat.dateNow

            }*/

                val durationKey: String = UniBaselStationAbbrevations.mappingduration.getOrElse(sortedMessarts._1, sortedMessarts._1.toString)
                val projectNrKey = UniBaselStationAbbrevations.mappingProjNr.getOrElse(mprojNr, mprojNr)

                val cr1000DataSortedDuration = valuesToBeWrittenForCR1000.toList
                //val cr10DataSortedDuration = valuesToBeWrittenForCR10.toList.sortBy(_.duration)

                val dataLinesToBeWrittenCR1000 = cr1000DataSortedDuration.map(dl => dl.measurementTime + "," +  dl.stationId + "," + dl.projectId + "," + dl.duration + "," + dl.measurementValues.mkString(","))
                //val dataLinesToBeWrittenCR10 = cr10DataSortedDuration.map(dl => dl.duration + "," + dl.stationId + "," + dl.projectId + "," + dl.year + "," + dl.yearToDate + "," + dl.time + "," + dl.measurementValues.mkString(","))

                Logger.debug(s"Data lines to be written for CR1000 stations: ${dataLinesToBeWrittenCR1000.mkString("\n")}")
                //Logger.debug(s"Data lines to be written for CR10x stations: ${dataLinesToBeWrittenCR10.mkString("\n")}")
                import Joda._
                val allDates = latestMeteoDataForStation.map(lt => StringToDate.stringToDateConvert(lt.dateReceived)).sorted
                val numberOfLinesSent = latestMeteoDataForStation.size
                val allEinfDates = latestMeteoDataForStation.map(lt => StringToDate.stringToDateConvert(lt.dateOfInsertion)).sorted

                val fromDate = if (allDates.nonEmpty) allDates.min.toDateTime() else new DateTime()

                val toDate = if (allDates.nonEmpty) allDates.max.toDateTime() else new DateTime()

                val einfDate = if (allDates.nonEmpty) allEinfDates.max.toDateTime() else new DateTime()
                val minDate = if (meteoDataForDuration.size > 0) meteoDataForDuration.map(_._1).min else fromDate
                val maxDate = if (meteoDataForDuration.size > 0) meteoDataForDuration.map(_._1).max else toDate
                val fileName = abbrevationForStation.map(ab => ab.kurzName + "_" + projectNrKey + "_" + durationKey + "_" + CurrentSysDateInSimpleFormat.changeFormatDateTimeForFileNameWithUTC(minDate) + "_" + CurrentSysDateInSimpleFormat.changeFormatDateTimeForFileNameWithUTC(maxDate) + "_" + CurrentSysDateInSimpleFormat.dateNow)

                val logInformation = MeteoDataFileLogInfo(station.stationNumber, o.organisationNr, fileName.getOrElse(o.prefix + station.stationsName + timeStampForFileName).toString, fromDate, toDate, numberOfLinesSent, einfDate)

                FileInfo(fileName.getOrElse(o.prefix + station.stationsName + "_" + sortedMessarts._1 + "_" + mprojNr + "_" + timeStampForFileName).toString, dataHeaderToBeWritten, dataLinesToBeWrittenCR1000, logInformation)
              }
              else {
                FileInfo("NoData" + CurrentSysDateInSimpleFormat.changeFormatDateTimeForFileNameWithUTC(new DateTime()), "",List(),MeteoDataFileLogInfo(station.stationNumber, o.organisationNr, "NoData", new DateTime(), new DateTime(), 0, new DateTime()))
              }
            })
          })
        })
        Logger.info(s"All data and file names for the stations are: ${allFilesDataGenerated.filter(_.meteoData.nonEmpty).toList.mkString("\n")}")

        allFilesDataGenerated.toList
      }).toList
    })
  }

  private def getCR10FormattedDataLines(groupMeteoDataDauer: mutable.Map[Int, mutable.Map[DateTime, mutable.LinkedHashSet[MeteoDataRow]]],
                                        sortedMessArts: mutable.Map[Int, mutable.LinkedHashSet[MessArtRow]],
                                        station: Station,
                                        mapFolgNrToMessArt: Seq[(Int, (Int,Int))]) = {
    groupMeteoDataDauer.map(dataLine => {
      dataLine._2.map(dt => {
        Logger.debug(s"dataline: ${dt._1}${dt._2.mkString(",")}")
        val messArtFound = sortedMessArts.find(_._1 == dataLine._1)
        Logger.debug(s"messartfound: ${messArtFound.mkString(",")}")

        val messartsForOnlyThisDuration = sortedMessArts.filter(_._1 == dataLine._1).values.flatMap(_.map(_.code)).toList
        val numberOfMessarts = messartsForOnlyThisDuration.size

        val folgNrToBeUsed = mapFolgNrToMessArt.filter(fNr => messartsForOnlyThisDuration.contains(fNr._2._2))
        val folgenrset = folgNrToBeUsed.map(_._1).toSet
        Logger.debug(s"folgenumber to be used: ${folgNrToBeUsed}")

        val listSeq  = 1 to folgNrToBeUsed.map(_._1).max
        val missingFolgNr = listSeq.toSet.diff(folgenrset).toList.sorted
        Logger.debug(s"missing folgenumber: ${missingFolgNr}")

        val messArtRowFound = messArtFound.flatMap(_._2.find(_.pDauer == dataLine._1).flatMap(_.messProjNr))
        Logger.debug(s"messartrowfound: ${messArtRowFound}")

        val messProjNr: Int = messArtRowFound.getOrElse(0)
        val year = dt._1.year().get()
        val yearToDate = dt._1.dayOfYear().get()
        val hourOfDay = dt._1.getHourOfDay
        val minOfTheHour = dt._1.getMinuteOfHour
        val measurementValues = folgNrToBeUsed.map(fl => {
          val valuetowrite = dt._2.find(m => m.messArt == fl._2._2).map(_.valueOfMeasurement).getOrElse(BigDecimal(-999))
          valuetowrite
        }).toList
        var listVar = measurementValues
        Logger.debug(s"list initial ${listVar}")
        val listWithMissingValues = missingFolgNr.map(mNr => {
          val newListWithInsertion = insert(listVar, mNr-1, BigDecimal(-999)) //this magic number is to represent that this data was not read
          listVar = newListWithInsertion
          newListWithInsertion
        })
        val finalMeasurementValues =  if(missingFolgNr.isEmpty) measurementValues else if(listWithMissingValues.size > 1) listWithMissingValues.last else  listWithMissingValues.flatten

        Logger.debug(s"fimalMeasurementValues: ${finalMeasurementValues}")

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

  private def insert[BigDecimal](list: List[BigDecimal], i: Int, value: BigDecimal) :List[BigDecimal]= {
    list.take(i) ++ List(value) ++ list.drop(i)
  }

  private def getTR05FormattedDataLines(meteoDataForDuration: mutable.Map[DateTime, mutable.LinkedHashSet[MeteoDataRow]],
                                        sortedMessArts: Seq[MessArtRow],
                                        station: Station,
                                        mapFolgNrToMessArt: Seq[(Int, (Int,Int))],
                                        duration: Int,
                                        messProjNr: Int): Seq[TOA5LinesFormatHOB] = {
    meteoDataForDuration.map(dataLine => {

        val measurementValues = mapFolgNrToMessArt.map(fl => {
          val messArtFound = dataLine._2.map(_.configuration).find(_ == fl._2._1)
          dataLine._2.find(dt => messArtFound.contains(dt.configuration)).map(_.valueOfMeasurement).getOrElse(BigDecimal(-999))
        }).toList
      TOA5LinesFormatHOB(dataLine._1.toString,0,UniBaselStationAbbrevations.listOfAllStationsAndAbbrvations.find(_.code == station.stationNumber).map(_.kurzName).getOrElse(station.stationNumber.toString),messProjNr,duration, measurementValues)
    }).toList
  }


  private def getMappingOfFolgeNrToMessArt(confForStation: List[MeteoStationConfiguration]): Seq[(Int, (Int, Int))] = {
    confForStation.map(cf => (cf.folgeNr.getOrElse(-1), (cf.configNumber,cf.messArt))).sortBy(_._1)
  }

  private def getFolegNrForStations(confForStation: List[MeteoStationConfiguration]): Seq[(Option[Int], (Int, Int))] = {
    confForStation.map(conf => (conf.folgeNr, (conf.configNumber,conf.messArt))).sortBy(c => c._1)
  }

  def saveLogInfoOfGeneratedFiles(fileInfos: List[MeteoDataFileLogInfo]) = {
    Logger.info(s"saving log information in database: ${fileInfos.mkString(",")}")
    meteoService.insertLogInformation(fileInfos)
  }

}
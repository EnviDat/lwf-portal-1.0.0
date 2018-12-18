package models.services

import java.io
import java.time.LocalDateTime
import javax.inject.Inject

import anorm.SqlParser.get
import models.domain.Ozone.{OzoneFileConfig, PassSammData}
import models.domain._
import models.domain.pheno.{BesuchInfo, PhanoFileLevelInfo}
import models.repositories.MeteoDataRepository
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._


class MeteoService @Inject()(meteoRepo: MeteoDataRepository) {
  implicit val stationReader: Reads[Station] = (
    (__ \ "statnr").read[Int] and
      (__ \ "beschr").read[String] and
        (__ \ "statgruppe").readNullable[Int]

    ) (Station.apply _)


  implicit val documentWriter: Writes[Station] = (
    (__ \ "statnr").write[Int] and
      (__ \ "beschr").write[String] and
        (__ \ "statgruppe").writeNullable[Int]
    ) (unlift(Station.unapply))

  implicit val meteoDataReader: Reads[MeteoDataRow] = (
    (__ \ "statnr").read[Int] and
      (__ \ "messart").read[Int] and
      (__ \ "konfnr").read[Int] and
      (__ \ "messdate").read[String] and
      (__ \ "messwert").read[BigDecimal] and
      (__ \ "einfdate").read[String] and
      (__ \ "ursprung").read[Int] and
      (__ \ "manval").readNullable[Int]
    ) (MeteoDataRow.apply _)

  implicit val meteoDataWriter: Writes[MeteoDataRow] = (
    (__ \ "statnr").write[Int] and
      (__ \ "messart").write[Int] and
      (__ \ "konfnr").write[Int] and
      (__ \ "messdate").write[String] and
      (__ \ "messwert").write[BigDecimal] and
      (__ \ "einfdate").write[String] and
      (__ \ "ursprung").write[Int] and
      (__ \ "manval").writeNullable[Int]
    ) (unlift(MeteoDataRow.unapply _))

  def getAllStations =  meteoRepo.findAllStations().toSeq/*{
    val listOfStations = meteoRepo.findAll()
    listOfStations.map(Json.toJson(_)).toString()
  }*/

  def getAllMessArts = meteoRepo.findAllMessArts()

  def getMeteoData(id: Int): Seq[JsValue] =
    {
      val listOfStations = meteoRepo.findMeteoDataForStation(id)
      listOfStations.map(Json.toJson(_))
    }

  def getAllMeteoData(id: Int): Seq[MeteoDataRow] = meteoRepo.findMeteoDataForStation(id)

  def getLatestMeteoData(stationNr: Int, messArtNr: Int): Seq[JsValue] =
  {
    val listOfStations = meteoRepo.findLastestMeteoDataForStation(stationNr, messArtNr)
    listOfStations.map(Json.toJson(_))
  }
  def getLatestMeteoDataToWrite(stationNr: Int, fromTime: Option[DateTime]) = meteoRepo.findLastMeteoDataForStation(stationNr, fromTime)
  def getLastMeteoDataForStationForDate(stationNr: Int, fromTime:  String) = meteoRepo.findLastMeteoDataForStationForDate(stationNr, fromTime)
  def getLastMeteoDataForStationBetweenDates(stationNr: Int, fromTime:  String, toTime: String) = meteoRepo.findLastMeteoDataForStationBetweenDates(stationNr, fromTime, toTime)


  def getLastOneDayOttPulvioDataForStation(stationNr : Int, messart: Int) = meteoRepo.getLastOneDayOttPulvioDataForStation(stationNr, messart)

  def getStatKonfForStation() = meteoRepo.getAllStatKonf()

  def getAllStatAbbrevations() = meteoRepo.getStationAbbrevations()

  def getLastDataSentInformation() = meteoRepo.findLogInfoForDataSentToOrganisations()

  def findAllDaysBetweenLast15Days() = meteoRepo.findAllDaysBetweenLast15Days()

  def getAllOrganisations() = meteoRepo.findAllOrganisations()

  def getAllOrganisationsProjnrConfig() = meteoRepo.findAllOrganisationsAndProjectsConfig()

  def getAllOrganisationStationsMappings() = meteoRepo.findOrganisationStationMapping()

  def insertLogInformation(meteoLogInfos: List[MeteoDataFileLogInfo]) = meteoRepo.insertLogInfoForFilesSent(meteoLogInfos)

  def insertMeteoDataCR1000(meteoData: Seq[MeteoDataRowTableInfo]) = meteoRepo.insertCR1000MeteoDataForFilesSent(meteoData)

  def insertOzoneData(passSammelenData: PassSammData, analyseId: Int) = meteoRepo.insertOzoneDataForFilesSent(passSammelenData, analyseId)

//  def updateOzoneDataWithBlindWert(passSammelenData: PassSammData, analyseId: Int) = meteoRepo.updateOzoneBlindWert(passSammelenData, analyseId)

  def insertOzoneFileInfo(fileLevelConfig: OzoneFileConfig, einfdat: String) = meteoRepo.insertOzoneFileInfo(fileLevelConfig, einfdat)

  def getAnalyseIdForFile(filename: String, einfdat: String): Int = meteoRepo.findLastAnalyseIdForOzoneFile(filename, einfdat)

  def getOzoneDataForYear(year: Int) = meteoRepo.getOzoneDataForTheYear(year)

  def getOzoneFileDataForYear(year: Int): List[String] = meteoRepo.getOzoneFileDataForTheYearSamplerOne(year) ::: meteoRepo.getOzoneFileDataForTheYearSamplerTwo(year) ::: meteoRepo.getOzoneFileDataForTheYearSamplerThree(year)

  def getAllMessartsForOrgFixedFormat() = meteoRepo.findAllMessartsForOrgFixedFormat()
  def getAllMessartsForOrgFixedAggFormat = meteoRepo.findAllMessartsForOrgFixedAggFormat()

  def insertPhanoPlotBesuchDatums(besuchInfo: List[BesuchInfo], einfdat: String) = meteoRepo.insertPhanoPlotBesuchDatums(besuchInfo, einfdat)

  def getAllEinfdates(stationNumber: Int, fromTime: DateTime, toTime: DateTime, fromMessart: Int, toMessart: Int, partitionNameMD: String, partitionNameMDat: String) = meteoRepo.findAllEinfdatesOfStationForTimePeriodDaily(stationNumber, fromTime, toTime, fromMessart, toMessart, partitionNameMD, partitionNameMDat)

  def getPhanoPersonId(name: String) = {
    val names = name.split(";")
    val nachName = names(0)
    val vorName = names(1)
    meteoRepo.getPhanoPersonId(nachName, vorName)
  }

  def getAllDaysBetweenDates(fromTime: DateTime, toTime: DateTime) = meteoRepo.findAllDaysBetweenDates(fromTime, toTime)

  def getPhanoStationId(stationName: String) = meteoRepo.getPhanoStationId(stationName)





}

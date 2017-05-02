package models.repositories

import javax.inject.Inject

import anorm._
import models.domain.{MessArtRow, MeteoDataRow, Station, Unit_Einheit}
import play.api.db.DBApi


@javax.inject.Singleton
class MeteoDataRepository  @Inject() (dbapi: DBApi) {

    private val db = dbapi.database("default")

    def findAllStations(): Seq[Station] = db.withConnection { implicit connection =>
      SQL("SELECT * FROM STATION ORDER BY STATNR").as(Station.parser *)}

   def findAllMessArts() : Seq[MessArtRow] = db.withConnection { implicit connection =>
     SQL("SELECT CODE, TEXT, PERIODE FROM MESSART ORDER BY CODE").as(MessArtRow.parser *)}

   def findMeteoDataForStation(stationNumber: Int): Seq[MeteoDataRow] = db.withConnection { implicit connection =>
     SQL("select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval from meteodat where STATNR = {stationNr} and messdat > SYSDATE-10 AND messdat <= SYSDATE order by messdat DESC").on("stationNr" -> stationNumber).as(MeteoDataRow.parser *)}
  //SQL("SELECT * FROM METEODAT WHERE STATNR = {stationNr} and messdat >= {frmDate} and messdat <= {toDat}").on("stationNr" -> stationNumber).as(MeteoData.parser *)}

  def findLastestMeteoDataForStation(stationNumber: Int, messartNr: Int): Seq[MeteoDataRow] = db.withConnection { implicit connection =>
    SQL("select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval from meteodat where STATNR = {stationNr} and messart = {messartNr} and messdat > (sysdate-0.25) -(30/1440) order by messdat DESC").on("stationNr" -> stationNumber, "messartNr" -> messartNr).as(MeteoDataRow.parser *)}

}

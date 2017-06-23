package models.repositories

import javax.inject.Inject

import anorm._
import models.domain.{MeteoDataFileLogInfo, _}
import models.util.StringToDate
import org.joda.time.DateTime
import play.api.Logger
import play.api.db.DBApi


@javax.inject.Singleton
class MeteoDataRepository  @Inject() (dbapi: DBApi) {

    private val db = dbapi.database("default")

    def findAllStations(): Seq[Station] = db.withConnection { implicit connection =>
      SQL("SELECT * FROM STATION ORDER BY STATNR").as(Station.parser *)}

    def findAllOrganisations(): Seq[Organisation] = db.withConnection{ implicit connection =>
     SQL("SELECT * FROM ORG").as(Organisations.parser *)}

    def findLogInfoForDataSentToOrganisations(): Seq[MeteoDataFileLogInfo] = db.withConnection{ implicit connection =>
      SQL("SELECT statnr,orgnr,to_char(vondatum, 'DD-MM-YYYY HH24:MI:SS') as vondatum, to_char(bisdatum, 'DD-MM-YYYY HH24:MI:SS') as bisdatum,dateiname,reihegesendet FROM METEODATALOGINFO ORDER BY STATNR,BISDATUM DESC").as(MeteoDataFileLogsInfo.parser *)}

    def findOrganisationStationMapping() : Seq[OrganisationStationMapping] = db.withConnection{ implicit connection =>
      SQL("SELECT * FROM STATORGKONF ORDER BY ORGNR,STATNR").as(OrganisationStationMappingS.parser *)}

    def findAllMessArts() : Seq[MessArtRow] = db.withConnection { implicit connection =>
      SQL("SELECT MT.CODE AS CODE, MT.TEXT  AS TEXT, MT.PERIODE AS PERIODE, MT.MPROJNR AS MPROJNR, P.PDAUER AS PDAUER FROM MESSART MT, PERIODE P WHERE MT.PERIODE = P.CODE  ORDER BY P.PDAUER").as(MessArtRow.parser *)}

    def getAllStatKonf()= db.withConnection { implicit connection =>
      SQL("SELECT STATNR, MESSART, KONFNR, SENSORNR, konfnr, to_char(ABDATUM, 'DD-MM-YYYY HH24:MI:SS') as ABDATUM , to_char(BISDATUM, 'DD-MM-YYYY HH24:MI:SS') as BISDATUM, FOLGENR, CLNR FROM STATKONF WHERE BISDATUM IS NULL ORDER BY STATNR, MESSART").as(MeteoStationConfiguration.parser *)}

    def getStationAbbrevations() = db.withConnection { implicit connection =>
      SQL("SELECT CODE, KURZ_BESCHR, BESCHREIBUNG FROM STATION_GRUPPE ORDER BY CODE").as(StationAbbrevation.parser *)}

    def findMeteoDataForStation(stationNumber: Int): Seq[MeteoDataRow] = db.withConnection { implicit connection =>
      SQL("select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval from meteodat where STATNR = {stationNr} and messdat > SYSDATE-10 AND messdat <= SYSDATE order by messdat DESC").on("stationNr" -> stationNumber).as(MeteoDataRow.parser *)}
    //SQL("SELECT * FROM METEODAT WHERE STATNR = {stationNr} and messdat >= {frmDate} and messdat <= {toDat}").on("stationNr" -> stationNumber).as(MeteoData.parser *)}

    def findLastestMeteoDataForStation(stationNumber: Int, messartNr: Int): Seq[MeteoDataRow] = db.withConnection { implicit connection =>
      SQL("select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval from meteodat where STATNR = {stationNr} and messart = {messartNr} and messdat > (sysdate-0.25) -(30/1440) order by messdat DESC").on("stationNr" -> stationNumber, "messartNr" -> messartNr).as(MeteoDataRow.parser *)}

    def findLastMeteoDataForStation(stationNumber: Int, fromTime: Option[DateTime]): Seq[MeteoDataRow] = db.withConnection { implicit connection => {
      fromTime.map(dt => {
        Logger.info(s"from Date is:${StringToDate.oracleDateFormat.print(dt)} ")
        SQL("select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval from meteodat where STATNR = {stationNr} and messdat >  to_date({fromDate}, 'DD.MM.YYYY HH24:MI:SS') order by messdat DESC").on("stationNr" -> stationNumber, "fromDate" -> StringToDate.oracleDateFormat.print(dt)).as(MeteoDataRow.parser *)
      }).getOrElse(Seq())

     }
    }

  def insertLogInfoForFilesSent(meteoLogInfo: List[MeteoDataFileLogInfo]) = {

    val conn = db.getConnection()
    val stmt = conn.createStatement()

    meteoLogInfo.map(ml => {

      val fromDate = s"to_date('${StringToDate.oracleDateFormat.print(ml.fromDate)}', 'DD.MM.YYYY HH24:MI:SS')"
      val toDate = s"to_date('${StringToDate.oracleDateFormat.print(ml.toDate)}', 'DD.MM.YYYY HH24:MI:SS')"

      val insertStatement = s"INSERT INTO METEODATALOGINFO (statnr, orgnr, vondatum, bisdatum, dateiname, reihegesendet) values(" +
        s"${ml.stationNr}, ${ml.orgNr}, $fromDate, $toDate, '${ml.fileName}', ${ml.numberOfLinesSent})"
      Logger.info(s"statement to be executed: ${insertStatement}")

      stmt.executeUpdate(insertStatement)
    })
    stmt.close()
    conn.close()

  }

}

package models.repositories

import java.sql.{SQLException, Statement}
import javax.inject.Inject

import anorm._
import models.domain.Ozone._
import models.domain.{MeteoDataFileLogInfo, _}
import models.ozone.OzoneOracleError
import models.util.{CurrentSysDateInSimpleFormat, StringToDate}
import models.util.StringToDate.formatOzoneDate
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.db.DBApi

import scala.util.{Failure, Try}


@javax.inject.Singleton
class MeteoDataRepository  @Inject() (dbapi: DBApi) {

    private val db = dbapi.database("default")

    def findAllStations(): Seq[Station] = db.withConnection { implicit connection =>
      SQL("SELECT * FROM STATION ORDER BY STATNR").as(Station.parser *)}

    def findAllOrganisations(): Seq[Organisation] = db.withConnection{ implicit connection =>
     SQL("SELECT * FROM ORG").as(Organisations.parser *)}

    def findLogInfoForDataSentToOrganisations(): Seq[MeteoDataFileLogInfo] = db.withConnection{ implicit connection =>
      SQL("SELECT statnr,orgnr,to_char(vondatum, 'DD-MM-YYYY HH24:MI:SS') as vondatum, to_char(bisdatum, 'DD-MM-YYYY HH24:MI:SS') as bisdatum,dateiname,reihegesendet, to_char(lasteinfdat, 'DD-MM-YYYY HH24:MI:SS') as lasteinfdat, lasteinfdat as lastdat  FROM METEODATALOGINFO ORDER BY STATNR,lastdat DESC").as(MeteoDataFileLogsInfo.parser *)}

    def findOrganisationStationMapping() : Seq[OrganisationStationMapping] = db.withConnection{ implicit connection =>
      SQL("SELECT * FROM STATORGKONF ORDER BY ORGNR,STATNR").as(OrganisationStationMappingS.parser *)}

    def findAllMessArts() : Seq[MessArtRow] = db.withConnection { implicit connection =>
      SQL("SELECT MT.CODE AS CODE, MT.TEXT  AS TEXT, MT.PERIODE AS PERIODE, MT.MPROJNR AS MPROJNR, P.PDAUER AS PDAUER, MT.MULTI AS MULTI FROM MESSART MT, PERIODE P WHERE MT.PERIODE = P.CODE  ORDER BY P.PDAUER").as(MessArtRow.parser *)}

    def getAllStatKonf()= db.withConnection { implicit connection =>
      SQL("SELECT STATNR, MESSART, KONFNR, SENSORNR, konfnr, to_char(ABDATUM, 'DD-MM-YYYY HH24:MI:SS') as ABDATUM , to_char(BISDATUM, 'DD-MM-YYYY HH24:MI:SS') as BISDATUM, FOLGENR, CLNR FROM STATKONF WHERE BISDATUM IS NULL ORDER BY STATNR, MESSART").as(MeteoStationConfiguration.parser *)}

    def getStationAbbrevations() = db.withConnection { implicit connection =>
      SQL("select distinct(statnr) as code,t.sma_name_tx as BESCHREIBUNG,t.sma_nat_abbr_tx as KURZ_BESCHR from SMA_PARAM_ZUORD t order by code").as(StationAbbrevation.parser *)}

    def findMeteoDataForStation(stationNumber: Int): Seq[MeteoDataRow] = db.withConnection { implicit connection =>
      SQL("select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval from meteodat where STATNR = {stationNr} and messdat > SYSDATE-10 AND messdat <= SYSDATE order by messdat DESC").on("stationNr" -> stationNumber).as(MeteoDataRow.parser *)}
    //SQL("SELECT * FROM METEODAT WHERE STATNR = {stationNr} and messdat >= {frmDate} and messdat <= {toDat}").on("stationNr" -> stationNumber).as(MeteoData.parser *)}

    def findLastestMeteoDataForStation(stationNumber: Int, messartNr: Int): Seq[MeteoDataRow] = db.withConnection { implicit connection =>
      SQL("select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval from meteodat where STATNR = {stationNr} and messart = {messartNr} and messdat > (sysdate-0.25) -(30/1440) order by messdat DESC").on("stationNr" -> stationNumber, "messartNr" -> messartNr).as(MeteoDataRow.parser *)}

    def findLastMeteoDataForStation(stationNumber: Int, fromTime: Option[DateTime]): Seq[MeteoDataRow] = db.withConnection { implicit connection => {
      fromTime.map(dt => {
        Logger.info(s"from Date is:${StringToDate.oracleDateFormat.print(dt)} ")
        SQL("select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval from meteodat where STATNR = {stationNr} and einfdat >  to_date({fromDate}, 'DD.MM.YYYY HH24:MI:SS') order by messdat DESC").on("stationNr" -> stationNumber, "fromDate" -> StringToDate.oracleDateFormat.print(dt)).as(MeteoDataRow.parser *)
      }).getOrElse(Seq())

     }
    }

  def findLastAnalyseIdForOzoneFile(filename: String, einfdat: String) = db.withConnection{ implicit connection =>
    SQL("select analysid from PASSIVESAMFILEINFO where filename = {fileName} and einfdat = to_date({einfDat}, 'DD.MM.YYYY HH24:MI:SS')").on("fileName" -> filename, "einfDat" -> einfdat).as(SqlParser.int("analysid").single)}


  def insertLogInfoForFilesSent(meteoLogInfo: List[MeteoDataFileLogInfo]) = {

    val conn = db.getConnection()
    val stmt = conn.createStatement()

    meteoLogInfo.map(ml => {

      val fromDate = s"to_date('${StringToDate.oracleDateFormat.print(ml.fromDate)}', 'DD.MM.YYYY HH24:MI:SS')"
      val toDate = s"to_date('${StringToDate.oracleDateFormat.print(ml.toDate)}', 'DD.MM.YYYY HH24:MI:SS')"
      val lastEinfDat = s"to_date('${StringToDate.oracleDateFormat.print(ml.lastEinfDat)}', 'DD.MM.YYYY HH24:MI:SS')"

      val insertStatement = s"INSERT INTO METEODATALOGINFO (statnr, orgnr, vondatum, bisdatum, dateiname, reihegesendet, lasteinfdat) values(" +
        s"${ml.stationNr}, ${ml.orgNr}, $fromDate, $toDate, '${ml.fileName}', ${ml.numberOfLinesSent}, ${lastEinfDat} )"
      Logger.info(s"statement to be executed: ${insertStatement}")

      stmt.executeUpdate(insertStatement)
    })
    stmt.close()
    conn.close()

  }

  def insertCR1000MeteoDataForFilesSent(meteoData: Seq[MeteoDataRowTableInfo]): Option[CR1000OracleError] = {
    val conn = db.getConnection()
    try {
      conn.setAutoCommit(false)
      val stmt: Statement = conn.createStatement()
        meteoData.map(m => {

          val ml = m.meteoDataRow
          //code that throws sql exception

          m.multi match {
            case Some(1) => {
              val insertStatement = s"INSERT INTO METEODAT  (statnr, messart, konfnr, messdat, messwert, ursprung, valstat, einfdat) values(" +
                s"${ml.station}, ${ml.messArt}, ${ml.configuration}, ${ml.dateReceived}, ${ml.valueOfMeasurement}, ${ml.methodApplied}, ${ml.status.getOrElse(0)},${ml.dateOfInsertion})"
              Logger.info(s"statement to be executed: ${insertStatement}")
              stmt.executeUpdate(insertStatement)
            }
            case Some(2) => {
              val insertStatement = s"INSERT INTO MDAT  (statnr, messart, konfnr, messdat, messwert, ursprung, valstat, einfdat) values(" +
                s"${ml.station}, ${ml.messArt}, ${ml.configuration}, ${ml.dateReceived}, ${ml.valueOfMeasurement}, ${ml.methodApplied}, ${ml.status.getOrElse(0)},${ml.dateOfInsertion})"
              Logger.info(s"statement to be executed: ${insertStatement}")
              stmt.executeUpdate(insertStatement)

            }
            case _ => None
          }
          //Insert information into MetaBlag

        })

      insertInfoIntoMetablag(meteoData, stmt)
      stmt.close()
      conn.commit()
      conn.close()
      None
      } catch {
      case ex: SQLException => {
        if(ex.getErrorCode() == 1){
        Logger.info(s"Data was already read. Primary key violation ${ex}")
        conn.rollback()
          conn.close()

          None
        } else {
          conn.close()
          Some(CR1000OracleError(8, s"Oracle Exception: ${ex}"))
        }

      }
    }


  }

  def insertInfoIntoMetablag(meteoData: Seq[MeteoDataRowTableInfo],stmt: Statement) = {
    val groupedFiles = meteoData.groupBy(_.filename)
    meteoData.groupBy(_.filename).map(l => {
      val fileName = l._1
      val einfDat = l._2.map(_.meteoDataRow.dateOfInsertion).max
      val fromDate = l._2.map(_.meteoDataRow.dateReceived).min
      val toDate = l._2.map(_.meteoDataRow.dateReceived).max
      val status = 1
      val statNr = l._2.headOption.map(_.meteoDataRow.station)
      statNr match {
        case Some(stationNr) => {
          val insertStatement = s"insert into metablag (statnr, einfdat, abdat, bisdat, bemerk, datei, ablstat) values(" +
            s"${stationNr}, ${einfDat}, ${fromDate}, ${toDate}, 'CR1000 Data', '${fileName}', ${status})"
          Logger.info(s"statement to be executed: ${insertStatement}")
          stmt.executeUpdate(insertStatement)
        }
        case _ =>
      }})
  }

  def insertOzoneDataForFilesSent(ozoneData: PassSammData, analyseid: Int): Option[OzoneOracleError] = {
    val conn = db.getConnection()
    try {
      conn.setAutoCommit(false)
      val stmt: Statement = conn.createStatement()

      val insertStatement = s"insert into passsamdat (clnr, startdat, analysid, enddat, expduration, rawdat1, rawdat2, rawdat3, BW_rawdat, absorpdat1, absorpdat2, absorpdat3, BW_absorpdat, konzdat1, konzdat2, konzdat3, BW_konzdat, mittel, bemerk, passval, einfdat, relSD) values(" +
              s"${ozoneData.clnr}, ${ozoneData.startDate}, ${analyseid}, ${ozoneData.endDate}, ${ozoneData.duration}, " +
        s"${ozoneData.rowData1},${ozoneData.rowData2}, ${ozoneData.rowData3}, ${ozoneData.rowData4}, ${ozoneData.absorpData1}, ${ozoneData.absorpData2}, ${ozoneData.absorpData3}, ${ozoneData.absorpData4}" +
        s", ${ozoneData.konzData1}, ${ozoneData.konzData2}, ${ozoneData.konzData3}, ${ozoneData.konzData4}, ${ozoneData.mittel}, '${ozoneData.bemerk}', ${ozoneData.passval},${ozoneData.einfdat}, ${ozoneData.relSD})"
            Logger.info(s"statement to be executed: ${insertStatement}")
            stmt.executeUpdate(insertStatement)
      stmt.close()
      conn.commit()
      conn.close()
      None
    } catch {
      case ex: SQLException => {
        if(ex.getErrorCode() == 1){
          Logger.info(s"Data was already read. Primary key violation ${ex}")
          conn.rollback()
          conn.close()
          None
        } else {
          conn.close()
          Some(OzoneOracleError(8, s"Oracle Exception: ${ex}"))
        }
      }
    }
  }

  def updateOzoneBlindWert(ozoneData: PassSammData, analyseid: Int): Option[OzoneOracleError] = {
    val conn = db.getConnection()
    try {
      conn.setAutoCommit(false)
      val stmt: Statement = conn.createStatement()

      val updateStatement = s"update passsamdat set bw_rawdat = ${ozoneData.rowData1}, bw_absorpdat = ${ozoneData.absorpData1}, bw_konzdat = ${ozoneData.konzData1} where clnr = ${ozoneData.clnr} and startdat = ${ozoneData.startDate} and analysid = ${analyseid} and enddat = ${ozoneData.endDate}"
      Logger.info(s"update statement to be executed: ${updateStatement}")
      stmt.executeUpdate(updateStatement)
      stmt.close()
      conn.commit()
      conn.close()
      None
    } catch {
      case ex: SQLException => {
        if(ex.getErrorCode() == 1){
          Logger.info(s"Data was already read. Primary key violation ${ex}")
          conn.rollback()
          conn.close()
          None
        } else {
          conn.close()
          Some(OzoneOracleError(8, s"Oracle Exception: ${ex}"))
        }
      }
    }
  }


  def insertOzoneFileInfo(fileLevelConfig: OzoneFileConfig, einfdat: String): Option[OzoneOracleError] = {
    val conn = db.getConnection()
    try {
      conn.setAutoCommit(false)
      val stmt: Statement = conn.createStatement()

      val  analysenDatum = s"to_date('${StringToDate.oracleDateNoTimeFormat.print(StringToDate.formatOzoneDateWithNoTime.withZone(DateTimeZone.UTC).parseDateTime(fileLevelConfig.analysenDatum))}', 'DD.MM.YYYY')"
      val  farrbreagens = s"to_date('${StringToDate.oracleDateNoTimeFormat.print(StringToDate.formatOzoneDateWithNoTime.withZone(DateTimeZone.UTC).parseDateTime(fileLevelConfig.farrbreagens))}', 'DD.MM.YYYY')"
      val  probenEingang = s"to_date('${StringToDate.oracleDateNoTimeFormat.print(StringToDate.formatOzoneDateWithNoTime.withZone(DateTimeZone.UTC).parseDateTime(fileLevelConfig.probenEingang))}', 'DD.MM.YYYY')"
      val insertStatement = s"insert into passivesamfileinfo (analysid, firmenid, stsnr, probeeingdat, analysedatum, einfdat, analysemethode, sammel, blindwert, farbreagens, calfactor, filename, nachweisgrenze, bemerk) values(PASSANALYSEID_SEQ.NEXTVAL,1,149," +
        s" ${probenEingang}, ${analysenDatum}, ${einfdat}, '${fileLevelConfig.anaylysenMethode}', '${fileLevelConfig.sammelMethode}', " +
        s"${fileLevelConfig.blindwert},${farrbreagens}, ${fileLevelConfig.calFactor}, '${fileLevelConfig.fileName}', ${fileLevelConfig.nachweisgrenze}, '${fileLevelConfig.remarks}')"
      Logger.info(s"statement to be executed: ${insertStatement}")
      stmt.executeUpdate(insertStatement)
      stmt.close()
      conn.commit()
      conn.close()
      None
    } catch {
      case ex: SQLException => {
        if(ex.getErrorCode() == 1){
          Logger.info(s"Data was already read. Primary key violation ${ex}")
          conn.rollback()
          conn.close()
          None
        } else {
          conn.close()
          Some(OzoneOracleError(8, s"Oracle Exception: ${ex}"))
        }
      }
    }
  }

  def getOzoneFileInformation() :List[OzoneFileData] = db.withConnection { implicit connection =>
    SQL("SELECT * FROM PASSIVESAMFILEINFO ORDER BY analysid").as(OzoneFileDataRow.parser *)
  }

  def getOzoneDataForTheYear(year: Int) = db.withConnection { implicit connection =>
    SQL("""select t.clnr, to_char(t.startdat, 'dd.mm.yyyy') as startDate, to_char(t.startdat, 'HH24:MI:SS') as startTime, to_char(t.enddat, 'dd.mm.yyyy') as endDate,
    to_char(t.enddat, 'HH24:MI:SS') as endTime,t.analysid,
    t.expduration, t.rawdat1, t.rawdat2, t.rawdat3, t.bw_rawdat,
    t.absorpdat1, t.absorpdat2, t.absorpdat3, t.bw_absorpdat, t.konzdat1,
    t.konzdat2, t.konzdat3, t.bw_konzdat, t.mittel, t.relsd,
    to_char(p.probeeingdat, 'dd.mm.yyyy') as probeeingdat,
    to_char(p.analysedatum, 'dd.mm.yyyy') as analysedatum,
    p.analysemethode, p.sammel, p.blindwert, to_char(p.farbreagens, 'dd.mm.yyyy') as farbreagens, p.calfactor, p.filename, p.nachweisgrenze, p.bemerk as FileBem, t.bemerk as lineBem
    from passsamdat t, passivesamfileinfo p
      where to_char(startdat,'yyyy') = {year}
    and t.analysid = p.analysid""".stripMargin).on("year" -> year).as(OzoneDataRow.parser *)

  }

}


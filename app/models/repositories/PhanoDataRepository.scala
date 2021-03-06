package models.repositories

import java.sql.{SQLException, Statement}
import javax.inject.Inject

import anorm.{SQL, SqlParser}
import models.domain._
import models.domain.pheno.{AufnEreig, BesuchInfo, PhanoGrowthData, PhanoPFLA}
import models.ozone.OzoneOracleError
import models.util.StringToDate
import org.joda.time.DateTimeZone
import play.api.Logger
import play.api.db.DBApi


@javax.inject.Singleton
class PhanoDataRepository  @Inject()(dbapi: DBApi) {

  private val db = dbapi.database("phano")

  /*def findAllMessArts() : Seq[MeasurementParameter] = db.withConnection { implicit connection =>
    SQL("SELECT ID, NAME, DURATION_MINS, MULTI FROM MEAS_PARAMETERS").as(MeasurementParameterRow.parser *)}

  def getAllStatKonf()= db.withConnection { implicit connection =>
    SQL("SELECT station_id, meas_parameters_id, konfid, sensors_id, to_char(valid_from, 'DD-MM-YYYY HH24:MI:SS') as ABDATUM , to_char(valid_to, 'DD-MM-YYYY HH24:MI:SS') as BISDATUM, column_nr, project_id FROM station_konf WHERE valid_to IS NULL ORDER BY station_id, meas_parameters_id").as(StationConfigurationRow.parser *)}

  def findAllStations(): Seq[Station] = db.withConnection { implicit connection =>
    SQL("SELECT * FROM STATION ORDER BY STATNR").as(Station.parser *)}

  def findAllOrganisations(): Seq[Organisation] = db.withConnection{ implicit connection =>
    SQL("SELECT * FROM ORG").as(Organisations.parser *)}

  def findLogInfoForDataSentToOrganisations(): Seq[MeteoDataFileLogInfo] = db.withConnection{ implicit connection =>
    SQL("SELECT statnr,orgnr,to_char(vondatum, 'DD-MM-YYYY HH24:MI:SS') as vondatum, to_char(bisdatum, 'DD-MM-YYYY HH24:MI:SS') as bisdatum,dateiname,reihegesendet, to_char(lasteinfdat, 'DD-MM-YYYY HH24:MI:SS') as lasteinfdat, lasteinfdat as lastdat  FROM METEODATALOGINFO ORDER BY STATNR,lastdat DESC").as(MeteoDataFileLogsInfo.parser *)}

  def findOrganisationStationMapping() : Seq[OrganisationStationMapping] = db.withConnection{ implicit connection =>
    SQL("SELECT * FROM STATORGKONF ORDER BY ORGNR,STATNR").as(OrganisationStationMappingS.parser *)}


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

  }*/

  def getPhanoPersonsList(): List[PhanoPersonsDataRow] = db.withConnection { implicit connection =>
    SQL("select persnr, NLS_UPPER(listagg(val,',') within group(order by val)) personname from ( select vorname,name,persnr from PERSON t ) unpivot exclude nulls ( val for key in (vorname,name)) group by persnr").as(PhanoPersonsData.parser *)
  }

  def getPhanoStationId(stationName: String) = db.withConnection { implicit connection =>
    SQL(s"""select statcode from STATION where statname like ${"'%"+ stationName+"%'"}""").as(SqlParser.int("statcode").*)
  }


  def getPhanoSpeciesId(speciesName: String) = db.withConnection { implicit connection =>
    SQL(s"""select art from artbed where sprache = 'D' and text like ${"'%"+ speciesName+"%'"}""").as(SqlParser.int("art").*)
  }

  def insertPhanoPlotBesuchDatums(besuchInfo: List[BesuchInfo], einfdat: String): Option[OzoneOracleError] = {
    /* val conn = db.getConnection()
     try {
       conn.setAutoCommit(false)

       besuchInfo.map( besuch => {
         val stmt: Statement = conn.createStatement()
         val analysenDatum = s"to_date('${StringToDate.oracleDateNoTimeFormat.print(StringToDate.formatOzoneDateWithNoTime.withZone(DateTimeZone.UTC).parseDateTime(fileLevelConfig.analysenDatum))}', 'DD.MM.YYYY')"
         val farrbreagens = s"to_date('${StringToDate.oracleDateNoTimeFormat.print(StringToDate.formatOzoneDateWithNoTime.withZone(DateTimeZone.UTC).parseDateTime(fileLevelConfig.farrbreagens))}', 'DD.MM.YYYY')"
         val probenEingang = s"to_date('${StringToDate.oracleDateNoTimeFormat.print(StringToDate.formatOzoneDateWithNoTime.withZone(DateTimeZone.UTC).parseDateTime(fileLevelConfig.probenEingang))}', 'DD.MM.YYYY')"
         val insertStatement = s"insert into passivesamfileinfo (analysid, firmenid, stsnr, probeeingdat, analysedatum, einfdat, analysemethode, sammel, blindwert, farbreagens, calfactor, filename, nachweisgrenze, bemerk) values(PASSANALYSEID_SEQ.NEXTVAL,1,149," +
           s" ${probenEingang}, ${analysenDatum}, ${einfdat}, '${fileLevelConfig.anaylysenMethode}', '${fileLevelConfig.sammelMethode}', " +
           s"${fileLevelConfig.blindwert},${farrbreagens}, ${fileLevelConfig.calFactor}, '${fileLevelConfig.fileName}', ${fileLevelConfig.nachweisgrenze}, '${fileLevelConfig.remarks}')"
         Logger.info(s"statement to be executed: ${insertStatement}")
         stmt.executeUpdate(insertStatement)
         stmt.close()
       })

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
     }*/
    None
  }

  def insertBesuchInfoData(besuchData: BesuchInfo): Option[CR1000OracleError] = {
    val conn = db.getConnection()
    try {
      conn.setAutoCommit(false)
      val stmt: Statement = conn.createStatement()
      val onDate = s"to_date('${StringToDate.oracleDateNoTimeFormat.print(StringToDate.formatOzoneDateWithNoTime.withZone(DateTimeZone.UTC).parseDateTime(besuchData.besuchDatum))}', 'DD.MM.YYYY')"

      val insertStatement = s"insert into besuch (persnr, invnr, statcode, besdatum, bemerkun) values ( ${besuchData.persnr}, ${besuchData.invnr}, ${besuchData.statnr}, ${onDate}, '${besuchData.comments}')"
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
          Some(CR1000OracleError(8, s"Oracle Exception: ${ex}"))
        }

      }
    }
  }

  def insertPhanoPFLA(phanoPfla: PhanoPFLA): Option[CR1000OracleError] = {
    val conn = db.getConnection()
    try {
      conn.setAutoCommit(false)
      val stmt: Statement = conn.createStatement()
      val insertStatement = s"insert into phaeno_pfla (statcode, typcode, statucode, art, sprache, invnr, pfla_id, b_einf, d_einf) values ( ${phanoPfla.stationNr}, ${phanoPfla.typCode}, ${phanoPfla.statusCode}, ${phanoPfla.species}, '${phanoPfla.sprache}',${phanoPfla.invnr},${phanoPfla.pflaId},'automatic insert',${phanoPfla.einfdat})"
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
          Some(CR1000OracleError(8, s"Oracle Exception: ${ex}"))
        }

      }
    }
  }


  def insertPhanoPFLAParameter(phanoPflaParam: PhanoGrowthData) : Option[CR1000OracleError] = {
    val conn = db.getConnection()
    try {
      conn.setAutoCommit(false)
      val stmt: Statement = conn.createStatement()
      //val onDate = s"to_date('${StringToDate.oracleDateNoTimeFormat.print(StringToDate.formatOzoneDateWithNoTime.withZone(DateTimeZone.UTC).parseDateTime(phanoPflaParam.messdat))}', 'DD.MM.YYYY')"
      val insertStatement = s"insert into pfla_param (invnr, statcode, typcode, messdat, sozcode, bhoehe, bumfang, bemerkungen, pfla_id) values ( ${phanoPflaParam.invnr}, ${phanoPflaParam.stationNr}, ${phanoPflaParam.typeCode}, ${phanoPflaParam.messdat}, ${phanoPflaParam.social},${phanoPflaParam.height},${phanoPflaParam.umfang},'${phanoPflaParam.bemerkung}', ${phanoPflaParam.pflaId})"
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
          Some(CR1000OracleError(8, s"Oracle Exception: ${ex}"))
        }

      }
    }
  }


  def insertPhanoAufnEreigParameter(phanoPflaAufnEreig: AufnEreig) : Option[CR1000OracleError] = {
    val conn = db.getConnection()
    try {
      conn.setAutoCommit(false)
      val stmt: Statement = conn.createStatement()
      //val onDate = s"to_date('${StringToDate.oracleDateNoTimeFormat.print(StringToDate.formatOzoneDateWithNoTime.withZone(DateTimeZone.UTC).parseDateTime(phanoPflaAufnEreig.ereignisDate))}', 'DD.MM.YYYY')"
      val insertStatement = s"insert into aufn_ereig (statcode, typcode, invnr, ereigniscode, ereignisdatum, persnr, intcode, pfla_id) values ( ${phanoPflaAufnEreig.stationNr}, ${phanoPflaAufnEreig.typCode}, ${phanoPflaAufnEreig.invnr}, ${phanoPflaAufnEreig.ereignisCode}, ${phanoPflaAufnEreig.ereignisDate},${phanoPflaAufnEreig.persNr},${phanoPflaAufnEreig.intensityCode},${phanoPflaAufnEreig.pflaId})"
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
          Some(CR1000OracleError(8, s"Oracle Exception: ${ex}"))
        }

      }
    }
  }

  def insertInfoIntoFileInfo(meteoData: Seq[MeteoDataRowTableInfo],stmt: Statement) = {
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
          val insertStatement = s"insert into file_log_info (id, station_id, file_name, timestamp, nr_of_rows, from_time, to_time) values (FileInfo_ID_SEQ.nextval, " +
            s"${stationNr}, '${fileName}', ${einfDat}, ${meteoData.size}, ${fromDate}, ${toDate})"
          Logger.info(s"statement to be executed: ${insertStatement}")
          stmt.executeUpdate(insertStatement)
        }
        case _ =>
      }})
    groupedFiles
  }



 /* def insertOzoneDataForFilesSent(ozoneData: PassSammData, analyseid: Int): Option[OzoneOracleError] = {
    val conn = db.getConnection()
    try {
      conn.setAutoCommit(false)
      val stmt: Statement = conn.createStatement()

      val insertStatement = s"insert into passsamdat (clnr, startdat, analysid, enddat, expduration, rawdat1, rawdat2, rawdat3, rawdat4, absorpdat1, absorpdat2, absorpdat3, absorpdat4, konzdat1, konzdat2, konzdat3, konzdat4, mittel, bemerk, passval, einfdat) values(" +
        s"${ozoneData.clnr}, ${ozoneData.startDate}, ${analyseid}, ${ozoneData.endDate}, ${ozoneData.duration}, " +
        s"${ozoneData.rowData1},${ozoneData.rowData2}, ${ozoneData.rowData3}, ${ozoneData.rowData4}, ${ozoneData.absorpData1}, ${ozoneData.absorpData2}, ${ozoneData.absorpData3}, ${ozoneData.absorpData4}" +
        s", ${ozoneData.konzData1}, ${ozoneData.konzData2}, ${ozoneData.konzData3}, ${ozoneData.konzData4}, ${ozoneData.mittel}, '${ozoneData.bemerk}', ${ozoneData.passval},${ozoneData.einfdat})"
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
  }*/

}



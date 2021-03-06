package models.repositories

import java.sql.{SQLException, Statement}
import javax.inject.Inject

import anorm._
import models.domain.Ozone._
import models.domain.pheno.{BesuchInfo, PhanoFileLevelInfo}
import models.domain.{MeteoDataFileLogInfo, MeteoDataRowTableInfo, _}
import models.ozone.OzoneOracleError
import models.util.{CurrentSysDateInSimpleFormat, StringToDate}
import models.util.StringToDate.formatOzoneDate
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.db.DBApi
import anorm.{SQL, SqlParser}
import SqlParser.{int, str}

import scala.util.{Failure, Try}


@javax.inject.Singleton
class MeteoDataRepository  @Inject() (dbapi: DBApi) {

    private val db = dbapi.database("default")

    def findAllStations(): Seq[Station] = db.withConnection { implicit connection =>
      SQL("SELECT * FROM STATION ORDER BY STATNR").as(Station.parser *)}

    def findAllOrganisations(): Seq[Organisation] = db.withConnection{ implicit connection =>
     SQL("SELECT * FROM ORG").as(Organisations.parser *)}

    def findAllOrganisationsAndProjectsConfig(): Seq[OrganisationProject] = db.withConnection{ implicit connection =>
      SQL("select orgnr,projnr from STATORGPROJKONF where senddata = 1").as(OrganisationProjects.parser *)}

    def findLogInfoForDataSentToOrganisations(): Seq[MeteoDataFileLogInfo] = db.withConnection{ implicit connection =>
      SQL("SELECT statnr, orgnr, to_char(vondatum, 'DD-MM-YYYY HH24:MI:SS') as vondatum, to_char(bisdatum, 'DD-MM-YYYY HH24:MI:SS') as bisdatum, dateiname,reihegesendet, to_char(lasteinfdat, 'DD-MM-YYYY HH24:MI:SS') as lasteinfdat, lasteinfdat as lastdat  FROM METEODATALOGINFO where (statnr,orgnr,lasteinfdat ) in (select ml.statnr,ml.orgnr,max(ml.lasteinfdat) from METEODATALOGINFO ml group by ml.statnr,ml.orgnr) ORDER BY STATNR,lastdat DESC").as(MeteoDataFileLogsInfo.parser *)}

    def findOrganisationStationMapping() : Seq[OrganisationStationMapping] = db.withConnection{ implicit connection =>
      SQL("SELECT * FROM STATORGKONF ORDER BY ORGNR,STATNR").as(OrganisationStationMappingS.parser *)}

    def findAllMessArts() : Seq[MessArtRow] = db.withConnection { implicit connection =>
      SQL("SELECT MT.CODE AS CODE, MT.TEXT  AS TEXT, MT.PERIODE AS PERIODE, MT.MPROJNR AS MPROJNR, P.PDAUER AS PDAUER, MT.MULTI AS MULTI, e.text as einheit FROM MESSART MT, PERIODE P , einheit e WHERE MT.PERIODE = P.CODE and mt.einheit = e.code ORDER BY P.PDAUER").as(MessArtRow.parser *)}

    def findAllMessartsForOrgFixedFormat() : Seq[OrgStationParamMapping] = db.withConnection { implicit connection =>
      SQL("select orgnr, projnr, statnr, parameterid, shortnamebyorg, columnnr, fromdate, todate, senddata, agg_method, completeness from statorgprojparamkonf where agg_method is null and completeness is null and todate is null ").as(OrgStationParamMappings.parser *)}

    def findAllMessartsForOrgFixedAggFormat() : Seq[OrgStationParamMapping] = db.withConnection { implicit connection =>
      SQL("select orgnr, projnr, statnr, parameterid, shortnamebyorg, columnnr, fromdate, todate, senddata, agg_method, completeness from statorgprojparamkonf where agg_method is not null and completeness is not null").as(OrgStationParamMappings.parser *)}

    def getAllStatKonf()= db.withConnection { implicit connection =>
      SQL("SELECT STATNR, MESSART, KONFNR, SENSORNR, konfnr, to_char(ABDATUM, 'DD-MM-YYYY HH24:MI:SS') as ABDATUM , to_char(BISDATUM, 'DD-MM-YYYY HH24:MI:SS') as BISDATUM, FOLGENR, CLNR,completeness, apply_method_agg, substatnr FROM STATKONF WHERE BISDATUM IS NULL ORDER BY STATNR, MESSART").as(MeteoStationConfiguration.parser *)}

    def getStationAbbrevations() = db.withConnection { implicit connection =>
      SQL("select distinct(statnr) as code,t.sma_name_tx as BESCHREIBUNG,t.sma_nat_abbr_tx as KURZ_BESCHR from SMA_PARAM_ZUORD t order by code").as(StationAbbrevation.parser *)}

    def findMeteoDataForStation(stationNumber: Int): Seq[MeteoDataRow] = db.withConnection { implicit connection =>
      SQL("select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval,substatnr from meteodat where STATNR = {stationNr} and messdat > SYSDATE-10 AND messdat <= SYSDATE order by messdat DESC").on("stationNr" -> stationNumber).as(MeteoDataRow.parser *)}
    //SQL("SELECT * FROM METEODAT WHERE STATNR = {stationNr} and messdat >= {frmDate} and messdat <= {toDat}").on("stationNr" -> stationNumber).as(MeteoData.parser *)}

    def findLastestMeteoDataForStation(stationNumber: Int, messartNr: Int): Seq[MeteoDataRow] = db.withConnection { implicit connection =>
      SQL("select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval,substatnr from meteodat where STATNR = {stationNr} and messart = {messartNr} and messdat > (sysdate-0.25) -(30/1440) order by messdat DESC").on("stationNr" -> stationNumber, "messartNr" -> messartNr).as(MeteoDataRow.parser *)}

    def findLastMeteoDataForStation(stationNumber: Int, fromTime: Option[DateTime]): Seq[MeteoDataRow] = db.withConnection { implicit connection => {
      fromTime.map(dt => {
        Logger.info(s"from Date is:${StringToDate.oracleDateFormat.print(dt)} ")
        SQL(
          """select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval,substatnr from meteodat   where STATNR = {stationNr} and einfdat >  to_date({fromDate}, 'DD.MM.YYYY HH24:MI:SS')
            |union
            |select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval,substatnr from mdat where STATNR = {stationNr} and einfdat >  to_date({fromDate}, 'DD.MM.YYYY HH24:MI:SS')
            |order by messdate DESC""".stripMargin).on("stationNr" -> stationNumber, "fromDate" -> StringToDate.oracleDateFormat.print(dt)).as(MeteoDataRow.parser *)
      }).getOrElse(Seq())

     }
    }


  /*For the historical file generation two methods are used:
  1) get the distinct eindate starting from a date and until system date from meteodat and mdat tables
   2) get the data for each einfdat timstamp for part 1
   */


  def findAllEinfdatesOfStationForTimePeriodDaily(stationNumber: Int, fromTime: DateTime, toTime: DateTime, fromMessart: Int, toMessart: Int, partitionNameMD: String, partitionNameMDat: String): Seq[String] = db.withConnection { implicit connection => {
      SQL(
        """select distinct(to_char(to_timestamp(einfdat, 'DD-MM-YYYY HH24:MI:SS.FF'), 'DD-MM-YYYY HH24:MI:SS.FF')) as einfdate from (
            select to_char(einfdat, 'DD.MM.YYYY HH24:MI:SS.FF') as einfdat from meteodat  where einfdat > to_date({fromDate}, 'DD.MM.YYYY HH24:MI:SS') and einfdat < to_date({toTime}, 'DD.MM.YYYY HH24:MI:SS') and STATNR = {stationNr} and messart between {fromMessart} and {toMessart}
            union
            select to_char(einfdat, 'DD.MM.YYYY HH24:MI:SS.FF') as einfdat from mdat partition(mdat2018) where einfdat > to_date({fromDate}, 'DD.MM.YYYY HH24:MI:SS') and einfdat < to_date({toTime}, 'DD.MM.YYYY HH24:MI:SS') and STATNR = {stationNr} and messart between {fromMessart} and {toMessart}
    ) order by einfdate""".stripMargin).on("stationNr" -> stationNumber, "fromDate" -> StringToDate.oracleDateFormat.print(fromTime),"toTime" -> StringToDate.oracleDateFormat.print(toTime), "fromMessart" -> fromMessart, "toMessart" -> toMessart, "partitionNameMD" -> partitionNameMD, "partitionNameMDat" -> partitionNameMDat).as(SqlParser.scalar[String] *)
   }
  }

  def findAllDaysBetweenDates(fromTime: DateTime, toTime: DateTime): List[(String, String)] = db.withConnection { implicit connection => {
    SQL(
      """select to_char(to_date({fromTime},'dd-MM-yyyy') + rownum -1,'dd-MM-yyyy') as fromDate , to_char(to_date({fromTime},'dd-MM-yyyy') + rownum,'dd-MM-yyyy') as toDate
        |from all_objects where rownum <= to_date({toTime},'dd-MM-yyyy')-to_date({fromTime},'dd-MM-yyyy')+1""".stripMargin).on("fromTime" -> StringToDate.formatOzoneDateWithNoTimeWithDash.print(fromTime),"toTime" -> StringToDate.formatOzoneDateWithNoTimeWithDash.print(toTime)).as((str("fromDate") ~ str("toDate")).map(f => (f._1, f._2)) *)
  }
  }

  def findAllDaysBetweenLast15Days(): Seq[(String, String)] = db.withConnection { implicit connection => {
    SQL(
      """select to_char(to_date(sysdate - 15,'dd-MM-yyyy') + rownum -1,'dd-MM-yyyy') as fromDate , to_char(to_date(sysdate - 15,'dd-MM-yyyy') + rownum,'dd-MM-yyyy') as toDate
    from all_objects where rownum <= to_date(sysdate,'dd-MM-yyyy')-to_date(sysdate - 15,'dd-MM-yyyy')+1""".stripMargin).as((str("fromDate") ~ str("toDate")).map(f => (f._1, f._2)) *)
    }
  }

  def findMaxMeasurementDateForAStation(statNr: Int): Seq[String] = db.withConnection { implicit connection => {
    SQL(
      """select to_char(max(m.messdat), 'DD.MM.YYYY HH24:MI:SS') as maxdate from meteodat m where m.messdat > sysdate -90 and m.messdat < sysdate - 7 and m.statnr = {stationNr}""".stripMargin).on("stationNr" -> statNr).as((str("maxdate")).map(f => f)*)
    }
  }


  def findLastMeteoDataForStationForDate(stationNumber: Int, fromTime: String): Seq[MeteoDataRow] = db.withConnection { implicit connection => {
    Logger.info(s"${fromTime}")
      SQL(
        """select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval,substatnr from meteodat  where STATNR = {stationNr} and einfdat =  to_timestamp({fromDate}, 'DD.MM.YYYY HH24:MI:SS.FF')
          |union
          |select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval,substatnr from mdat  where STATNR = {stationNr} and einfdat =  to_timestamp({fromDate}, 'DD.MM.YYYY HH24:MI:SS.FF')
          |order by messdate DESC""".stripMargin).on("stationNr" -> stationNumber, "fromDate" -> fromTime).as(MeteoDataRow.parser *)
    }
  }

  def findLastMeteoDataForStationBetweenDates(stationNumber: Int, fromTime: String, toTime: String): Seq[MeteoDataRow] = db.withConnection { implicit connection => {
    Logger.info(s"${fromTime}")
    SQL(
      """select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval,substatnr from meteodat   where STATNR = {stationNr} and einfdat >=  to_timestamp({fromTime}, 'DD.MM.YYYY HH24:MI:SS') and einfdat <  to_timestamp({toTime}, 'DD.MM.YYYY HH24:MI:SS')
        |union
        |select statnr, messart, konfnr, to_char(messdat, 'DD-MM-YYYY HH24:MI:SS') as messdate, messwert, to_char(einfdat, 'DD-MM-YYYY HH24:MI:SS') as einfdate,ursprung,manval,substatnr from mdat  where STATNR = {stationNr} and einfdat >=  to_timestamp({fromTime}, 'DD.MM.YYYY HH24:MI:SS') and einfdat <  to_timestamp({toTime}, 'DD.MM.YYYY HH24:MI:SS')
        |order by messdate DESC""".stripMargin).on("stationNr" -> stationNumber, "fromTime" -> fromTime, "toTime" -> toTime).as(MeteoDataRow.parser *)
    }
  }

  def getLastOneDayOttPulvioDataForStation(stationNr: Int, messart: Int) = db.withConnection { implicit connection => {
    SQL(
      """select sum(messwert) as summessart,count(*) as countval  from meteodat  where STATNR = {stationNr} and messart = {messart} and messdat >= sysdate - 1  - (2 / 24)""".stripMargin).on("stationNr" -> stationNr, "messart" -> messart).as(OttPulvioData.parser *)
      }
  }

  def getLastOneWeekDataForStation(stationNr: Int, messart: Int) = db.withConnection { implicit connection => {
    SQL(
      """select TO_CHAR(messdat, 'DAY') as tag, to_char(messdat, 'dd.mm.yyyy') as measdate, sum(messwert) as summessart,count(*) as countval  from meteodat
        |where STATNR = {stationNr}
        |and messart = {messart}
        |and to_date(messdat, 'dd.mm.yyyy') between to_date(sysdate-8, 'dd.mm.yyyy') and to_date(sysdate-2, 'dd.mm.yyyy')
        |group by TO_CHAR(messdat, 'DAY') , to_char(messdat, 'dd.mm.yyyy')
        |union
        |select TO_CHAR(messdat, 'DAY') as tag, to_char(messdat, 'dd.mm.yyyy') as measdate, sum(messwert) as summessart,count(*) as countval  from mdat
        |where STATNR = {stationNr}
        |and messart = {messart}
        |and to_date(messdat, 'dd.mm.yyyy') between to_date(sysdate-8, 'dd.mm.yyyy') and to_date(sysdate-2, 'dd.mm.yyyy')
        |group by TO_CHAR(messdat, 'DAY') , to_char(messdat, 'dd.mm.yyyy')
        |order by measdate,tag
        |""".stripMargin).on("stationNr" -> stationNr, "messart" -> messart).as(WeeklyPreciVordemwaldData.parser *)
    }
  }

  def getLastTimeDataWasSentOutMeteoSchweiz():Seq[MeteoDataFileLogInfo] = db.withConnection{ implicit connection =>
    SQL("""SELECT statnr, orgnr, to_char(vondatum, 'DD-MM-YYYY HH24:MI:SS') as vondatum, to_char(bisdatum, 'DD-MM-YYYY HH24:MI:SS') as bisdatum, dateiname,reihegesendet, to_char(lasteinfdat, 'DD-MM-YYYY HH24:MI:SS') as lasteinfdat, lasteinfdat as lastdat
          |FROM METEODATALOGINFO
          |where (statnr,orgnr,lasteinfdat ) in
          |(select ml.statnr,ml.orgnr,max(ml.lasteinfdat)
          |from METEODATALOGINFO ml group by ml.statnr,ml.orgnr)
          |and dateiname <> 'RAWDATA'
          |AND ORGNR = 1
          |ORDER BY STATNR,lastdat DESC""".stripMargin).as(MeteoDataFileLogsInfo.parser *)}


  def findLastAnalyseIdForOzoneFile(filename: String, einfdat: String) = db.withConnection{ implicit connection =>
    SQL("select analysid from PASSIVESAMFILEINFO where filename = {fileName} and einfdat = to_date({einfDat}, 'DD.MM.YYYY HH24:MI:SS')").on("fileName" -> filename, "einfDat" -> einfdat).as(SqlParser.int("analysid").single)}


  def insertLogInfoForFilesSent(meteoLogInfo: List[MeteoDataFileLogInfo]) = db.withConnection { implicit conn => {
    try {
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
      conn.commit()
      //conn.close()

    } catch {
      case ex: SQLException => {
        if (ex.getErrorCode() == 1) {
          Logger.info(s"Data was already read. Primary key violation ${ex}")
          //conn.rollback()
          None
        } else {
          Logger.info(s"Oracle DB error while storing the file information")
        }
      }
    }

  }
  }


  def insertInfoIntoMetablag(meteoData: Seq[MeteoDataRowTableInfo]) = db.withConnection { implicit conn => {
    try {
      conn.setAutoCommit(true)
      val stmt: Statement = conn.createStatement()
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
        }
      })
      stmt.close()
      conn.commit()
      //conn.close()
      None
    } catch {
      case ex: SQLException => {
        if (ex.getErrorCode() == 1) {
          Logger.info(s"Data was already read. Primary key violation ${ex}")
          //conn.rollback()
          None
        } else {
          Some(CR1000OracleError(8, s"Oracle Exception: ${ex}"))
        }

      }
    }
  }
  }

  def insertCR1000MeteoDataForFilesSent(meteoData: Seq[MeteoDataRowTableInfo]): Option[CR1000OracleError] = db.withConnection { implicit conn => {
    try {
    val exceptionsInsertingDataRows = meteoData.par.flatMap(m => {
      try {
        //val conn = db.getConnection()
        conn.setAutoCommit(true)
        val stmt: Statement = conn.createStatement()

        val ml = m.meteoDataRow
        //code that throws sql exception

        m.multi match {
          case Some(1) => {
            val insertStatement = s"INSERT INTO METEODAT  (statnr, messart, konfnr, messdat, messwert, ursprung, valstat, einfdat, substatnr) values(" +
              s"${ml.station}, ${ml.messArt}, ${ml.configuration}, ${ml.dateReceived}, ${ml.valueOfMeasurement}, ${ml.methodApplied}, ${ml.status.getOrElse(0)}, ${ml.dateOfInsertion}, ${ml.subStatNr})"
            //Logger.info(s"statement to be executed: ${insertStatement}")
            stmt.executeUpdate(insertStatement)
          }
          case Some(2) => {
            val insertStatement = s"INSERT INTO MDAT  (statnr, messart, konfnr, messdat, messwert, ursprung, valstat, einfdat, substatnr) values(" +
              s"${ml.station}, ${ml.messArt}, ${ml.configuration}, ${ml.dateReceived}, ${ml.valueOfMeasurement}, ${ml.methodApplied}, ${ml.status.getOrElse(0)},${ml.dateOfInsertion}, ${ml.subStatNr})"
            //Logger.info(s"statement to be executed: ${insertStatement}")
            stmt.executeUpdate(insertStatement)

          }
          case _ => None
        }
        //Insert information into MetaBlag
        //insertInfoIntoMetablag(meteoData, stmt)
        stmt.close()
        conn.commit()
        //conn.close()
        None
      } catch {
        case ex: SQLException => {
          if (ex.getErrorCode() == 1) {
            Logger.info(s"Data was already read. Primary key violation ${ex} ${m.meteoDataRow.toString}")
            //conn.rollback()
            None
          } else {
            Some(CR1000OracleError(8, s"Oracle Exception: ${ex}"))
          }

        }
      }

    }).headOption
    val metablagException = if (exceptionsInsertingDataRows.isEmpty) insertInfoIntoMetablag(meteoData) else None
    exceptionsInsertingDataRows match {
      case Some(x) => Some(x)
      case None => metablagException
    }} catch {
        case ex: SQLException => {
          if (ex.getErrorCode() == 1) {
            Logger.info(s"Data was already read. Primary key violation ${ex} ")
            //conn.rollback()
            None
          } else {
            Some(CR1000OracleError(8, s"Oracle Exception: ${ex}"))
          }
        }
      }
  }
  }

      def insertCR1000MeteoDataForLargeFilesSent(m: MeteoDataRowTableInfo): Option[CR1000OracleError] = db.withConnection { implicit conn => {
        try {
          //val conn = db.getConnection()
          conn.setAutoCommit(true)
          val stmt: Statement = conn.createStatement()

          val ml = m.meteoDataRow
          //code that throws sql exception

          m.multi match {
            case Some(1) => {
              val insertStatement = s"INSERT INTO METEODAT  (statnr, messart, konfnr, messdat, messwert, ursprung, valstat, einfdat, substatnr) values(" +
                s"${ml.station}, ${ml.messArt}, ${ml.configuration}, ${ml.dateReceived}, ${ml.valueOfMeasurement}, ${ml.methodApplied}, ${ml.status.getOrElse(0)},${ml.dateOfInsertion}, ${ml.subStatNr})"
              //Logger.info(s"statement to be executed: ${insertStatement}")
              stmt.executeUpdate(insertStatement)
            }
            case Some(2) => {
              val insertStatement = s"INSERT INTO MDAT  (statnr, messart, konfnr, messdat, messwert, ursprung, valstat, einfdat, substatnr) values(" +
                s"${ml.station}, ${ml.messArt}, ${ml.configuration}, ${ml.dateReceived}, ${ml.valueOfMeasurement}, ${ml.methodApplied}, ${ml.status.getOrElse(0)},${ml.dateOfInsertion}, ${ml.subStatNr})"
              //Logger.info(s"statement to be executed: ${insertStatement}")
              stmt.executeUpdate(insertStatement)

            }
            case _ => None
          }
          //Insert information into MetaBlag
          //insertInfoIntoMetablag(meteoData, stmt)
          stmt.close()
          conn.commit()
          Logger.info(s"INSERTED ONE ROW")

          //conn.close()
          None
        } catch {
          case ex: SQLException => {
            if (ex.getErrorCode() == 1) {
              //Logger.info(s"Data was already read. Primary key violation ${ex} ${m.meteoDataRow.toString}")
              //conn.rollback()
              None
            } else {
              Logger.info(s"Oracle Exception detected ${ex} ${m.meteoDataRow.toString}")

              Some(CR1000OracleError(8, s"Oracle Exception: ${ex}"))
            }

          }
        }
      }
      }



  def insertCR1000MeteoDataForLargeFiles(meteoData: Seq[MeteoDataRowTableInfo]): Option[CR1000OracleError] = {
    try {
      val exceptionsInsertingDataRows = meteoData.flatMap(m => {
        insertCR1000MeteoDataForLargeFilesSent(m)
      }).headOption
      val metablagException = if (exceptionsInsertingDataRows.isEmpty) insertInfoIntoMetablag(meteoData) else None
      exceptionsInsertingDataRows match {
        case Some(x) => Some(x)
        case None => metablagException
      }

    } catch {
      case ex: SQLException => {
        if (ex.getErrorCode() == 1) {
          Logger.info(s"Data was already read. Primary key violation ${ex} ")
          //conn.rollback()
          None
        } else {
          Some(CR1000OracleError(8, s"Oracle Exception: ${ex}"))
        }
      }
    }
  }

  def insertOzoneDataForFilesSent(ozoneData: PassSammData, analyseid: Int): Option[OzoneOracleError] = {
    val conn = db.getConnection()
    try {
      conn.setAutoCommit(false)
      val stmt: Statement = conn.createStatement()

      val insertStatement = s"insert into passsamdat (clnr, startdat, analysid, enddat, expduration, rawdat1, rawdat2, rawdat3, absorpdat1, absorpdat2, absorpdat3, konzdat1, konzdat2, konzdat3, mittel, bemerk, passval, einfdat, relSD, blindsampler) values(" +
              s"${ozoneData.clnr}, ${ozoneData.startDate}, ${analyseid}, ${ozoneData.endDate}, ${ozoneData.duration}, " +
        s"${ozoneData.rowData1},${ozoneData.rowData2}, ${ozoneData.rowData3}, ${ozoneData.absorpData1}, ${ozoneData.absorpData2}, ${ozoneData.absorpData3}, " +
        s" ${ozoneData.konzData1}, ${ozoneData.konzData2}, ${ozoneData.konzData3},  ${ozoneData.mittel}, '${ozoneData.bemerk}', ${ozoneData.passval},${ozoneData.einfdat}, ${ozoneData.relSD}, ${ozoneData.blindSampler})"
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

  /*def updateOzoneBlindWert(ozoneData: PassSammData, analyseid: Int): Option[OzoneOracleError] = {
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
  }*/


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

    /* This query is only for ICP Forest
     SQL("""select t.clnr, to_char(t.startdat, 'dd.mm.yyyy') as startDate, to_char(t.startdat, 'HH24:MI') as startTime, to_char(t.enddat, 'dd.mm.yyyy') as endDate,
 |    to_char(t.enddat, 'HH24:MI') as endTime,t.analysid,
 |to_char(t.expduration,'9990.0') as expduration, to_char(t.rawdat1,'9990.0') as rawdat1, to_char(t.rawdat2,'9990.0') as rawdat2,  to_char(t.rawdat3,'9990.0') as rawdat3,
 |    t.absorpdat1, t.absorpdat2, t.absorpdat3, to_char(t.konzdat1,'9990.0') as konzdat1,
 |to_char(t.konzdat2,'9990.0') as konzdat2, to_char(t.konzdat3,'9990.0') as konzdat3, to_char(t.mittel,'9990.0') as mittel, to_char(t.relsd,'9990.0') as relsd,
 |    to_char(p.probeeingdat, 'dd.mm.yyyy') as probeeingdat,
 |    to_char(p.analysedatum, 'dd.mm.yyyy') as analysedatum,
 |    p.analysemethode, p.sammel, p.blindwert, to_char(p.farbreagens, 'dd.mm.yyyy') as farbreagens, p.calfactor, p.filename, p.nachweisgrenze, p.bemerk as FileBem, t.bemerk as lineBem, t.blindsampler
 |    from passsamdat t, passivesamfileinfo p
      where to_char(startdat,'yyyy') = {year}
      |and t.clnr not in (374674,-1112)
    and t.analysid = p.analysid order by t.clnr,t.startdat""".stripMargin).on("year" -> year).as(OzoneDataRow.parser *)
     */
    SQL("""select t.clnr, to_char(t.startdat, 'dd.mm.yyyy') as startDate, to_char(t.startdat, 'HH24:MI') as startTime, to_char(t.enddat, 'dd.mm.yyyy') as endDate,
 |    to_char(t.enddat, 'HH24:MI') as endTime,t.analysid,
 |  to_char(t.expduration,'9990.0') as expduration, to_char(t.rawdat1,'9990.0') as rawdat1, to_char(t.rawdat2,'9990.0') as rawdat2,  to_char(t.rawdat3,'9990.0') as rawdat3,
 |    t.absorpdat1, t.absorpdat2, t.absorpdat3, to_char(t.konzdat1,'9990.0') as konzdat1,
 |  to_char(t.konzdat2,'9990.0') as konzdat2, to_char(t.konzdat3,'9990.0') as konzdat3, to_char(t.mittel,'9990.0') as mittel, to_char(t.relsd,'9990.0') as relsd,
 |    to_char(p.probeeingdat, 'dd.mm.yyyy') as probeeingdat,
 |    to_char(p.analysedatum, 'dd.mm.yyyy') as analysedatum,
 |    p.analysemethode, p.sammel, p.blindwert, to_char(p.farbreagens, 'dd.mm.yyyy') as farbreagens, p.calfactor, p.filename, p.nachweisgrenze, p.bemerk as FileBem, t.bemerk as lineBem, t.blindsampler
 |    from passsamdat t, passivesamfileinfo p
      where to_char(startdat,'yyyy') = {year}
      and t.startdat <> to_date('01.01.1900 00:00:00', 'DD.MM.YYYY HH24:MI:SS')
      |and t.enddat <> to_date('01.01.1900 00:00:00', 'DD.MM.YYYY HH24:MI:SS')
      |and t.clnr not in (374674,-1112,337588.1,337588.2,337588.3,337588.4,335577)
      and t.blindsampler <> 4
      and t.analysid = p.analysid order by t.clnr,t.startdat""".stripMargin).on("year" -> year).as(OzoneDataRow.parser *)

  }

  def getOzoneFileDataForTheYearSamplerOne(year: Int): List[String] = db.withConnection { implicit connection =>

   SQL("""select  '50' -- country
         |   ||';'|| cl.ecenr -- plotnr
         |  ||';'|| to_char(TRUNC(beo.Math0.Hdms(cl.latitude+SIGN(cl.latitude)/7200)*10000),'S999999')
         |   ||';'|| to_char(TRUNC(beo.Math0.Hdms(cl.longitude+SIGN(cl.longitude)/7200)*10000),
         |'S099999') -- longitude rounded to seconds
         |   ||';'||TRUNC(((cl.z-1)/50)+1) -- altitude 50m classes
         |   ||';'|| ' O3'
         |   ||';'|| '1' -- Sampler ID
         |   ||';'|| '11' -- Passive sampler manufacturer
         |    ||';'|| to_char(min(passsamdat.startdat),'ddmmyy') -- Startdatum
         |   ||';'|| to_char(max(passsamdat.enddat),'ddmmyy') -- Enddatum
         |   ||';'|| '3'  -- number of measurements
         |   ||';'|| case passsamdat.clnr
         |               when 331934 then 'N'
         |               when 313136 then 'N'
         |               else 'Y'            -- Variable co-location
         |             end
         |   ||';'|| trunc(decode(cl_st.zmess,null,cl_st.z,cl_st.zmess)) -- Altitude (in m)
         |   ||';'|| station.min_z_2500 -- lowest elevation with r = 2.5 km (in m)
         |   ||';'|| station.min_z_5000 -- lowest elevation with r = 5.0 km (in m)
         |   ||';'|| '2.00' -- Sampling height in m (neu PJ 17.1.2013)
         |   ||';'|| substr(cl.clname,1,29) zeile
         |  from passsamdat, beo.cl cl, beo.cl cl_st, meteo.station
         |  where TO_char(passsamdat.enddat,'yyyy') = {year}
         |    and passsamdat.clnr = station.clnr
         |    and cl.clnr = station.clnrlwf
         |    and cl_st.clnr = station.clnr
         |    and passsamdat.clnr <> 337588
         |    AND passsamdat.blindsampler in (0,1)
         |    and passsamdat.konzdat1 != -9999
         |    and passsamdat.konzdat1 != -8888
              and passsamdat.clnr not in (374674,-1112,337588.1,337588.2,337588.3,337588.4,335577)
         |    group by passsamdat.clnr,cl.ecenr, cl.latitude, cl.longitude, cl.z, cl_st.zmess, cl_st.z, station.min_z_2500,station.min_z_5000, cl.clname
         |  order by cl.ecenr
         |""".stripMargin).on("year" -> year).as(SqlParser.str("zeile").+)

  }

  def getOzoneFileDataForTheYearSamplerTwo(year: Int): List[String] = db.withConnection { implicit connection =>

    SQL("""select  '50' -- country
          |   ||';'|| cl.ecenr -- plotnr
          |  ||';'|| to_char(TRUNC(beo.Math0.Hdms(cl.latitude+SIGN(cl.latitude)/7200)*10000),'S999999')
          |   ||';'|| to_char(TRUNC(beo.Math0.Hdms(cl.longitude+SIGN(cl.longitude)/7200)*10000),
          |'S099999') -- longitude rounded to seconds
          |   ||';'||TRUNC(((cl.z-1)/50)+1) -- altitude 50m classes
          |   ||';'|| ' O3'
          |   ||';'|| '2' -- Sampler ID
          |   ||';'|| '11' -- Passive sampler manufacturer
          |    ||';'|| to_char(min(passsamdat.startdat),'ddmmyy') -- Startdatum
          |   ||';'|| to_char(max(passsamdat.enddat),'ddmmyy') -- Enddatum
          |   ||';'|| '3'  -- number of measurements
          |   ||';'|| case passsamdat.clnr
          |               when 331934 then 'N'
          |               when 313136 then 'N'
          |               else 'Y'            -- Variable co-location
          |             end
          |   ||';'|| trunc(decode(cl_st.zmess,null,cl_st.z,cl_st.zmess)) -- Altitude (in m)
          |   ||';'|| station.min_z_2500 -- lowest elevation with r = 2.5 km (in m)
          |   ||';'|| station.min_z_5000 -- lowest elevation with r = 5.0 km (in m)
          |   ||';'|| '2.00' -- Sampling height in m (neu PJ 17.1.2013)
          |   ||';'|| substr(cl.clname,1,29) zeile
          |  from passsamdat, beo.cl cl, beo.cl cl_st, meteo.station
          |  where TO_char(passsamdat.enddat,'yyyy') = {year}
          |    and passsamdat.clnr = station.clnr
          |    and cl.clnr = station.clnrlwf
          |    and cl_st.clnr = station.clnr
          |    and passsamdat.clnr <> 337588
          |    AND passsamdat.blindsampler in (0,1)
          |    and passsamdat.konzdat2 != -9999
          |    and passsamdat.konzdat2 != -8888
               and passsamdat.clnr not in (374674,-1112,337588.1,337588.2,337588.3,337588.4,335577)
          |    group by passsamdat.clnr,cl.ecenr, cl.latitude, cl.longitude, cl.z, cl_st.zmess, cl_st.z, station.min_z_2500,station.min_z_5000, cl.clname
          |  order by cl.ecenr
          |""".stripMargin).on("year" -> year).as(SqlParser.str("zeile").+)

  }
  def getOzoneFileDataForTheYearSamplerThree(year: Int): List[String] = db.withConnection { implicit connection =>

    SQL("""select  '50' -- country
          |   ||';'|| cl.ecenr -- plotnr
          |  ||';'|| to_char(TRUNC(beo.Math0.Hdms(cl.latitude+SIGN(cl.latitude)/7200)*10000),'S999999')
          |   ||';'|| to_char(TRUNC(beo.Math0.Hdms(cl.longitude+SIGN(cl.longitude)/7200)*10000),
          |'S099999') -- longitude rounded to seconds
          |   ||';'||TRUNC(((cl.z-1)/50)+1) -- altitude 50m classes
          |   ||';'|| ' O3'
          |   ||';'|| '3' -- Sampler ID
          |   ||';'|| '11' -- Passive sampler manufacturer
          |    ||';'|| to_char(min(passsamdat.startdat),'ddmmyy') -- Startdatum
          |   ||';'|| to_char(max(passsamdat.enddat),'ddmmyy') -- Enddatum
          |   ||';'|| '3'  -- number of measurements
          |   ||';'|| case passsamdat.clnr
          |               when 331934 then 'N'
          |               when 313136 then 'N'
          |               else 'Y'            -- Variable co-location
          |             end
          |   ||';'|| trunc(decode(cl_st.zmess,null,cl_st.z,cl_st.zmess)) -- Altitude (in m)
          |   ||';'|| station.min_z_2500 -- lowest elevation with r = 2.5 km (in m)
          |   ||';'|| station.min_z_5000 -- lowest elevation with r = 5.0 km (in m)
          |   ||';'|| '2.00' -- Sampling height in m (neu PJ 17.1.2013)
          |   ||';'|| substr(cl.clname,1,29) zeile
          |  from passsamdat, beo.cl cl, beo.cl cl_st, meteo.station
          |  where TO_char(passsamdat.enddat,'yyyy') = {year}
          |    and passsamdat.clnr = station.clnr
          |    and cl.clnr = station.clnrlwf
          |    and cl_st.clnr = station.clnr
          |    and passsamdat.clnr <> 337588
          |    AND passsamdat.blindsampler in (0,1)
          |    and passsamdat.konzdat3 != -9999
          |    and passsamdat.konzdat3 != -8888
          |    and passsamdat.clnr not in (374674,-1112,337588.1,337588.2,337588.3,337588.4,335577)
          |    group by passsamdat.clnr,cl.ecenr, cl.latitude, cl.longitude, cl.z, cl_st.zmess, cl_st.z, station.min_z_2500,station.min_z_5000, cl.clname
          |  order by cl.ecenr
          |""".stripMargin).on("year" -> year).as(SqlParser.str("zeile").+)

  }






}


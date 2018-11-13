package models.domain

import anorm.SqlParser.get
import anorm.{RowParser, ~}
import models.util.StringToDate
import org.joda.time.DateTime

case class FileInfo(fileName: String, header: String, meteoData: List[String], logInformation: MeteoDataFileLogInfo)

case class Organisation(organisationNr: Int, prefix: String, organisationName: String)
object Organisations {
  val parser: RowParser[Organisation] = {
    get[Int]("ORGNR") ~
      get[String]("KURZNAME") ~
      get[String]("ORGNAME") map {
      case orgNr ~ prefix ~ orgName => Organisation(orgNr, prefix, orgName)
    }
  }
}

case class MeteoDataFileLogInfo(stationNr: Int, orgNr: Int, fileName: String, fromDate: DateTime, toDate: DateTime, numberOfLinesSent: Int, lastEinfDat: DateTime)

object MeteoDataFileLogsInfo {
  val parser: RowParser[MeteoDataFileLogInfo] = {
    get[Int]("STATNR")~
      get[Int]("ORGNR") ~
        get[String]("DATEINAME")~
          get[String]("VONDATUM") ~
            get[String]("BISDATUM") ~
              get[Int]("REIHEGESENDET") ~
                get[String]("LASTEINFDAT") map {
              case stationnr~orgNr~dateiName~vonDatum~bisDatum~numberOfLines~lastEinfDat => MeteoDataFileLogInfo(stationnr,orgNr,dateiName,StringToDate.stringToDateConvert(vonDatum),StringToDate.stringToDateConvert(bisDatum), numberOfLines,StringToDate.stringToDateConvert(lastEinfDat))
            }
  }
}

case class OrganisationStationMapping(statNr: Int, orgNr: Int, shouldSendData: Int, fileFormat: String)

object OrganisationStationMappingS {
  val parser: RowParser[OrganisationStationMapping] = {
    get[Int]("STATNR") ~
      get[Int]("ORGNR") ~
        get[Int]("SENDDATEN") ~
          get[String]("DATEIFORMAT")map {
          case stationnr ~ orgNr ~ sendDaten ~fileFormat => OrganisationStationMapping(stationnr, orgNr, sendDaten, fileFormat)
    }
  }
}


case class OrgStationParamMapping(statNr: Int, orgNr: Int, projnr: Int, paramId: Int, shortName: String, columnNr: Int)

object OrgStationParamMappings {
  val parser: RowParser[OrgStationParamMapping] = {
    get[Int]("STATNR") ~
      get[Int]("ORGNR") ~
      get[Int]("projnr") ~
      get[Int]("parameterid") ~
      get[String]("shortnamebyorg") ~
      get[Int]("columnnr") map {
      case stationnr ~ orgNr ~ projnr ~parameterId~shortName~columnNr => OrgStationParamMapping(stationnr, orgNr, projnr, parameterId, shortName, columnNr)
    }
  }
}


case class OrganisationProject(organisationNr: Int, projnr: Int)
object OrganisationProjects {
  val parser: RowParser[OrganisationProject] = {
    get[Int]("ORGNR") ~
      get[Int]("projnr") map {
      case orgNr ~ projnr => OrganisationProject(orgNr, projnr)
    }
  }
}
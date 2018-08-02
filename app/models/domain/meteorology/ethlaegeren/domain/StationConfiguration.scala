package models.domain.meteorology.ethlaegeren.domain

import anorm.SqlParser.get
import anorm.{RowParser, ~}


case class StationConfiguration(station: Int,
                                     measurementParam: Int,
                                     configNumber: Int,
                                     sensorNr: Option[Int],
                                     validFromDate: String,
                                     validToDate: Option[String],
                                     folgeNr: Option[Int],
                                     projectNr: Option[Int]
                                    )

object StationConfigurationRow {
  val parser: RowParser[StationConfiguration] = {
    get[Int]("STATION_ID") ~
      get[Int]("meas_parameters_id") ~
      get[Int]("konfid") ~
      get[Option[Int]]("sensors_id") ~
      get[String]("ABDATUM") ~
      get[Option[String]]("BISDATUM") ~
      get[Option[Int]]("column_nr") ~
      get[Option[Int]]("project_id") map {
      case station ~ messart ~ confnr ~ sensor ~ abdatum ~ bisdatum ~ folgnr ~ projnr => StationConfiguration(station, messart, confnr, sensor, abdatum, bisdatum, folgnr, projnr)
    }
  }
}

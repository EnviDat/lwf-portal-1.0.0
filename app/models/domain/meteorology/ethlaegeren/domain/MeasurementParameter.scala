package models.domain.meteorology.ethlaegeren.domain

import anorm.SqlParser.get
import anorm.{RowParser, ~}


case class MeasurementParameter(code: Int,text: String, period: Int, multi: Int)
object MeasurementParameterRow {
  val parser: RowParser[MeasurementParameter] = {
    get[Int]("ID") ~
      get[String]("NAME") ~
      get[Int]("DURATION_MINS") ~
      get[Int] ("MULTI") map {
      case id ~ name ~ duration ~ multi => {
        MeasurementParameter(id, name, duration, multi)
      }
    }
  }
}

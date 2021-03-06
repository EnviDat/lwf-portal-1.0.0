package models.domain

import org.joda.time.DateTime

case class TOA5LinesFormat(measurementTime: String, recordNr: Int, stationId: Int, projectId: Int, duration: Int, measurementValues: List[BigDecimal])

case class FixedLinesFormat(stationId: Int, measurementTime: String,  measurementValues: List[BigDecimal])

case class FixedLinesFormatWithDate(stationId: Int, measurementTime: String,  measurementValues: List[BigDecimal], measurementDate: DateTime)


case class TOA5LinesFormatHOB(measurementTime: String, recordNr: Int, stationId: String, projectId: Int, duration: Int, measurementValues: List[BigDecimal])

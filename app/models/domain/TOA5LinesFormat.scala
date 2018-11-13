package models.domain

case class TOA5LinesFormat(measurementTime: String, recordNr: Int, stationId: Int, projectId: Int, duration: Int, measurementValues: List[BigDecimal])

case class FixedLinesFormat(stationId: Int, measurementTime: String,  measurementValues: List[BigDecimal])

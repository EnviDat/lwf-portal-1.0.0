package models.domain

case class TOA5LinesFormat(measurementTime: String, recordNr: Int, stationId: Int, projectId: Int, duration: Int, measurementValues: List[BigDecimal])

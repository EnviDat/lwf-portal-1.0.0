package models.domain


case class Cr10LinesFormat(duration: Int, stationId: Int, projectId: Int, year: Int, yearToDate: Int, time: String, measurementValues: List[BigDecimal])

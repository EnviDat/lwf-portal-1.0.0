package models.domain.unibasel

import models.domain.StationAbbrevations

object UniBaselStationAbbrevations {

  val listOfProjects = List(1,4,5,35)

  val organisationNr = 3

  val listOfAllStationsAndAbbrvations = List(StationAbbrevations(192,"HOB", "Hoelstein_Bestand"),
    StationAbbrevations(193,"GP2_HOB_1_", "Hoelstein_Bestand_GP2_Logger_1"),
    StationAbbrevations(194,"GP2_HOB_2_", "Hoelstein_Bestand_GP2_Logger_2"),
    StationAbbrevations(195,"GP2_HOB_3_", "Hoelstein_Bestand_GP2_Logger_3"),
    StationAbbrevations(196,"GP2_HOB_4_", "Hoelstein_Bestand_GP2_Logger_4"),
    StationAbbrevations(197,"GP2_HOB_5_", "Hoelstein_Bestand_GP2_Logger_5"),
    StationAbbrevations(198,"GP2_HOB_6_", "Hoelstein_Bestand_GP2_Logger_6"),
    StationAbbrevations(199,"GP2_HOB_7_", "Hoelstein_Bestand_GP2_Logger_7"),
    StationAbbrevations(200,"GP2_HOB_8_", "Hoelstein_Bestand_GP2_Logger_8"))


  val mappingduration: Map[Int, String] = List(10 -> "10mins", 60 -> "60mins", 240 -> "240mins").toMap
  val mappingProjNr: Map[Int, String] = List(1 -> "WeatherData", 4 -> "DeviceStatus", 5 -> "SoilData", 35 -> "GP2").toMap


}

object HexenRubiStationAbbrevations {

  val projectNr = 36

  val stationNr = 190

  val listOfAllStationsAndAbbrvations = List(StationAbbrevations(190,"HEX", "Hexenrubi"))



}

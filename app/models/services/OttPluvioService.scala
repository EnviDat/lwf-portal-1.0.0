package models.services

import models.domain.OttPulvioDataRow
import models.util.{CurrentSysDateInSimpleFormat, StringToDate}
import schedulers.ConfigurationOttPluvioData

import scala.math.BigDecimal.RoundingMode


class OttPluvioService(config: ConfigurationOttPluvioData, meteoService: MeteoService) {
  def getAndSendDataForLastOneDay() = {
   val ottPulvioMeasurement: Seq[OttPulvioDataRow] = meteoService.getLastOneDayOttPulvioDataForStation(config.stationNrOttPluvio, config.messartOttPluvio)
    val emailList = config.emailUserListOttPluvio.split(";").toSeq
      EmailService.sendEmail("OttPluvio measurements", "LWF_Data_Processing@wsl.ch", emailList, emailList, "Niederschlag UIF", s"${CurrentSysDateInSimpleFormat.dateNowOnlyDay}: ${ottPulvioMeasurement.map(o => "Niderschlagssumme: " + o.sumPrecipitation.toString() + " mm, Anzahl Messungen: " + o.countValues.toString() + " = " + ((o.countValues + 1)/145 *100).setScale(2,RoundingMode.HALF_UP) + "%.").mkString("\n")} \n Bei Fragen: meteo@wsl.ch oder simpal.kumar@wsl.ch")
    }

  def getAndSendDataForLast15Days() = {
    val last15DaysDates = meteoService.findAllDaysBetweenLast15Days()

    val ottPulvioMeasurement: Seq[OttPulvioDataRow] = meteoService.getLastOneDayOttPulvioDataForStation(config.stationNrOttPluvio, config.messartOttPluvio)
    val emailList = config.emailUserListOttPluvio.split(";").toSeq
    EmailService.sendEmail("OttPluvio measurements", "simpal.kumar@wsl.ch", emailList, emailList, "OttPluvio measurements for the day", s"Date: ${CurrentSysDateInSimpleFormat.dateNowOnlyDay}, ${ottPulvioMeasurement.map(o => "sum of precipitation (mm): " + o.sumPrecipitation.toString() + ", count: " + o.countValues.toString()).mkString("\n")}")
  }

}

package models.services

import models.domain.{OttPulvioDataRow, WeeklyPreciVordemwaldData, WeeklyPreciVordemwaldDataRow}
import models.util.CurrentSysDateInSimpleFormat
import schedulers.{ConfigurationOttPluvioData, ConfigurationPreciVordemwaldData}

import scala.math.BigDecimal.RoundingMode


class WeeklyPrecipitationService(config: ConfigurationPreciVordemwaldData, meteoService: MeteoService) {
  def getAndSendDataForLastWeekVORB() = {
   val ottPulvioMeasurement: Seq[WeeklyPreciVordemwaldDataRow] = meteoService.getLastOneWeekDataForStation(config.stationNrPreciVordemwaldB, config.messartPreciVordemwald)
    val emailList = config.emailUserListPreciVordemwald.split(";").toSeq
      EmailService.sendEmail("Vordemwald Bestand measurements", "LWF_Data_Processing@wsl.ch", emailList, emailList, "Niederschlag VOR B", s"${ottPulvioMeasurement.map(o => o.tag + "," + o.measdate + ": Niederschlagssumme: " + o.sumPrecipitation.toString() + " mm, Anzahl Messungen: " + o.countValues.toString() + " = " + ((o.countValues)/144 *100).setScale(2,RoundingMode.HALF_UP) + "%.").mkString("\n")} \n " +
        s"Für jedes Datum wurde der Niederschlag von Mitternacht (00:00) bis 23:59 Uhr aufsummiert. Bei Fragen: simpal.kumar@wsl.ch")
    }

  def getAndSendDataForLastWeekVORF() = {
    val ottPulvioMeasurement: Seq[WeeklyPreciVordemwaldDataRow] = meteoService.getLastOneWeekDataForStation(config.stationNrPreciVordemwaldF, config.messartPreciVordemwald)
    val emailList = config.emailUserListPreciVordemwald.split(";").toSeq
    EmailService.sendEmail("Vordemwald Freiland measurements", "LWF_Data_Processing@wsl.ch", emailList, emailList, "Niederschlag VOR F", s"${ottPulvioMeasurement.map(o => o.tag + "," + o.measdate + ": Niederschlagssumme: " + o.sumPrecipitation.toString() + " mm, Anzahl Messungen: " + o.countValues.toString() + " = " + ((o.countValues)/144 *100).setScale(2,RoundingMode.HALF_UP) + "%.").mkString("\n")} \n " +
      s"Für jedes Datum wurde der Niederschlag von Mitternacht (00:00) bis 23:59 Uhr aufsummiert. Bei Fragen: meteo@wsl.ch oder simpal.kumar@wsl.ch")
  }
}

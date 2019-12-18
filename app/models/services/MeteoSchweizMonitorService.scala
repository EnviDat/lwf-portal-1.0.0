package models.services

import models.domain.OttPulvioDataRow
import models.util.CurrentSysDateInSimpleFormat
import org.joda.time.DateTime
import schedulers.{ConfigurationMeteoSchweizReportData, ConfigurationOttPluvioData}

import scala.math.BigDecimal.RoundingMode


class MeteoSchweizMonitorService(config: ConfigurationMeteoSchweizReportData, meteoService: MeteoService) {
  def getAndSendReportForLastTimeDataSentToMeteoSchweiz() = {
   val deliveryStatusData = meteoService.getLastTimeDataWasSentOutMeteoSchweiz()
    val emailList = config.emailListMeteoSchweizMonitoring.split(";").toSeq

    val freilandStations = deliveryStatusData.filter(_.fileName.contains("F"))
    val freilandStationOlder = freilandStations.filter(_.toDate.isBefore((new DateTime()).minusHours(2)))
    val freilandStationOnTime = freilandStations.filter(_.toDate.isAfter((new DateTime()).minusHours(2)))

    val bestandStations = deliveryStatusData.filterNot(_.fileName.contains("F"))
    val bestandStationsOlder = bestandStations.filter(_.toDate.isBefore((new DateTime()).minusHours(2)))
    val bestandStationsOnTime = bestandStations.filter(_.toDate.isAfter((new DateTime()).minusHours(2)))

   val formattedMessage =  s"Freiland Stations delayed: \n" +
     s"${freilandStationOlder.map(o => "StationNr: " + o.stationNr.toString() + ", " +
       "FileName: " + o.fileName.toString() + ", NrOfLines :" + o.numberOfLinesSent + ", From DateTime: "
       + o.fromDate + ", To DateTime: " + o.toDate + ".").mkString("\n")}" +
     s"\nFreiland Stations On Time: \n" +
     s"${freilandStationOnTime.map(o => "StationNr: " + o.stationNr.toString() + ", " +
       "FileName: " + o.fileName.toString() + ", NrOfLines :" + o.numberOfLinesSent + ", From DateTime: "
       + o.fromDate + ", To DateTime: " + o.toDate + ".").mkString("\n")}" +
       s"\nBestand Station Delayed: \n" +
     s"${bestandStationsOlder.map(o => "StationNr: " + o.stationNr.toString() + ", " +
       "FileName: " + o.fileName.toString() + ", NrOfLines :" + o.numberOfLinesSent + ", From DateTime: "
       + o.fromDate + ", To DateTime: " + o.toDate + ".").mkString("\n")}" +
       s"\nBestand Station On Time: \n" +
     s"${bestandStationsOnTime.map(o => "StationNr: " + o.stationNr.toString() + ", " +
       "FileName: " + o.fileName.toString() + ", NrOfLines :" + o.numberOfLinesSent + ", From DateTime: "
       + o.fromDate + ", To DateTime: " + o.toDate + ".").mkString("\n")}" +
       s" \nBei Fragen: simpal.kumar@wsl.ch"

      EmailService.sendEmail("Meteo Schweiz Data Delivery report", "LWF_Data_Delivery@wsl.ch", emailList, emailList, "Meteo Schweiz Data Delivery Report", formattedMessage)
    }
}

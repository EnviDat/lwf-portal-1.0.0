package schedulers

import akka.actor.Actor
import javax.inject.{Inject, Singleton}
import models.services.{MeteoSchweizMonitorService, MeteoService, OttPluvioService}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorMonitoringMeteoSwiss @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "sendMonitorEmail" =>  {
      val config = ConfigurationLoader.loadMeteoSchweizMonitorConfiguration(configuration)
      sendMeasurementEmail(config)
      //readFile(config)
    }
  }

  def sendMeasurementEmail(config: ConfigurationMeteoSchweizReportData): Unit ={
    Logger.info("Monitoring Meteo Schweiz Delivery job started")

    val emailList = config.emailListMeteoSchweizMonitoring
    val monitoringService = new MeteoSchweizMonitorService(config, meteoService)
    monitoringService.getAndSendReportForLastTimeDataSentToMeteoSchweiz()
    //FtpConnector.readFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo)
     //DatFileReader.readFilesFromFile(pathInputFile)
    Logger.info("Monitoring Meteo Schweiz Delivery job finished")
  }


}

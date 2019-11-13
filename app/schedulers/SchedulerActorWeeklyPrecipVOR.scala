package schedulers

import akka.actor.Actor
import javax.inject.{Inject, Singleton}
import models.services.{MeteoService, OttPluvioService, WeeklyPrecipitationService}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext
import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

@Singleton
class SchedulerActorWeeklyPrecipVOR @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "sendWeeklyMeasurementEmail" =>  {
      val config = ConfigurationLoader.loadPreciVordemwaldConfiguration(configuration)
      sendMeasurementEmail(config)
      //readFile(config)
    }
  }

  def sendMeasurementEmail(config: ConfigurationPreciVordemwaldData): Unit ={
    Logger.info("Precipitation Vordemwald job started")

    val ottpluvioService = new WeeklyPrecipitationService(config, meteoService)
    ottpluvioService.getAndSendDataForLastWeekVORB()
    ottpluvioService.getAndSendDataForLastWeekVORF()
    //FtpConnector.readFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo)
     //DatFileReader.readFilesFromFile(pathInputFile)
    Logger.info("Precipitation Vordemwald job finished")
  }


}

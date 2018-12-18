package schedulers

import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.{MeteoService, MeteorologyService}
import models.util.FtpConnector
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorETHLaegeren @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processEthLaeFile" =>  {

      val config = ConfigurationLoader.loadETHLaeConfiguration(configuration)
      //processFile(config)
      //readFile(config)
    }
  }

  def processFile(config: ETHLaegerenLoggerFileConfig): Unit ={
    val pathForArchiveFiles = config.ftpPathForETHLaeArchiveFiles
    Logger.info("processing data task running")
    FtpConnector.readETHLaeFileFromFtp(config, meteoService, pathForArchiveFiles)
    Logger.info("File processing task finished")
  }
}

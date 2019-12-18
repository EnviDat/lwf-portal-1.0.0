package schedulers

import akka.actor.Actor
import javax.inject.{Inject, Singleton}
import models.services.MeteoService
import models.util.FtpConnector
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorETHDavosTower @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processEthDavosTFile" =>  {

      val config = ConfigurationLoader.loadETHDavTConfiguration(configuration)
      processFile(config)
      //readFile(config)
    }
  }

  def processFile(config: ETHDavosLoggerFileConfig): Unit ={
    val pathForArchiveFiles = config.ftpPathForETHDavArchiveFiles
    Logger.info("processing data task running")
    FtpConnector.readETHDavosTFileFromFtp(config, meteoService, pathForArchiveFiles)
    Logger.info("File processing task finished")
  }
}

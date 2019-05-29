package schedulers

import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.MeteoService
import models.util.FtpConnector
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorGP2Logger @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processGP2LoggerFile" =>  {

      val config = ConfigurationLoader.loadGP2LoggerConfiguration(configuration)
       processFile(config)
      //readFile(config)
    }
  }

  def processFile(config: CR1000LoggerFileConfig): Unit ={
    val userNameFtp = config.fptUserNameCR1000
    val passwordFtp = config.ftpPasswordCR1000
    val pathForFtpFolder = config.ftpPathForIncomingFileCR1000
    val ftpUrlMeteo = config.ftpUrlCR1000
    val pathForFaultyFiles = config.ftpPathForCR1000FaultyFile
    val pathForArchiveFiles = config.ftpPathForCR1000ArchiveFiles
    val stationKonfigs = config.stationConfigs
    val emailUserList = config.emailUserList
    Logger.info("processing data task running")
    FtpConnector.readCrdCR1000FileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, stationKonfigs, emailUserList, meteoService, pathForArchiveFiles)
    Logger.info("File processing task finished")
  }
}

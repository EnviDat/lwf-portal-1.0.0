package schedulers

import java.io.File
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.{FileGeneratorFromDB, MeteoService}
import models.util.{CurrentSysDateInSimpleFormat, DirectoryCompressor, FtpConnector}
import org.apache.commons.io.FileUtils
import play.api.Configuration
import play.api.Logger

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorCR1000 @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processFile" =>  {

      val config = ConfigurationLoader.loadCR1000Configuration(configuration)
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
    FtpConnector.readFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, stationKonfigs, emailUserList, meteoService, pathForArchiveFiles)

    Logger.info("File processing task finished")
  }
}

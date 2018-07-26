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
class SchedulerActorOzone @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processOzoneFile" =>  {

      val config = ConfigurationLoader.loadOzoneConfiguration(configuration)
      processFile(config)
      //readFile(config)
    }
  }

  def processFile(config: OzoneFileConfig): Unit ={
    val userNameFtp = config.fptUserNameOzone
    val passwordFtp = config.ftpPasswordOzone
    val pathForFtpFolder = config.ftpPathForIncomingFileOzone
    val ftpUrlMeteo = config.ftpUrlOzone
    val pathForFaultyFiles = config.ftpPathForOzoneFaultyFile
    val pathForArchiveFiles = config.ftpPathForOzoneArchiveFiles
    val emailUserList = config.emailUserList
    Logger.info("processing data task running")
    FtpConnector.readOzoneCSVFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, emailUserList, meteoService, pathForArchiveFiles)

    Logger.info("File processing task finished")
  }
}

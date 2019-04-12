package schedulers

import java.io.File
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.{MeteoService, OzoneFileGeneratorFromDB, PhanoService}
import models.util.{CurrentSysDateInSimpleFormat, DirectoryCompressor, FtpConnector}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorPhano @Inject()(configuration: Configuration, meteoService: PhanoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processPhanoFile" =>  {

      val config = ConfigurationLoader.loadPhanoConfiguration(configuration)
      //processFile(config)
      //writeFile(config)
      //writeICPSubmissionAQPFile(config)
      //writeICPSubmissionAQBFile(config)
      //writeICPSubmissionPPSFile(config)
    }
  }

  def processFile(config: PhanoFileConfig): Unit ={
    val userNameFtp = config.fptUserNamePhano
    val passwordFtp = config.ftpPasswordPhano
    val pathForFtpFolder = config.ftpPathForIncomingFilePhano
    val ftpUrlMeteo = config.ftpUrlPhano
    val pathForFaultyFiles = config.ftpPathForPhanoFaultyFile
    val pathForArchiveFiles = config.ftpPathForPhanoeArchiveFiles
    val emailUserList = config.emailUserList
    Logger.info("processing data task running")
    FtpConnector.readPhanoCSVFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, emailUserList, meteoService, pathForArchiveFiles)

    Logger.info("File processing task finished")
  }

}

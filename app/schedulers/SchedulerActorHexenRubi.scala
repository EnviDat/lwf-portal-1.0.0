package schedulers

import java.io.File
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.{FileGeneratorUniBaselLoggerFormat, MeteoService}
import models.util.{CurrentSysDateInSimpleFormat, DirectoryCompressor, FtpConnector}
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Logger}
import jcifs.smb.NtlmPasswordAuthentication
import jcifs.smb.SmbFile
import jcifs.smb.SmbFileOutputStream

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorHexenRubi @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processFile" =>  {
      val config = ConfigurationLoader.loadHexenRubiConfiguration(configuration)
      //processFile(config)
      //readFile(config)
    }
  }

  def readFile(config: ConfigurationHexenrubiData): Unit ={
    val pathInputFile = config.pathForIncomingFileHexenRubi
    val userNameFtp = config.userNameHexenRubi
    val passwordFtp = config.passwordHexenRubi
    val pathForFtpFolder = config.pathForArchivedFiles
//    FtpConnector.readFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo)
     //DatFileReader.readFilesFromFile(pathInputFile)
    Logger.info(pathInputFile)
  }

  def processFile(config: ConfigurationHexenrubiData): Unit ={
    val pathInputFile = config.pathForIncomingFileHexenRubi

    val pathForArchivedFiles = config.pathForArchivedFiles
    Logger.info("looking into network drive task running")

    val userName= config.userNameHexenRubi
    val password = config.passwordHexenRubi
    val auth = new NtlmPasswordAuthentication("wsl.ch" , userName , password)
    val sFile = new SmbFile(pathInputFile, auth).listFiles()
    /*val sfos = new SmbFileOutputStream(sFile)
    sfos.write("Test".getBytes)*/
    Logger.info(s"Destination for archive files ${pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip"} ")
    Logger.info("writing data task finished")
  }
}

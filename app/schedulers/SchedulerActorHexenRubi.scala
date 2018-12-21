package schedulers

import java.io._
import java.net.MalformedURLException
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services._
import models.util.{CurrentSysDateInSimpleFormat, DirectoryCompressor, FtpConnector}
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Logger}
import jcifs.smb.{SmbException, _}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorHexenRubi @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processFile" =>  {
      val config = ConfigurationLoader.loadHexenRubiConfiguration(configuration)
      processFile(config)
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
    val emailList = config.emailUserListHexenRubi.split(";").toSeq

    for(f <- sFile) {
      if (f.getName.startsWith(config.dataFileNameHexenRubi) && (f.getName.endsWith(".dat") || f.getName.endsWith(".Dat") || f.getName.endsWith(".DAT"))) {

        val content = readFileContent(f).toList
        val caughtExceptions = HexenRubiFileParser.parseAndSaveData(content, meteoService, f.getName, config.stationNrHexenRubi, config.projectNrHexenRubi, config.periodeHexenRubi)
        caughtExceptions match {
          case None => {
            EmailService.sendEmail("HexenRubi File Processor", "CR1000_Data_Processing@klaros.wsl.ch", emailList, emailList, "Hexenrubi File Processing Report OK", s"file Processed Report${f.getName}. \n PS: ***If there is any change in  wind direction and wind speed parameter, please contact Database Manager LWF to change in DB and Akka config.}")
          }
            case Some(_)
            => EmailService.sendEmail("HexenRubi File Processor", "CR1000_Data_Processing@klaros.wsl.ch", emailList, emailList, "Hexenrubi File Processing Report With errors", s"file Processed Report${f.getName}. \n PS: ***If there is any change in  wind direction and wind speed parameter, please contact Database Manager LWF to change in DB and Akka config.}}")
          }
            Logger.info(s"content of file read:${content}")
      }
    }
    /*val sfos = new SmbFileOutputStream(sFile)
    sfos.write("Test".getBytes)*/
    Logger.info(s"Destination for archive files ${pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip"} ")
    Logger.info("processing data task finished")
  }

  private def  readFileContent( sFile: SmbFile):   Seq[String]= {
    var reader: BufferedReader  = null
    try {
      reader = new BufferedReader(new InputStreamReader(new SmbFileInputStream(sFile)))
      val linesParsed: Seq[String] = Stream.continually(reader.readLine()).takeWhile(_ != null).toList
      Logger.info("lines read")
      linesParsed
    } catch {
      case ex =>
      Logger.debug(s"smb exception:${ex}")
        Seq()
    }
  }
}

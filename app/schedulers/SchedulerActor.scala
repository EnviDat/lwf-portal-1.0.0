package schedulers

import java.io.File
import javax.inject.{Inject, Singleton}
import org.apache.commons.io.FileUtils
import akka.actor.Actor
import models.services.{FileGeneratorFromDB, MeteoService}
import models.util.{CurrentSysDateInSimpleFormat, DirectoryCompressor, FtpConnector}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActor @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "writeFile" =>  {
      writeFile()
      readFile()
    }
  }

  def readFile(): Unit ={
    val pathInputFile = configuration.getString("pathInputFile").get
    val userNameFtp = configuration.getString("fptUserNameMeteo").get
    val passwordFtp = configuration.getString("ftpPasswordMeteo").get
    val pathForFtpFolder = configuration.getString("ftpPathForOutgoingFile").get
    val ftpUrlMeteo = configuration.getString("ftpUrlMeteo").get
//    FtpConnector.readFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo)
     //DatFileReader.readFilesFromFile(pathInputFile)
    Logger.info(pathInputFile)
  }

  def writeFile(): Unit ={
    val pathInputFile = configuration.getString("pathInputFile").get
    val userNameFtp = configuration.getString("fptUserNameMeteo").get
    val passwordFtp = configuration.getString("ftpPasswordMeteo").get
    val pathForFtpFolder = configuration.getString("ftpPathForOutgoingFile").get
    val ftpUrlMeteo = configuration.getString("ftpUrlMeteo").get
    val pathForLocalWrittenFiles = configuration.getString("pathForLocalWrittenFiles").get
    val pathForArchivedFiles = configuration.getString("pathForArchivedFiles").get
    Logger.info("writing data task running")
    val fileGenerator =  new FileGeneratorFromDB(meteoService)
    val fileInfos = fileGenerator.generateFiles()
    val logInformation = fileInfos.map(_.logInformation)
    fileInfos.toList.map( ff => {

        FtpConnector.writeFileToFtp( ff.header :: ff.meteoData, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, ff.fileName)
    })
    val source = new File(pathForLocalWrittenFiles)
    val destination = new File(pathForArchivedFiles + "generatedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip")
    destination.setReadable(true, false)
    destination.setExecutable(true, false)
    destination.setWritable(true, false)
    DirectoryCompressor.compressAllFiles(source,destination)
    //ZipUtil.packEntry(new File("\\csvFiles\\"), new File("\\csvFiles" + ".zip"))
    fileGenerator.saveLogInfoOfGeneratedFiles(logInformation)
    FileUtils.cleanDirectory(source)
    Logger.info("writing data task finished")
  }
}

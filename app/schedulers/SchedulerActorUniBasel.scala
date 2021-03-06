package schedulers

import java.io.File
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.{FileGeneratorUniBaselLoggerFormat, FileGeneratorUniBaselHistoricalLoggerFormat, MeteoService}
import models.util.{CurrentSysDateInSimpleFormat, DirectoryCompressor, FtpConnector}
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorUniBasel @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "writeFile" =>  {
      val config = ConfigurationLoader.loadUniBaselConfiguration(configuration)
      writeFile(config)
      //readFile(config)
    }
  }

  def readFile(config: ConfigurationUniBaselData): Unit ={
    val pathInputFile = config.pathInputFile
    val userNameFtp = config.userNameFtp
    val passwordFtp = config.passwordFtp
    val pathForFtpFolder = config.pathForFtpFolder
    val ftpUrlMeteo = config.ftpUrlMeteo
//    FtpConnector.readFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo)
     //DatFileReader.readFilesFromFile(pathInputFile)
    Logger.info(pathInputFile)
  }

  def writeFile(config: ConfigurationUniBaselData): Unit ={
    val pathInputFile = config.pathInputFile
    val userNameFtp = config.userNameFtp
    val passwordFtp = config.passwordFtp
    val pathForFtpFolder = config.pathForFtpFolder
    val ftpUrlMeteo = config.ftpUrlMeteo
    val pathForLocalWrittenFiles = config.pathForLocalWrittenFiles
    val pathForArchivedFiles = config.pathForArchivedFiles
    Logger.info("writing data task running")
    val fileGenerator =  new FileGeneratorUniBaselLoggerFormat(meteoService)
    val fileInfos = fileGenerator.generateFiles()
    val logInformation = fileInfos.filter(_.fileName.startsWith("NoData") == false).map(_.logInformation)
    Logger.info(s"Generated File Information:${logInformation} ")
    val pathForTempFiles = config.pathForTempFiles

    fileInfos.toList.map( ff => {
      if(!ff.fileName.startsWith("NoData")) {
        FtpConnector.writeFileToFtp(List(ff.header) ::: ff.meteoData, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, ff.fileName, pathForLocalWrittenFiles, ".DAT", pathForTempFiles)
      }
    })
    val source = new File(pathForLocalWrittenFiles)
    Logger.info(s"Source for local written files pathForLocalWrittenFiles : ${source.getName} ")
    val destination = new File(pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip")
    Logger.info(s"Destination for archive files ${pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + "_BASEL.zip"} ")

    destination.setReadable(true, false)
    destination.setExecutable(true, false)
    destination.setWritable(true, false)
    Logger.info("Compressing the files generated")
    DirectoryCompressor.compressAllFiles(source,destination)
    //ZipUtil.packEntry(new File("\\csvFiles\\"), new File("\\csvFiles" + ".zip"))
    fileGenerator.saveLogInfoOfGeneratedFiles(logInformation)
    FileUtils.cleanDirectory(source)
    Logger.info("writing data task finished")
  }
}

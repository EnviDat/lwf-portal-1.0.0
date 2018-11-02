package schedulers

import java.io.File
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.{FileGeneratorFromDB, MeteoService, OzoneFileGeneratorFromDB}
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
      //processFile(config)
      //writeFile(config)
      //writeICPSubmissionAQPFile(config)
      //writeICPSubmissionAQBFile(config)
      //writeICPSubmissionPPSFile(config)
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

  def writeFile(config: OzoneFileConfig): Unit ={
    val userNameFtp = config.fptUserNameOzone
    val passwordFtp = config.ftpPasswordOzone
    val pathForFtpFolder = config.ftpPathForIncomingFileOzone
    val ftpUrlMeteo = config.ftpUrlOzone
    val pathForLocalWrittenFiles = config.ftpPathForOzoneArchiveFiles
    val pathForArchivedFiles = config.ftpPathForOzoneArchiveFiles
    Logger.info("writing data task running")
    val fileGenerator =  new OzoneFileGeneratorFromDB(meteoService)
    val years = List(2000,2001,2002,2003,2004,2005,2006,2007,2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018)
    val fileInfos = fileGenerator.generateFiles(years)
    fileInfos.toList.map( ff => {
      FtpConnector.writeFileToFtp(List(ff.header) ::: ff.ozoneData, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, ff.fileName, pathForLocalWrittenFiles,".csv")
    })
    val source = new File(pathForLocalWrittenFiles)
    Logger.info(s"Source for local written files pathForLocalWrittenFiles : ${source.getName} ")
    val destination = new File(pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip")
    Logger.info(s"Destination for archive files ${pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip"} ")

    destination.setReadable(true, false)
    destination.setExecutable(true, false)
    destination.setWritable(true, false)
    Logger.info("Compressing the files generated")
    DirectoryCompressor.compressAllFiles(source,destination)
    //ZipUtil.packEntry(new File("\\csvFiles\\"), new File("\\csvFiles" + ".zip"))
    //FileUtils.cleanDirectory(source)
    Logger.info("writing data task finished")
  }

  def writeICPSubmissionAQPFile(config: OzoneFileConfig): Unit ={
    val userNameFtp = config.fptUserNameOzone
    val passwordFtp = config.ftpPasswordOzone
    val pathForFtpFolder = config.ftpPathForIncomingFileOzone
    val ftpUrlMeteo = config.ftpUrlOzone
    val pathForLocalWrittenFiles = config.ftpPathForOzoneArchiveFiles
    val pathForArchivedFiles = config.ftpPathForOzoneArchiveFiles
    Logger.info("icp forests data task running")
    val fileGenerator =  new OzoneFileGeneratorFromDB(meteoService)
    val years = List(2004) //List(2000,2001,2002,2003,2004,2005,2006,2007,2008,2009,2010,2011,2012,2013,2014,2015,2016,2017)
    val fileInfos = fileGenerator.generateICPForestsAQPFiles(years)
    fileInfos.toList.map( ff => {
      FtpConnector.writeFileToFtp(List(ff.header) ::: ff.ozoneData, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, ff.fileName, pathForLocalWrittenFiles,".aqp")
    })
    val source = new File(pathForLocalWrittenFiles)
    Logger.info(s"Source for local written files pathForLocalWrittenFiles : ${source.getName} ")
    val destination = new File(pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip")
    Logger.info(s"Destination for archive files ${pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip"} ")

    destination.setReadable(true, false)
    destination.setExecutable(true, false)
    destination.setWritable(true, false)
    Logger.info("Compressing the files generated")
    DirectoryCompressor.compressAllFiles(source,destination)
    //ZipUtil.packEntry(new File("\\csvFiles\\"), new File("\\csvFiles" + ".zip"))
    //FileUtils.cleanDirectory(source)
    Logger.info("icp forest data task finished")
  }

  def writeICPSubmissionAQBFile(config: OzoneFileConfig): Unit ={
    val userNameFtp = config.fptUserNameOzone
    val passwordFtp = config.ftpPasswordOzone
    val pathForFtpFolder = config.ftpPathForIncomingFileOzone
    val ftpUrlMeteo = config.ftpUrlOzone
    val pathForLocalWrittenFiles = config.ftpPathForOzoneArchiveFiles
    val pathForArchivedFiles = config.ftpPathForOzoneArchiveFiles
    Logger.info("icp forests data task running")
    val fileGenerator =  new OzoneFileGeneratorFromDB(meteoService)
    val years = List(2000,2001,2002,2003,2004,2005,2006,2007,2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018)
    val fileInfos = fileGenerator.generateICPForestsAQBFiles(years)
    fileInfos.toList.map( ff => {
      FtpConnector.writeFileToFtp(List(ff.header) ::: ff.ozoneData, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, ff.fileName, pathForLocalWrittenFiles,".aqb")
    })
    val source = new File(pathForLocalWrittenFiles)
    Logger.info(s"Source for local written files pathForLocalWrittenFiles : ${source.getName} ")
    val destination = new File(pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip")
    Logger.info(s"Destination for archive files ${pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip"} ")

    destination.setReadable(true, false)
    destination.setExecutable(true, false)
    destination.setWritable(true, false)
    Logger.info("Compressing the files generated")
    DirectoryCompressor.compressAllFiles(source,destination)
    //ZipUtil.packEntry(new File("\\csvFiles\\"), new File("\\csvFiles" + ".zip"))
    //FileUtils.cleanDirectory(source)
    Logger.info("icp forest data task finished")
  }

  def writeICPSubmissionPPSFile(config: OzoneFileConfig): Unit ={
    val userNameFtp = config.fptUserNameOzone
    val passwordFtp = config.ftpPasswordOzone
    val pathForFtpFolder = config.ftpPathForIncomingFileOzone
    val ftpUrlMeteo = config.ftpUrlOzone
    val pathForLocalWrittenFiles = config.ftpPathForOzoneArchiveFiles
    val pathForArchivedFiles = config.ftpPathForOzoneArchiveFiles
    Logger.info("icp forests data task running")
    val fileGenerator =  new OzoneFileGeneratorFromDB(meteoService)
    val years = List(2011,2012,2013,2014,2015,2016,2017,2018)//List(2000,2001,2002,2003,2004,2005,2006,2007,2008,2009,2010,2011,2012,2013,2014,2015,2016,2017,2018)
    val fileInfos = fileGenerator.generateICPForestsPPSFiles(years)
    fileInfos.toList.map( ff => {
      FtpConnector.writeFileToFtp(List(ff.header) ::: ff.ozoneData, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, ff.fileName, pathForLocalWrittenFiles,".pps")
    })
    val source = new File(pathForLocalWrittenFiles)
    Logger.info(s"Source for local written files pathForLocalWrittenFiles : ${source.getName} ")
    val destination = new File(pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip")
    Logger.info(s"Destination for archive files ${pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip"} ")

    destination.setReadable(true, false)
    destination.setExecutable(true, false)
    destination.setWritable(true, false)
    Logger.info("Compressing the files generated")
    DirectoryCompressor.compressAllFiles(source,destination)
    //ZipUtil.packEntry(new File("\\csvFiles\\"), new File("\\csvFiles" + ".zip"))
    //FileUtils.cleanDirectory(source)
    Logger.info("icp forest data task finished")
  }


}

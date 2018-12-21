package schedulers

import java.io.File
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.{FileGeneratorMeteoSchweizHEXFixedAggregatedFormat, FileGeneratorMeteoSchweizFixedFormat, MeteoService}
import models.util.{CurrentSysDateInSimpleFormat, DirectoryCompressor, FtpConnector}
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorHexenRübiFixedFormatExport @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "writeFile" =>  {
      val configMeteoSwiss = ConfigurationLoader.loadMeteoSchweizConfiguration(configuration)
      val configHexenrubi: ConfigurationHexenrubiData = ConfigurationLoader.loadHexenRubiConfiguration(configuration)

      writeFile(configMeteoSwiss, configHexenrubi)
      //readFile(config)
    }
  }

  def readFile(config: ConfigurationMeteoSchweizData): Unit ={
    val pathInputFile = config.pathInputFile
    val userNameFtp = config.userNameFtp
    val passwordFtp = config.passwordFtp
    val pathForFtpFolder = config.pathForFtpFolder
    val ftpUrlMeteo = config.ftpUrlMeteo
   //FtpConnector.readFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo)
   //DatFileReader.readFilesFromFile(pathInputFile)
    Logger.info(pathInputFile)
  }

  def writeFile(configMeteoSwiss: ConfigurationMeteoSchweizData, configHexenrubi: ConfigurationHexenrubiData): Unit ={
    val pathInputFile = configMeteoSwiss.pathInputFile
    val userNameFtp = configMeteoSwiss.userNameFtp
    val passwordFtp = configMeteoSwiss.passwordFtp
    val pathForFtpFolder = configMeteoSwiss.pathForFtpFolder
    val ftpUrlMeteo = configMeteoSwiss.ftpUrlMeteo
    val pathForLocalWrittenFiles = configMeteoSwiss.pathForLocalWrittenFiles
    val pathForArchivedFiles = configMeteoSwiss.pathForArchivedFiles
    Logger.info("writing data task running")
    val fileGenerator =  new FileGeneratorMeteoSchweizHEXFixedAggregatedFormat(meteoService)
    val fileInfos = fileGenerator.generateFiles(configHexenrubi.specialStationKonfNrsHexenRubi)
    val logInformation = fileInfos.map(_.logInformation)
    Logger.info(s"Generated File Information:${logInformation} ")
    fileInfos.toList.map( ff => {
        FtpConnector.writeFileToFtp(List(ff.header) ::: ff.meteoData, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, ff.fileName, pathForLocalWrittenFiles, ".DAT")
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
    fileGenerator.saveLogInfoOfGeneratedFiles(logInformation)
    FileUtils.cleanDirectory(source)
    Logger.info("writing data task finished")
  }
}

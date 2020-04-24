package schedulers

import java.io._
import java.net.MalformedURLException
import java.sql.Date
import java.util

import javax.inject.{Inject, Singleton}
import akka.actor.Actor
import com.jcraft.jsch.{ChannelSftp, JSch}
import models.services._
import models.util.{CurrentSysDateInSimpleFormat, DirectoryCompressor, FtpConnector, StringToDate}
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Logger}
import jcifs.smb.{SmbException, _}
import models.domain.CR1000OracleError
import org.joda.time.{DateTime, Days}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorCR10X @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processFile" =>  {
      val config = ConfigurationLoader.loadCR10XConfiguration(configuration)
      processFile(config)
      //readFile(config)
    }
  }

  def readFile(config: ConfigurationCR10XData): Unit ={
    val pathInputFile = config.pathForIncomingFileHexenRubi
    val userNameFtp = config.userNameHexenRubi
    val passwordFtp = config.passwordHexenRubi
    val pathForFtpFolder = config.pathForArchivedFiles
//    FtpConnector.readFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo)
     //DatFileReader.readFilesFromFile(pathInputFile)
    Logger.info(pathInputFile)
  }

  def processFile(config: ConfigurationCR10XData): Unit ={
    val pathInputFile = config.pathForIncomingFileHexenRubi

    val pathForArchivedFiles = config.pathForArchivedFiles
    Logger.info("looking into network drive for Hexenrubi File")
    val emailList = config.emailUserListHexenRubi.split(";").toSeq

    val userName= config.userNameHexenRubi
    val password = config.passwordHexenRubi
    val jsch = new JSch
    try {

      val session = jsch.getSession(userName,config.ftpUrlCR10X, 22)
      session.setPassword(password)
      session.setConfig("StrictHostKeyChecking", "no")
      session.setConfig("PreferredAuthentications",
        "publickey,keyboard-interactive,password")
      session.connect()
      val channel = session.openChannel("sftp")
      channel.connect()
      val sftpChannel = channel.asInstanceOf[ChannelSftp]
      sftpChannel.cd(pathInputFile)
      Logger.info(s"Logged in to ftp folder")
      import scala.collection.JavaConverters._
      val listOfFiles = sftpChannel.ls("*.DAT").asScala
        .map(_.asInstanceOf[sftpChannel.LsEntry])
        .map(entry => {
          val stream = sftpChannel.get(entry.getFilename)
          Logger.info(s"File on ftp is:${entry.getFilename}")

          val br = new BufferedReader(new InputStreamReader(stream))

          val linesToParse = Stream.continually(br.readLine()).takeWhile(_ != null).toList
          val caughtExceptions = CR10XFileParser.parseAndSaveData(linesToParse, meteoService, entry.getFilename, config.stationNrHexenRubi, config.projectNrHexenRubi, config.periodeHexenRubi)
              caughtExceptions match {
                case None => {
                  EmailService.sendEmail("CR10X File Processor", "CR10X_Data_Processing@wsl.ch", emailList, emailList, "CR10X File Processing Report OK", s"file Processed Report${entry.getFilename}. \n PS: ***If there is any change in  wind direction and wind speed parameter, please contact Database Manager LWF to change in DB and Akka config.}")
                }
                case Some(_) => EmailService.sendEmail("CR10X File Processor", "CR10X_Data_Processing@wsl.ch", emailList, emailList, "CR10X File Processing Report With errors", s"file Processed Report${entry.getFilename}. \n PS: ***If there is any change in  wind direction and wind speed parameter, please contact Database Manager LWF to change in DB and Akka config.}}")
              }
              //Logger.info(s"Last timestamp when file was changed:${f.getName} time: ${lastModifiedTime}")
    })} catch { case ex =>
      Logger.info(s"smb connection problem:${ex}")
      EmailService.sendEmail("CR10X File Processor", "CR10X_Data_Processing@wsl.ch", emailList, emailList, "CR10X File Processing Report With errors", s"file Processed Report. \n PS: ***If there is any change in  wind direction and wind speed parameter, please contact Database Manager LWF to change in DB and Akka config.}}")
      Seq()
    }
    /*val sfos = new SmbFileOutputStream(sFile)
    sfos.write("Test".getBytes)*/
    Logger.info(s"Destination for archive files ${pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip"} ")
    Logger.info("processing data task finished")
  }

  private def  readFileContent( sFile: SmbFile):   Seq[String]= {
    var reader: BufferedReader  = null
    try {
      val inStream = new SmbFileInputStream(sFile)
      val inStreamReader = new InputStreamReader(inStream)
      reader = new BufferedReader(inStreamReader)
      val linesParsed: Seq[String] = Stream.continually(reader.readLine()).takeWhile(_ != null).toList
      Logger.info("lines read")
      inStream.close()
      inStreamReader.close()
      linesParsed
    } catch {
      case ex =>
      Logger.info(s"smb exception:${ex}")
        Seq()
    }
  }
}

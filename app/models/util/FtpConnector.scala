package models.util

import java.io._
import java.util
import java.util.stream

import com.jcraft.jsch.{ChannelSftp, JSch, JSchException, SftpException}
import models.domain.{CR1000ErrorFileInfo, CR1000Exceptions, DataImport, FormatMessage}
import models.services.{CR1000FileParser, CR1000FileValidator, EmailService, MeteoService}
import org.apache.commons.io.FileUtils
import play.api.Logger
import schedulers.{ConfigurationLoader, StationKonfig}

import scala.io.Source

object FtpConnector {

  @throws[Exception]
  def readFileFromFtp(userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrlMeteo: String, stationKonfigs: List[StationKonfig], emailUserList: String, meteoService: MeteoService, pathForArchiveFiles: String): Unit = {
    val jsch = new JSch
    try {

      val session = jsch.getSession(userNameFtp, ftpUrlMeteo, 22)
      session.setPassword(passwordFtp)
      session.setConfig("StrictHostKeyChecking", "no")
      session.setConfig("PreferredAuthentications",
        "publickey,keyboard-interactive,password")
      session.connect()
      val channel = session.openChannel("sftp")
      channel.connect()
      val sftpChannel = channel.asInstanceOf[ChannelSftp]
      sftpChannel.cd(pathForFtpFolder)
      Logger.info(s"Logged in to ftp folder")
      import scala.collection.JavaConverters._
      val listOfFiles = sftpChannel.ls("*.crd").asScala
        .map(_.asInstanceOf[sftpChannel.LsEntry])
        .map(entry => {
          val stream = sftpChannel.get(entry.getFilename)
          val br = new BufferedReader(new InputStreamReader(stream))
          val linesToParse = Stream.continually(br.readLine()).takeWhile(_ != null).toList
          val validLines = linesToParse.filter(l => CurrentSysDateInSimpleFormat.dateRegex.findFirstIn(l).nonEmpty)
          val errors: Seq[(Int, List[CR1000Exceptions])] = validLines.zipWithIndex.map(l => (l._2,CR1000FileValidator.validateLine(entry.getFilename,l._1,stationKonfigs)))
          CR1000ErrorFileInfo(entry.getFilename,errors, validLines)
         }
        )
        .toList
      val infoAboutFileProcessed =
        listOfFiles.toList.map(file => {
        if(file.errors.flatMap(_._2).nonEmpty) {
          val errorstring = FormatMessage.formatErrorMessage(file.errors)
          (s"File not processed: ${file.fileName} \n errors: ${errorstring} \n",Some(errorstring))
        } else {
          val caughtExceptions = CR1000FileParser.parseAndSaveData(file.linesToSave, meteoService, file.fileName)
          caughtExceptions match {
            case None =>  {
              sftpChannel.get(file.fileName, pathForArchiveFiles + file.fileName)
              sftpChannel.rm(file.fileName)
              Thread.sleep(10)
              (s"File is processed successfully: ${file.fileName}",None)
            }
            case Some(x) => (s"File is not processed successfully: ${file.fileName} reason: ${x.errorMessage}",Some(x.errorMessage))
          }
        }
      })

      val emailList = emailUserList.split(";").toSeq
      if(infoAboutFileProcessed.nonEmpty) {
        if(infoAboutFileProcessed.exists(_._2.nonEmpty)) {
          EmailService.sendEmail("CR 1000 Processor", "simpal.kumar@wsl.ch", emailList, emailList, "CR 1000 Processing Report With Errors", s"file Processed Report${infoAboutFileProcessed.map(_._1).mkString("\n")}")
        } else {
          EmailService.sendEmail("CR 1000 Processor", "simpal.kumar@wsl.ch", emailList, emailList, "CR 1000 Processing Report OK", s"file Processed Report${infoAboutFileProcessed.map(_._1).mkString("\n")}")
        }
      }
      Logger.info(s"list of files received: ${infoAboutFileProcessed.map(_._1).mkString("\n")}")

      sftpChannel.exit()
      session.disconnect()
    } catch {
      case e: JSchException =>
        e.printStackTrace()
      case e: SftpException =>
        e.printStackTrace()
    }
  }

  @throws[Exception]
  def writeFileToFtp(dataToWrite: List[String],userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrlMeteo: String, fileName: String, pathForLocalWrittenFiles: String): Unit = {
    val jsch = new JSch
    try {
      val session = jsch.getSession(userNameFtp, ftpUrlMeteo, 22)
      session.setConfig("StrictHostKeyChecking", "no")
      session.setPassword(passwordFtp)
      session.connect()
      val channel = session.openChannel("sftp")
      channel.connect()
      val sftpChannel = channel.asInstanceOf[ChannelSftp]
      sftpChannel.cd(pathForFtpFolder)
      val file = new File(fileName + ".DAT")
      Logger.info(s"Empty file before moving to ftp: ${file.getAbsolutePath}")
      val pw = new PrintWriter(file)
      pw.write(dataToWrite.mkString("\n"))
      pw.close
      val newFileStream: FileInputStream = new FileInputStream(file)
      sftpChannel.put(newFileStream, file.getName)
      sftpChannel.exit()
      session.disconnect()
      val srcFile = FileUtils.getFile(fileName + ".DAT")
      val destFile = FileUtils.getFile(pathForLocalWrittenFiles)
      newFileStream.close()
        FileUtils.moveFileToDirectory(srcFile, destFile, true)
        Logger.info(s"file is moved from source: ${srcFile.getAbsolutePath} destination:${destFile.getAbsolutePath} ")
        srcFile.delete()
    } catch {
      case e: JSchException =>
        e.printStackTrace()
      case e: SftpException =>
        e.printStackTrace()
    }
  }
}
package models.util

import java.io.{File, FileInputStream, PrintWriter}

import com.jcraft.jsch.{ChannelSftp, JSch, JSchException, SftpException}
import org.apache.commons.io.FileUtils
import play.api.Logger
import schedulers.ConfigurationLoader

object FtpConnector {

  @throws[Exception]
  def readFileFromFtp(userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrlMeteo: String): Unit = {
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
      sftpChannel.get("testDownload.txt", "temp/testDownload.txt")
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
      val file = new File(fileName + ".csv")
      Logger.info(s"Empty file before moving to ftp: ${file.getAbsolutePath}")
      val pw = new PrintWriter(file)
      pw.write(dataToWrite.mkString("\n"))
      pw.close
      val newFileStream: FileInputStream = new FileInputStream(file)
      sftpChannel.put(newFileStream, file.getName)
      sftpChannel.exit()
      session.disconnect()
      val srcFile = FileUtils.getFile(fileName + ".csv")
      val destFile = FileUtils.getFile(pathForLocalWrittenFiles)
      newFileStream.close()
      if(!destFile.exists()) {
        FileUtils.moveFileToDirectory(srcFile, destFile, false)
        Logger.info(s"file is moved from source: ${srcFile.getAbsolutePath} destination:${destFile.getAbsolutePath} ")
        srcFile.delete()
      }
    } catch {
      case e: JSchException =>
        e.printStackTrace()
      case e: SftpException =>
        e.printStackTrace()
    }
  }
}
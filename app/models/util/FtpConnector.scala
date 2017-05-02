package models.util

import java.io.{File, FileInputStream, PrintWriter}

import com.jcraft.jsch.{ChannelSftp, JSch, JSchException, SftpException}
import models.domain.MeteoDataRow


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
      sftpChannel.get("/data/ftp/pub/kumar/testDownload.txt", "C:\\Temp\\testDownload.txt")
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
  def writeFileToFtp(dataToWrite: Seq[MeteoDataRow],userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrlMeteo: String): Unit = {
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
      val file = new File("testData.txt")
      val pw = new PrintWriter(file)
      pw.write(dataToWrite.toList.mkString("\n"))
      pw.close
      sftpChannel.put(new FileInputStream(file), file.getName)
      sftpChannel.exit()
      session.disconnect()
    } catch {
      case e: JSchException =>
        e.printStackTrace()
      case e: SftpException =>
        e.printStackTrace()
    }
  }
}
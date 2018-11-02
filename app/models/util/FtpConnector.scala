package models.util

import java.io._

import com.jcraft.jsch.{ChannelSftp, JSch, JSchException, SftpException}
import models.domain.Ozone.{OzoneFileConfig, OzoneKeysConfig}
import models.domain.meteorology.ethlaegeren.parser.ETHLaeFileParser
import models.domain.meteorology.ethlaegeren.validator.ETHLaeFileValidator
import models.domain.pheno.{BesuchInfo, PhanoFileLevelInfo, PhanoPlotKeysConfig}
import models.domain.{CR1000ErrorFileInfo, CR1000Exceptions, FormatMessage}
import models.ozone.{OzoneFileLevelInfoMissingError, _}
import models.phano.{PhanoFileParser, PhanoFileValidator}
import models.services._
import org.apache.commons.io.FileUtils
import play.api.Logger
import schedulers.StationKonfig

object FtpConnector {

  @throws[Exception]
  def readCrdCR1000FileFromFtp(userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrlMeteo: String, stationKonfigs: List[StationKonfig], emailUserList: String, meteoService: MeteoService, pathForArchiveFiles: String): Unit = {
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
          val errorstring = FormatMessage.formatCR1000ErrorMessage(file.errors)
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
  def writeFileToFtp(dataToWrite: List[String],userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrlMeteo: String, fileName: String, pathForLocalWrittenFiles: String, extensionFile: String): Unit = {
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
      val file = new File(fileName + extensionFile)
      Logger.info(s"Empty file before moving to ftp: ${file.getAbsolutePath}")
      val pw = new PrintWriter(file)
      pw.write(dataToWrite.mkString("\n"))
      pw.close
      val newFileStream: FileInputStream = new FileInputStream(file)
      sftpChannel.put(newFileStream, file.getName)
      sftpChannel.exit()
      session.disconnect()
      val srcFile = FileUtils.getFile(fileName + extensionFile)
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

  @throws[Exception]
  def readOzoneCSVFileFromFtp(userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrl: String, emailUserList: String, meteoService: MeteoService, pathForArchiveFiles: String): Unit = {
    val jsch = new JSch
    try {
      val session = jsch.getSession(userNameFtp, ftpUrl, 22)
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
      val listOfErrors = sftpChannel.ls("*.csv").asScala
        .map(_.asInstanceOf[sftpChannel.LsEntry])
        .map(entry => {
          Logger.info(s"Parsing the file ${entry.getFilename}")

          val stream = sftpChannel.get(entry.getFilename)
          val br = new BufferedReader(new InputStreamReader(stream))
          val linesToParse = Stream.continually(br.readLine()).takeWhile(_ != null).toList
          val lineWithColumDefiToDecideNrOfParams = linesToParse.filter(l => OzoneKeysConfig.forNumberOfParametersInFile.exists(l.contains))
          val numberofParameters = if (lineWithColumDefiToDecideNrOfParams.nonEmpty)  15 else 12

          val validLines = linesToParse.filterNot(l => OzoneKeysConfig.defaultInvalidLinesPrefix.exists(l.contains) || l.matches("^[;]+$"))
          val validDataAndCommentsLines = validLines.filterNot(l => OzoneKeysConfig.defaultValidKeywords.exists(l.contains))
          val validDataLines = validDataAndCommentsLines.filter(l => OzoneKeysConfig.defaultPlotConfigs.map(p => p.plotName + ";").filter(l.startsWith(_)).nonEmpty || OzoneKeysConfig.defaultPlotConfigs.map(p => p.abbrePlot + ";").filter(l.startsWith(_)).nonEmpty)
          val validKommentLines = validDataAndCommentsLines.filterNot(l => OzoneKeysConfig.defaultPlotConfigs.map(p => p.plotName + ";").filter(l.startsWith(_)).nonEmpty || OzoneKeysConfig.defaultPlotConfigs.map(p => p.abbrePlot + ";").filter(l.startsWith(_)).nonEmpty)

          val suspiciousKommentLines = OzoneKeysConfig.findSuspiciousKommentLines(validKommentLines)

          val validFileHeaderLines: Seq[String] = validLines.filter(l => OzoneKeysConfig.defaultValidKeywords.exists(l.contains))
          val einfdat = CurrentSysDateInSimpleFormat.systemDateForEinfdat
          val fileLevelConfig: OzoneFileConfig = OzoneKeysConfig.prepareOzoneFileLevelInfo(validFileHeaderLines, validKommentLines, entry.getFilename)
          val errorsWhilesavingFileInfo = meteoService.insertOzoneFileInfo(fileLevelConfig, CurrentSysDateInSimpleFormat.sysdateDateInOracleformat)
          val analysId = meteoService.getAnalyseIdForFile(fileLevelConfig.fileName, einfdat)
          if (analysId > 0) {
            val errors: Seq[(Int, Either[(List[OzoneExceptions], String), String])] = validDataLines.zipWithIndex.map(l => (l._2, WSOzoneFileValidator.validateLine(entry.getFilename, l._1, numberofParameters)))
            val errorList: List[OzoneExceptions] = errors.flatMap(er =>

              er._2 match {
                case Right(x) => {
                  val caughtExceptions = WSOzoneFileParser.parseAndSaveData(x, meteoService, true, analysId, numberofParameters, "", fileLevelConfig.nachweisgrenze)
                  caughtExceptions match {
                    case None => {
                      Thread.sleep(10)
                      List()
                    }
                    case Some(x) => List(x)
                  }
                }
                case Left(y) => {
                val exceptionsMessage: String = y._1.map(ex => {
                  ex match {
                    case OzoneInvalidDateException(_,_) => "Correction Required for Dates."
                    case _ => ""
                  }
                }).mkString("")
                  val caughtExceptions = WSOzoneFileParser.parseAndSaveData(y._2, meteoService, false, analysId, numberofParameters,exceptionsMessage,fileLevelConfig.nachweisgrenze)
                  caughtExceptions match {
                    case None =>
                      y._1
                    case Some(er) => y._1.:::(List(er))
                  }
                }
              }).toList
            val missingInfoErrors: List[OzoneFileLevelInfoMissingError] = if (fileLevelConfig.missingInfo == true) List(OzoneFileLevelInfoMissingError(100, "Missing important File level parameters.")) else List()
            val missingParametersErrors: Seq[OzoneNotSufficientParameters] = if (errorList.nonEmpty) List(OzoneNotSufficientParameters(100,"Missing some values for the Data.")) else List()
            OzoneErrorFileInfo(entry.getFilename, (missingParametersErrors.toList ::: suspiciousKommentLines ::: missingInfoErrors) )
          } else {
            val missingInfoErrors: List[OzoneFileLevelInfoMissingError] = if (fileLevelConfig.missingInfo == true) List(OzoneFileLevelInfoMissingError(100, "Missing important File level parameters.")) else List()
            OzoneErrorFileInfo(entry.getFilename, List(errorsWhilesavingFileInfo.get) ::: missingInfoErrors)
          }
        }).toList

      val infoAboutFileProcessed =
        listOfErrors.map(err => {
          if(err.errors.nonEmpty) {
            val errorstring = s"File not processed: ${err.fileName} \n" + FormatMessage.formatOzoneErrorMessage(err.errors)
            sftpChannel.get(err.fileName, pathForArchiveFiles + err.fileName)
            sftpChannel.rm(err.fileName)
            (errorstring,Some(errorstring))
          }
          else {
            sftpChannel.rm(err.fileName)
            (s"File was processed successfully: ${err.fileName} \n ",None)
          }

        })

      val emailList = emailUserList.split(";").toSeq
      if(infoAboutFileProcessed.nonEmpty) {
           EmailService.sendEmail("Ozone File Processor", "simpal.kumar@wsl.ch", emailList, emailList, "Ozone File Processing Report With Errors", s"${infoAboutFileProcessed.flatMap(_._2).mkString("\n")}")
      }
      Logger.info(s"list of files received: ${infoAboutFileProcessed.map(_._2).mkString("\n")}")

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
  def readPhanoCSVFileFromFtp(userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrl: String, emailUserList: String, meteoService: MeteoService, pathForArchiveFiles: String): Unit = {
   /* val jsch = new JSch
    try {
      val session = jsch.getSession(userNameFtp, ftpUrl, 22)
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
      val listOfErrors = sftpChannel.ls("*.csv").asScala
        .map(_.asInstanceOf[sftpChannel.LsEntry])
        .map(entry => {
          Logger.info(s"Parsing the file ${entry.getFilename}")

          val stream = sftpChannel.get(entry.getFilename)
          val br = new BufferedReader(new InputStreamReader(stream))
          val linesToParse = Stream.continually(br.readLine()).takeWhile(_ != null).toList
          val numberofParameters = 14
          val invnr = 15
          val typeCode = 2

          val validLines = linesToParse.filterNot(l => PhanoPlotKeysConfig.defaultInvalidLinesPrefix.exists(l.contains) || l.matches("^[;]+$"))
          val validDataAndCommentsLines = validLines.filterNot(l => PhanoPlotKeysConfig.defaultValidKeywords.exists(l.contains))
          val validDataLines = validDataAndCommentsLines.filter(l => Character.isDigit(l.charAt(0)))

          val validFileHeaderLines: Seq[String] = validLines.filter(l => PhanoPlotKeysConfig.defaultValidKeywords.exists(l.contains))
          val einfdat = CurrentSysDateInSimpleFormat.systemDateForEinfdat
          val fileLevelConfig: PhanoFileLevelInfo = PhanoPlotKeysConfig.preparePhanoFileLevelInfo(validFileHeaderLines, entry.getFilename)
          val personNr = meteoService.getPhanoPersonId(fileLevelConfig.beobachterName)
          val stationNr = meteoService.getPhanoStationId(fileLevelConfig.stationName)
          val besuchDatumInfos = fileLevelConfig.besuchDatums.map(BesuchInfo(stationNr, invnr, personNr, _))
          if (stationNr > 0) {
          val errorsWhilesavingFileInfo = meteoService.insertPhanoPlotBesuchDatums(besuchDatumInfos, CurrentSysDateInSimpleFormat.sysdateDateInOracleformat)

            validDataLines.map(line => {
            PhanoFileParser.parseAndSaveData(line, meteoService, true, stationNr, personNr, invnr, typeCode)


            val missingInfoErrors: List[OzoneFileLevelInfoMissingError] = if (fileLevelConfig.missingInfo == true) List(OzoneFileLevelInfoMissingError(100, "Missing important File level parameters.")) else List()
            val missingParametersErrors: Seq[OzoneNotSufficientParameters] = if (errorList.nonEmpty) List(OzoneNotSufficientParameters(100,"Missing some values for the Data.")) else List()
            OzoneErrorFileInfo(entry.getFilename, (missingParametersErrors.toList ::: suspiciousKommentLines ::: missingInfoErrors) )
          }
          }else {
            val missingInfoErrors: List[OzoneFileLevelInfoMissingError] = if (fileLevelConfig.missingInfo == true) List(OzoneFileLevelInfoMissingError(100, "Missing important File level parameters.")) else List()
            OzoneErrorFileInfo(entry.getFilename, List(errorsWhilesavingFileInfo.get) ::: missingInfoErrors)
          }
        }).toList

      val infoAboutFileProcessed =
        listOfErrors.map(err => {
          if(err.errors.nonEmpty) {
            val errorstring = s"File not processed: ${err.fileName} \n" + FormatMessage.formatOzoneErrorMessage(err.errors)
            sftpChannel.get(err.fileName, pathForArchiveFiles + err.fileName)
            sftpChannel.rm(err.fileName)
            (errorstring,Some(errorstring))
          }
          else {
            sftpChannel.rm(err.fileName)
            (s"File was processed successfully: ${err.fileName} \n ",None)
          }

        })

      val emailList = emailUserList.split(";").toSeq
      if(infoAboutFileProcessed.nonEmpty) {
        EmailService.sendEmail("Ozone File Processor", "simpal.kumar@wsl.ch", emailList, emailList, "Ozone File Processing Report With Errors", s"${infoAboutFileProcessed.flatMap(_._2).mkString("\n")}")
      }
      Logger.info(s"list of files received: ${infoAboutFileProcessed.map(_._2).mkString("\n")}")

      sftpChannel.exit()
      session.disconnect()
    } catch {
      case e: JSchException =>
        e.printStackTrace()
      case e: SftpException =>
        e.printStackTrace()
    }*/
  }


  @throws[Exception]
  def readETHLaeFileFromFtp(userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrlMeteo: String, stationKonfigs: List[StationKonfig], emailUserList: String, meteoService: MeteorologyService, pathForArchiveFiles: String): Unit = {
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
      val listOfFiles = sftpChannel.ls("*.lwf").asScala
        .map(_.asInstanceOf[sftpChannel.LsEntry])
        .map(entry => {
          val stream = sftpChannel.get(entry.getFilename)
          val br = new BufferedReader(new InputStreamReader(stream))
          val linesToParse = Stream.continually(br.readLine()).takeWhile(_ != null).toList
          val validLines = linesToParse.filter(l => CurrentSysDateInSimpleFormat.dateRegex.findFirstIn(l).nonEmpty)
          val errors: Seq[(Int, List[CR1000Exceptions])] = validLines.zipWithIndex.map(l => (l._2,ETHLaeFileValidator.validateLine(entry.getFilename,l._1,stationKonfigs)))
          CR1000ErrorFileInfo(entry.getFilename,errors, validLines)
        }
        )
        .toList
      val infoAboutFileProcessed =
        listOfFiles.toList.map(file => {
          if(file.errors.flatMap(_._2).nonEmpty) {
            val errorstring = FormatMessage.formatCR1000ErrorMessage(file.errors)
            (s"File not processed: ${file.fileName} \n errors: ${errorstring} \n",Some(errorstring))
          } else {
            val projNrForFile: Option[Int] = stationKonfigs.find(sk => file.fileName.startsWith(sk.fileName)).flatMap(_.projs.headOption.map(_.projNr))
            val caughtExceptions = ETHLaeFileParser.parseAndSaveData(file.linesToSave, meteoService, file.fileName, projNrForFile)
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


}
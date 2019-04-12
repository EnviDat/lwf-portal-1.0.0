package models.util

import java.io._

import com.jcraft.jsch.{ChannelSftp, JSch, JSchException, SftpException}
import models.domain.Ozone.{OzoneFileConfig, OzoneKeysConfig}
import models.domain._
import models.domain.meteorology.ethlaegeren.parser.ETHLaeFileParser
import models.domain.pheno.{BesuchInfo, PhanoErrorFileInfo, PhanoFileLevelInfo, PhanoPlotKeysConfig}
import models.ozone.{OzoneFileLevelInfoMissingError, _}
import models.phano.PhanoFileParser
import models.services._
import models.util.StringToDate.formatCR1000Date
import org.apache.commons.io.FileUtils
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import schedulers.{ETHLaegerenLoggerFileConfig, StationKonfig}

import scala.collection.immutable
import scala.collection.parallel.ParMap

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
          Logger.info(s"File on ftp is:${entry.getFilename}")

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
          val nrOfErrorMessages = file.errors.size
          val errorstring = if(nrOfErrorMessages < 5) FormatMessage.formatCR1000ErrorMessage(file.errors) else FormatMessage.formatCR1000ErrorMessage(file.errors.take(5))
          (s"File not processed: ${file.fileName} \n errors: ${errorstring} \n",Some(errorstring), nrOfErrorMessages)
        } else {
          val caughtExceptions = CR1000FileParser.parseAndSaveData(file.linesToSave, meteoService, file.fileName)
          caughtExceptions match {
            case None =>  {
             val phoneixExists = new java.io.File(pathForArchiveFiles).exists
              if(phoneixExists) {
                sftpChannel.get(file.fileName, pathForArchiveFiles + file.fileName)
                sftpChannel.rm(file.fileName)
              }
              Thread.sleep(10)
              (s"File is processed successfully: ${file.fileName}",None, 0)
            }
            case Some(x) => (s"File is not processed successfully: ${file.fileName} reason: ${x.errorMessage}",Some(x.errorMessage),1)
          }
        }
      })

      val emailList = emailUserList.split(";").toSeq
      if(infoAboutFileProcessed.nonEmpty) {
        if(infoAboutFileProcessed.exists(_._2.nonEmpty)) {
          if(infoAboutFileProcessed.filter(errorsList => errorsList._3 > 10).nonEmpty || infoAboutFileProcessed.filter(errorMessage => errorMessage._1.size > 10000).nonEmpty) {
            Logger.info(s"CR 1000 Processor, CR1000_Data_Processing@klaros.wsl.ch ${emailList}, CR 1000 Processing Report With Errors file Processed Report${infoAboutFileProcessed.map(_._1).mkString("\n")}")
            EmailService.sendEmail("CR 1000 Processor", "CR1000_Data_Processing@klaros.wsl.ch", emailList, emailList, "CR 1000 Processing Alarm Report With Errors ", s"Big Files/large number of files with errors were detected on ftp. Please check it if its expected.\n File Processed Report${infoAboutFileProcessed.map(_._1).mkString("\n")}")
          } else {
            Logger.info(s"CR 1000 Processor, CR1000_Data_Processing@klaros.wsl.ch ${emailList}, CR 1000 Processing Report With Errors file Processed Report${infoAboutFileProcessed.map(_._1).mkString("\n")}")
            EmailService.sendEmail("CR 1000 Processor", "CR1000_Data_Processing@klaros.wsl.ch", emailList, emailList, "CR 1000 Processing Report Errors", s"Files Processed Report${infoAboutFileProcessed.map(_._1).mkString("\n")}")
          }
        } else {
          Logger.info(s"CR 1000 Processor, CR1000_Data_Processing@klaros.wsl.ch ${emailList}, CR 1000 Processing Report OK, file Processed Report${infoAboutFileProcessed.map(_._1).mkString("\n")}")
          EmailService.sendEmail("CR 1000 Processor", "CR1000_Data_Processing@klaros.wsl.ch", emailList, emailList, "CR 1000 Processing Report OK", s"File Processed Report${infoAboutFileProcessed.map(_._1).mkString("\n")}")
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
  def writeFileToFtp(dataToWrite: List[String],userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrlMeteo: String, fileName: String, pathForLocalWrittenFiles: String, extensionFile: String, pathForTempFiles: String): Unit = {
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
      val file = new File(pathForTempFiles + fileName + extensionFile)
      Logger.info(s"Empty file before moving to ftp: ${file.getAbsolutePath}")
      val pw = new PrintWriter(file)
      dataToWrite.map(pw.println(_))
      //pw.write(dataToWrite.mkString("\n"))
      pw.close
      val newFileStream: FileInputStream = new FileInputStream(file)
      sftpChannel.put(newFileStream, file.getName)
      sftpChannel.exit()
      session.disconnect()
      val srcFile = FileUtils.getFile(pathForTempFiles + fileName + extensionFile)
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
  def readPhanoCSVFileFromFtp(userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrl: String, emailUserList: String, meteoService: PhanoService, pathForArchiveFiles: String) = {
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
        .map(f = entry => {
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
          val besuchDatumInfos = fileLevelConfig.besuchDatums.map(BesuchInfo(stationNr, invnr, personNr, _, fileLevelConfig.comments))
          val speciesId = meteoService.getSpeciesId(fileLevelConfig.speciesName)

          val listOfAllCaughtExceptions = if (stationNr > 0) {
            val errorsWhilesavingFileInfo = meteoService.insertBesuchInfo(besuchDatumInfos).flatten.toList

            val otherExceptionsCaught = validDataLines.flatMap(line => {
              val exceptionsCaught: List[CR1000OracleError] = PhanoFileParser.parseAndSaveData(line, meteoService, true, stationNr, personNr, invnr, typeCode, speciesId).toList
              val missingInfoErrors: List[CR1000OracleError] = if (fileLevelConfig.missingInfo) List(CR1000OracleError(100, "Missing important File level parameters.")) else List()
              exceptionsCaught.:::(missingInfoErrors)
            })
            PhanoErrorFileInfo(entry.getFilename, otherExceptionsCaught.:::(errorsWhilesavingFileInfo))
          } else {
            val missingInfoErrors: List[CR1000FileError] = if (fileLevelConfig.missingInfo) List(CR1000FileError(100, "Missing important File level parameters.")) else List()
            PhanoErrorFileInfo(entry.getFilename, missingInfoErrors)
          }
          listOfAllCaughtExceptions
        }).toList

      val infoAboutFileProcessed =
        listOfErrors.map(err => {
          if(listOfErrors.nonEmpty) {
            val errorstring = s"File not processed"
            /*val errorstring = s"File not processed: ${err.fileName} \n" + FormatMessage.formatOzoneErrorMessage(err.errors)
            sftpChannel.get(err.fileName, pathForArchiveFiles + err.fileName)
            sftpChannel.rm(err.fileName)*/
            (errorstring,Some(errorstring))
          }
          else {
            //sftpChannel.rm(err.fileName)
            (s"File was processed successfully")
          }

        })

      val emailList = emailUserList.split(";").toSeq
      if(infoAboutFileProcessed.nonEmpty) {
        EmailService.sendEmail("Ozone File Processor", "simpal.kumar@wsl.ch", emailList, emailList, "Ozone File Processing Report With Errors", s"")
      }
      Logger.info(s"list of files received:")

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
  def readETHLaeFileFromFtp(config: ETHLaegerenLoggerFileConfig, meteoService: MeteoService, pathForArchiveFiles: String): Unit = {
    val userNameFtp = config.fptUserNameETHLae
    val passwordFtp = config.ftpPasswordETHLae
    val pathForFtpFolder = config.ftpPathForIncomingFileETHLae
    val ftpUrlMeteo = config.ftpUrlETHLae
    val stationKonfigs = config.stationConfigs
    val emailUserList = config.emailUserList
    val headerT1_47File = config.ethHeaderLineT1_47
    val headerPrefixT1_47 = config.ethHeaderPrefixT1_47
    val specialParamKonfig = config.specialStationKonfNrsETHLae
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
      import org.joda.time.Days

      import scala.collection.JavaConverters._
      val allLinesCollectedFromFiles = sftpChannel.ls("*.lwf").asScala
        .map(_.asInstanceOf[sftpChannel.LsEntry])
        .flatMap(entry => {
          val fileName =  entry.getFilename
          val mapStationKonfig: Option[StationKonfig] = stationKonfigs.find(sk => fileName.startsWith(sk.fileName))
            mapStationKonfig.map(statKonf => {
              val stream = sftpChannel.get(fileName)
              val fileAttributes = entry.getAttrs
              val lastModified = fileAttributes.getMtimeString
              val lastModifiedDate = new org.joda.time.DateTime(StringToDate.formatFromDateToStringDefaultJava.parse(lastModified))
              val diffInSysdate = Days.daysBetween(lastModifiedDate,new org.joda.time.DateTime()).getDays
              val validLinesToBeparsed =  if(diffInSysdate <= 31) {
                  val br = new BufferedReader(new InputStreamReader(stream))
                  val linesToParse = Stream.continually(br.readLine()).takeWhile(_ != null).toList
                  val headerLine = linesToParse.filter(l => l.contains(headerPrefixT1_47))
                  val validHeaderLine = headerLine.find(l => l.replaceAll("\"", "").contains(headerT1_47File.replaceAll("\"", "")))
                 if(validHeaderLine.isEmpty)
                   EmailService.sendEmail("Lageren File Header doesn't match", "laegeren_no_reply@wsl.ch", emailUserList.split(";").toList, emailUserList.split(";").toList, "Laegeren File Processing Report With Errors", s"${headerT1_47File} doesn't match in file.")
                validHeaderLine.map(vLine => {
                  linesToParse.filter(l => CurrentSysDateInSimpleFormat.dateRegex.findFirstIn(l).nonEmpty)
                  }).getOrElse(List())
              } else List()
              (statKonf,validLinesToBeparsed)
          })
        })

      val groupByConfig = allLinesCollectedFromFiles.groupBy(_._1)
      val groupedValidLines = groupByConfig.map(l => (l._1,groupValidLinesForTimeStampsWithTenMinutes(l._2.flatMap(_._2).toList)))
      val fileName = "MergedLaegerenDataFile_" + CurrentSysDateInSimpleFormat.dateNow
     val errors: immutable.Iterable[CR1000OracleError] =  groupedValidLines.flatMap(dataForStatKonf => {
       dataForStatKonf._2.map(
       dataForTimeStamp => {
          ETHLaeFileParser.parseAndSaveData(dataForTimeStamp._1._1._2, dataForTimeStamp._1._2, dataForTimeStamp._2, meteoService, fileName, dataForStatKonf._1, specialParamKonfig)
        })}).flatten
      errors.nonEmpty match {
        case false => {
          EmailService.sendEmail("Lägeren ETH-EMPA Tower File Processor", "LWF_Data_Processing@klaros.wsl.ch", emailUserList.split(";"), emailUserList.split(";"), "Läegeren ETH-EMPA File Processing Report OK", s"file Processed Successfully. \n PS: ***If there is any change in  wind direction and wind speed parameter, please contact Database Manager LWF to change in DB and Akka config.}")
        }
        case true =>
          EmailService.sendEmail("Lägeren ETH-EMPA Tower File Processor", "LWF_Data_Processing@klaros.wsl.ch", emailUserList.split(";"), emailUserList.split(";"), "Läegeren ETH-EMPA File Processing Report With errors", s"file Processed Report${errors.map(_.errorMessage).mkString(",")}. \n PS: ***If there is any change in  wind direction and wind speed parameter, please contact Database Manager LWF to change in DB and Akka config.}}")
      }
      sftpChannel.exit()
      session.disconnect()
    } catch {
      case e: JSchException =>
        e.printStackTrace()
      case e: SftpException =>
        e.printStackTrace()
    }
  }


  private def groupValidLinesForTimeStampsWithTenMinutes(validLines: List[String]) = {
    import Joda._
    val allDatesInFiles = validLines.map(l => (formatCR1000Date.withZone(DateTimeZone.UTC).parseDateTime(l.split(",")(0).replace("\"", "")),l)).sortBy(_._1)
    val minDateInFile = allDatesInFiles.map(_._1).min
    val maxDateInFile = allDatesInFiles.map(_._1).max
    val allDays = Iterator.iterate(minDateInFile.withTimeAtStartOfDay()) {
      _.plusMinutes(10)
    }.takeWhile(_.isBefore(maxDateInFile)).toList
    val allDaysTimeStamps = allDays.map(d => (d.minusMinutes(10), d)).sorted
    /*
    val allLinesGrouped: Map[((DateTime, DateTime), Int), List[String]] = allDaysTimeStamps.map(dt => {
      val linesBetweenTimePeriod = validLines.filter(l => {
        val dateTimeStampInLine = formatCR1000Date.withZone(DateTimeZone.UTC).parseDateTime(l.split(",")(0).replace("\"", ""))
        dateTimeStampInLine.isAfter(dt._1) && (dateTimeStampInLine.isBefore(dt._2) || dateTimeStampInLine.isEqual(dt._2))
      })
      ((dt, linesBetweenTimePeriod.length), linesBetweenTimePeriod)
    }).toMap
    */
    val allLinesGrouped: ParMap[((DateTime, DateTime), Int), List[String]] = allDaysTimeStamps.par.map(dt => {
      val linesBetweenTimePeriod = allDatesInFiles.filter(l => {
        val dateTimeStampInLine = l._1
        dateTimeStampInLine.isAfter(dt._1) && (dateTimeStampInLine.isBefore(dt._2) || dateTimeStampInLine.isEqual(dt._2))
      }).map(_._2)
      Logger.info(s"grouping is done for time stamp ${dt._1.toString() + dt._2.toString()}")
      ((dt, linesBetweenTimePeriod.length), linesBetweenTimePeriod)
    }).toMap
    Logger.info("grouping task finished")

    allLinesGrouped

      //.getOrElse(Map.empty[((DateTime, DateTime), Int), List[String]])
  }
}
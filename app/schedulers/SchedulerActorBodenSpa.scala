package schedulers

import java.io._
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import jcifs.smb._
import models.domain.BodenDataRow
import models.services._
import models.util.StringToDate.formatOzoneDate
import models.util.{CurrentSysDateInSimpleFormat, NumberParser, StringToDate}
import org.joda.time.DateTimeZone
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorBodenSpa @Inject()(configuration: Configuration, meteoService: MeteorologyService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processFile" =>  {
      val config = ConfigurationLoader.loadBodenSpaConfiguration(configuration)
      //processFile(config)
      //readFile(config)
    }
  }

  def readFile(config: ConfigurationHexenrubiData): Unit ={
    val pathInputFile = config.pathForIncomingFileHexenRubi
    val userNameFtp = config.userNameHexenRubi
    val passwordFtp = config.passwordHexenRubi
    val pathForFtpFolder = config.pathForArchivedFiles
//    FtpConnector.readFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo)
     //DatFileReader.readFilesFromFile(pathInputFile)
    Logger.info(pathInputFile)
  }

  def processFile(config: ConfigurationBodenSpaData): Unit ={
    val pathInputFile = config.pathForIncomingFileBodenSpa

    val pathForArchivedFiles = config.pathForArchivedFilesBodenSpa
    Logger.info("looking into network drive task running")

    val inputFiles = new File(pathInputFile).listFiles().filter(_.getName.startsWith(config.dataFileNameBodenSpa))
    inputFiles.map(inFile => {
      val br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile)))
      val linesToParse = Stream.continually(br.readLine()).takeWhile(_ != null).toList.filterNot(l => l.contains("PROFILNR") || l.startsWith(";;"))
    val linesToSave = linesToParse.flatMap(lineToStore => {
        val dataColumns = lineToStore.split(";")
        val profileId = NumberParser.parseNumber(dataColumns(1))
        val profilKonfId = NumberParser.parseNumber(dataColumns(2))
        val measurementDate = getActualOrDummyDate(dataColumns(3))
        val measurementValue = NumberParser.parseBigDecimal(dataColumns(4))
        val validity = 1
        val valVersion = 1
        for {
          profil <- profileId
          konfId <- profilKonfId
         messValue <- measurementValue
          bodenDataRow = BodenDataRow(profil,konfId,measurementDate,messValue,validity,valVersion)
        } yield bodenDataRow
      }
      )
      val exceptions = meteoService.insertSoilBodenSpaData(linesToSave)
      exceptions
    })

    Logger.info(s"Destination for archive files ${pathForArchivedFiles + "generatedArchivedFiles" + CurrentSysDateInSimpleFormat.dateNow + ".zip"} ")
    Logger.info("processing data task finished")
  }

  def getActualOrDummyDate(dateInString: String) = {
    s"to_date('${StringToDate.oracleDateFormat.print(formatOzoneDate.withZone(DateTimeZone.UTC).parseDateTime(dateInString))}', 'DD.MM.YYYY HH24:MI:SS')"
  }
}

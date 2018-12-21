package schedulers

import java.io.File
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.{FileGeneratorUniBaselLoggerFormat, MeteoService, OttPluvioService}
import models.util.{CurrentSysDateInSimpleFormat, DirectoryCompressor, FtpConnector}
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorOttPluvio @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "sendMeasurementEmail" =>  {
      val config = ConfigurationLoader.loadOttPluvioConfiguration(configuration)
      sendMeasurementEmail(config)
      //readFile(config)
    }
  }

  def sendMeasurementEmail(config: ConfigurationOttPluvioData): Unit ={
    Logger.info("OttPluvio job started")
    val stationNr = config.stationNrOttPluvio
    val messart = config.messartOttPluvio
    val emailList = config.emailUserListOttPluvio
    val ottpluvioService = new OttPluvioService(config, meteoService)
    ottpluvioService.getAndSendDataForLastOneDay()
    //FtpConnector.readFileFromFtp(userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo)
     //DatFileReader.readFilesFromFile(pathInputFile)
    Logger.info("OttPluvio job finished")
  }


}

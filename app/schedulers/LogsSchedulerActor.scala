package schedulers

import java.io.File
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.{FileGeneratorFromDB, MeteoService}
import models.util.{CurrentSysDateInSimpleFormat, DirectoryCompressor, FtpConnector}
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class LogsSchedulerActor @Inject()(configuration: Configuration)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "moveArchiveLog" =>  {
      val config = ConfigurationLoader.loadConfiguration(configuration)
      moveLogFile(config)
      //readFile(config)
    }
  }


  def moveLogFile(config: ConfigurationMeteoSchweizData): Unit ={

    val source = new File(config.pathForLogFiles)
    val destination = new File(config.pathForArchivedLogFiles + CurrentSysDateInSimpleFormat.dateNow + ".zip")
    destination.setReadable(true, false)
    destination.setExecutable(true, false)
    destination.setWritable(true, false)
    if(!destination.exists()) {
      FileUtils.moveFileToDirectory(source, destination, true)
      source.delete()
    }
    //ZipUtil.packEntry(new File("\\csvFiles\\"), new File("\\csvFiles" + ".zip"))
    Logger.info("moving log file task finished")
  }
}

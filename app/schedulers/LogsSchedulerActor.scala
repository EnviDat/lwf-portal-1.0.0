package schedulers

import java.io.File
import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.util.CurrentSysDateInSimpleFormat
import org.apache.commons.io.FileUtils
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class LogsSchedulerActor @Inject()(configuration: Configuration)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "moveArchiveLog" =>  {
      val config = ConfigurationLoader.loadConfiguration(configuration)
      //moveLogFile(config)
      //readFile(config)
    }
  }


  def moveLogFile(config: ConfigurationMeteoSchweizData): Unit ={

    val source = new File(config.pathForLogFiles)
    val destination = new File(config.pathForArchivedLogFiles + CurrentSysDateInSimpleFormat.dateNow)
    destination.setReadable(true, false)
    destination.setExecutable(true, false)
    destination.setWritable(true, false)

    if(source.exists()) {
      Logger.info(s"Source File Exist: ${source.getAbsolutePath}")
      if (source.getName.endsWith(".gz") || source.getName.endsWith(".DAT")) {
        val diff = new java.util.Date().getTime - source.lastModified
        Logger.info(s"Time difference from now and File modified : ${source.getAbsolutePath}")
        if (diff > 1 * 24 * 60 * 60 * 1000) {
          if (!destination.exists())
            Logger.info(s"Moving the file from source: ${source.getAbsolutePath} to destination: ${destination.getAbsolutePath}")
          FileUtils.moveFileToDirectory(source, destination, false)
          source.delete()
        }
      }
    }
    //ZipUtil.packEntry(new File("\\csvFiles\\"), new File("\\csvFiles" + ".zip"))
    Logger.info("moving log file task finished")
  }
}

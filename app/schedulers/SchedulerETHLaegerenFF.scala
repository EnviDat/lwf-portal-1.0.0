package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerETHLaegerenFF @Inject()(val system: ActorSystem, @Named("scheduler-actor-ethlae-ff") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadETHLaeFFConfiguration(configuration)
  val frequency = config.frequencyETHLae
  var actor = system.scheduler.schedule(
    0.microseconds, frequency.seconds, schedulerActor, "processEthLaeFFFile")

}


import javax.inject.{Inject, Singleton}

import akka.actor.Actor
import models.services.{MeteoService, MeteorologyService}
import models.util.FtpConnector
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext

@Singleton
class SchedulerActorETHFFLaegeren @Inject()(configuration: Configuration, meteoService: MeteoService)(implicit ec: ExecutionContext) extends Actor {
  override def receive: Receive = {
    case "processEthLaeFFFile" =>  {

      val config = ConfigurationLoader.loadETHLaeFFConfiguration(configuration)
      processFile(config)
      //readFile(config)
    }
  }

  def processFile(config: ETHLaegerenLoggerFileConfig): Unit ={
    val pathForArchiveFiles = config.ftpPathForETHLaeArchiveFiles
    Logger.info("processing data task running")
    FtpConnector.readETHLaeFFFileFromFtp(config, meteoService, pathForArchiveFiles)
    Logger.info("File processing task finished")
  }
}

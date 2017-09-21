package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class LogsScheduler @Inject()(val system: ActorSystem, @Named("log-scheduler-actor") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadMeteoSchweizConfiguration(configuration)
  var actor = system.scheduler.schedule(
    0.microseconds, 1.day, schedulerActor, "moveArchiveLog")
}

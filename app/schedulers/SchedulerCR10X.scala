package schedulers

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerCR10X @Inject()(val system: ActorSystem, @Named("scheduler-actor-cr10x") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadCR10XConfiguration(configuration)
  val frequency = config.frequency
  var actor = system.scheduler.schedule(
    0.microseconds, frequency.seconds, schedulerActor, "processFile")
}

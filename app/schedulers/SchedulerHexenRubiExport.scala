package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerHexenRubiExport @Inject()(val system: ActorSystem, @Named("scheduler-actor-hexenrubi-export") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadMeteoSchweizConfiguration(configuration)
  val frequency = config.frequency
  var actor = system.scheduler.schedule(
    0.microseconds, 3600.seconds, schedulerActor, "writeFile")

}

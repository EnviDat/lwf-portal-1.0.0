package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerHexenRubi @Inject()(val system: ActorSystem, @Named("scheduler-actor-hexenrubi") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadHexenRubiConfiguration(configuration)
  val frequency = config.frequency
  var actor = system.scheduler.schedule(
    0.microseconds, frequency.seconds, schedulerActor, "processFile")

}

package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerPhano @Inject()(val system: ActorSystem, @Named("scheduler-actor-phano") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadPhanoConfiguration(configuration)
  val frequency = config.frequencyPhano
  var actor = system.scheduler.schedule(
    0.microseconds, frequency.seconds, schedulerActor, "processPhanoFile")

}


package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerOzone @Inject()(val system: ActorSystem, @Named("scheduler-actor-ozone") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadOzoneConfiguration(configuration)
  val frequency = config.frequencyOzone
  var actor = system.scheduler.schedule(
    0.microseconds, frequency.seconds, schedulerActor, "processOzoneFile")

}


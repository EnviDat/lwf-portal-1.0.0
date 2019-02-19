package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerBodenSpa @Inject()(val system: ActorSystem, @Named("scheduler-actor-bodenspa") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadBodenSpaConfiguration(configuration)
  val frequency = config.frequencyBodenSpa
  var actor = system.scheduler.schedule(
    0.microseconds, frequency.seconds, schedulerActor, "processFile")

}


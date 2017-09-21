package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerCR1000 @Inject()(val system: ActorSystem, @Named("scheduler-actor-cr1000") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadCR1000Configuration(configuration)
  val frequency = config.frequencyCR1000
  var actor = system.scheduler.schedule(
    0.microseconds, frequency.seconds, schedulerActor, "processFile")

}

package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerGP2Logger @Inject()(val system: ActorSystem, @Named("scheduler-actor-gp2logger") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadGP2LoggerConfiguration(configuration)
  val frequency = config.frequencyCR1000
  var actor = system.scheduler.schedule(
    1000.microseconds, frequency.seconds, schedulerActor, "processGP2LoggerFile")
}

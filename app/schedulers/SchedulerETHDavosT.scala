package schedulers

import akka.actor.{ActorRef, ActorSystem}
import javax.inject.{Inject, Named}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerETHDavosT @Inject()(val system: ActorSystem, @Named("scheduler-actor-ethdav-t") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadETHDavTConfiguration(configuration)
  val frequency = config.frequencyETHDav
  var actor = system.scheduler.schedule(
    1500.seconds, frequency.seconds, schedulerActor, "processEthDavosTFile")

}

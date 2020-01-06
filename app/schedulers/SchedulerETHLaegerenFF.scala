package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerETHLaegerenFF @Inject()(val system: ActorSystem, @Named("scheduler-actor-ethlae-ff") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadETHLaeFFConfiguration(configuration)
  val frequency = config.frequencyETHLae
  var actor = system.scheduler.schedule(
    2000.seconds, frequency.seconds, schedulerActor, "processEthLaeFFFile")

}

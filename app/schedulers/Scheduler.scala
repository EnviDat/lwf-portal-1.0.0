package schedulers

import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.{Configuration, Logger}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class Scheduler @Inject() (val system: ActorSystem, @Named("scheduler-actor") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val frequency = configuration.getInt("frequency").get
  var actor = system.scheduler.schedule(
    0.microseconds, frequency.seconds, schedulerActor, "writeFile")

}

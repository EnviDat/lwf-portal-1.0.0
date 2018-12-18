package schedulers

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}
import javax.inject.{Inject, Named}

import akka.actor.{ActorRef, ActorSystem}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerOttPluvio @Inject()(val system: ActorSystem, @Named("scheduler-actor-ottpluvio") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadOttPluvioConfiguration(configuration)
  val frequency = config.frequencyOttPluvio
  var actor = system.scheduler.schedule(
    calculateInitialDelay().milliseconds, frequency.seconds, schedulerActor, "sendMeasurementEmail")

  def calculateInitialDelay(): Long = {
    val now = new Date()
    val sdf = new SimpleDateFormat("HH:mm:ss")
    sdf.setTimeZone(TimeZone.getTimeZone("CET"))
    val time1 = sdf.format(now)
    val time2 = config.startTimeForOttPulvio
    val format = new SimpleDateFormat("HH:mm:ss")
    val date1 = format.parse(time1)
    val date2 = format.parse(time2)
    val timeDifference = date2.getTime() - date1.getTime()
    val calculatedTime = if (timeDifference < 0) ((frequency * 1000) + timeDifference) else timeDifference
    calculatedTime
  }
}


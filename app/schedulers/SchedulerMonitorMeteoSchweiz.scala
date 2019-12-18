package schedulers

import java.text.SimpleDateFormat
import java.util.{Date, TimeZone}

import akka.actor.{ActorRef, ActorSystem}
import javax.inject.{Inject, Named}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerMonitorMeteoSchweiz @Inject()(val system: ActorSystem, @Named("scheduler-actor-monitorMETSCH") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadMeteoSchweizMonitorConfiguration(configuration)
  val frequency = config.frequencyMeteoSchweizMonitoring
  var actor = system.scheduler.schedule(30.seconds, frequency.seconds, schedulerActor, "sendMonitorEmail")
    //calculateInitialDelay().milliseconds, frequency.seconds, schedulerActor, "sendMonitorEmail")

  def calculateInitialDelay(): Long = {
    val now = new Date()
    val sdf = new SimpleDateFormat("HH:mm:ss")
    sdf.setTimeZone(TimeZone.getTimeZone("CET"))
    val time1 = sdf.format(now)
    val time2 = config.startTimeForMeteoSchweizMonitoring
    val format = new SimpleDateFormat("HH:mm:ss")
    val date1 = format.parse(time1)
    val date2 = format.parse(time2)
    val timeDifference = date2.getTime() - date1.getTime()
    val calculatedTime = if (timeDifference < 0) ((frequency * 1000) + timeDifference) else timeDifference
    calculatedTime
  }
}


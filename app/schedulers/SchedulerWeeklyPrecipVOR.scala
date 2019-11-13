package schedulers

import java.text.SimpleDateFormat
import java.time.{DayOfWeek, LocalDate, LocalDateTime}
import java.time.temporal.TemporalAdjusters
import java.util.{Date, TimeZone}

import akka.actor.{ActorRef, ActorSystem}
import javax.inject.{Inject, Named}
import play.api.Configuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class SchedulerWeeklyPrecipVOR @Inject()(val system: ActorSystem, @Named("scheduler-actor-weekly-precip") val schedulerActor: ActorRef, configuration: Configuration)(implicit ec: ExecutionContext) {
  val config = ConfigurationLoader.loadPreciVordemwaldConfiguration(configuration)
  val nextMonday = LocalDate.now() `with` TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY)
  val nextMondayMillis = nextMonday.toEpochDay * 24 * 60 * 60 * 1000
  val frequency = config.frequencyPreciVordemwald
  var actor = system.scheduler.schedule(
    (calculateInitialDelay + (nextMondayMillis - System.currentTimeMillis())).milliseconds,  7.days, schedulerActor, "sendWeeklyMeasurementEmail")

  def calculateInitialDelay(): Long = {
    val now = new Date()
    val sdf = new SimpleDateFormat("HH:mm:ss")
    sdf.setTimeZone(TimeZone.getTimeZone("CET"))
    val time1 = sdf.format(now)
    val time2 = config.startTimeForPreciVordemwald
    val format = new SimpleDateFormat("HH:mm:ss")
    val date1 = format.parse(time1)
    val date2 = format.parse(time2)
    val timeDifference = date2.getTime() - date1.getTime()
    val calculatedTime = if (timeDifference < 0) ((frequency * 1000) + timeDifference) else timeDifference
    calculatedTime
  }
}


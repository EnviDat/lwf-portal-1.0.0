import com.google.inject.AbstractModule
import java.time.Clock

import play.api.libs.concurrent.AkkaGuiceSupport
import schedulers._
import services.{ApplicationTimer, AtomicCounter, Counter}

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule with AkkaGuiceSupport {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
     bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
     // Ask Guice to create an instance of ApplicationTimer when the
     // application starts.
     bind(classOf[ApplicationTimer]).asEagerSingleton()
     // Set AtomicCounter as the implementation for Counter.
     bind(classOf[Counter]).to(classOf[AtomicCounter])
     bindActor[LogsSchedulerActor]("log-scheduler-actor")


     bind(classOf[SchedulerMeteoSchweizExport]).asEagerSingleton()
     bindActor[SchedulerActorMeteoSchweiz]("scheduler-actor")

     bindActor[SchedulerActorCR1000]("scheduler-actor-cr1000")
     bind(classOf[SchedulerCR1000]).asEagerSingleton()

     //Below schedulers were for GP2 Logger import which is temporarily switched off
     //bindActor[SchedulerActorGP2Logger]("scheduler-actor-gp2logger")
     //bind(classOf[SchedulerGP2Logger]).asEagerSingleton()
     //bindActor[SchedulerActorUniBasel]("scheduler-actor-unibasel")
     //bind(classOf[SchedulerUniBasel]).asEagerSingleton()


     //Below all processes are in process to move to separate micro service, data comes via FTP and is aggregated
      bindActor[SchedulerActorETHLaegeren]("scheduler-actor-ethlae")
      bind(classOf[SchedulerETHLaegeren]).asEagerSingleton()

     //bindActor[SchedulerActorETHFFLaegeren]("scheduler-actor-ethlae-ff")
     //bind(classOf[SchedulerETHLaegerenFF]).asEagerSingleton()

     //bindActor[SchedulerActorETHDavosTower]("scheduler-actor-ethdav-t")
     //bind(classOf[SchedulerETHDavosT]).asEagerSingleton()

     //Export to SwissSMEX
     bindActor[SchedulerActorSwissSMEX]("scheduler-actor-swissmex")
     bind(classOf[SchedulerSwissSMEX]).asEagerSingleton()

    // Export of HexenRubi station to meteoschweiz in their required format
    //bindActor[SchedulerActorHexenRübiFixedFormatExport]("scheduler-actor-hexenrubi-export")
    //bind(classOf[SchedulerHexenRubiExport]).asEagerSingleton()

    //Send email with daily precipitation amount of one station
    bindActor[SchedulerActorOttPluvio]("scheduler-actor-ottpluvio")
    bind(classOf[SchedulerOttPluvio]).asEagerSingleton()

    //Send email with weekly precipitation amount of two stations
    bindActor[SchedulerActorWeeklyPrecipVOR]("scheduler-actor-weekly-precip")
    bind(classOf[SchedulerWeeklyPrecipVOR]).asEagerSingleton()

    //Old CR10X data import for only one station. Needs to be refactored to make it generic.
    bindActor[SchedulerActorCR10X]("scheduler-actor-cr10x")
    bind(classOf[SchedulerCR10X]).asEagerSingleton()

    //Below processes are required only on demand

    //Monitoring of Data delivery to meteoschweiz
    //bindActor[SchedulerActorMonitoringMeteoSwiss]("scheduler-actor-monitorMETSCH")
    //bind(classOf[SchedulerMonitorMeteoSchweiz]).asEagerSingleton()

    //ETL process for Phaeno data
    //bindActor[SchedulerActorPhano]("scheduler-actor-phano")
    //bind(classOf[SchedulerPhano]).asEagerSingleton()

    //ETL process for Ozone data from Laboratory
    //bindActor[SchedulerActorOzone]("scheduler-actor-ozone")
    //bind(classOf[SchedulerOzone]).asEagerSingleton()*/

    //ETL job for importing data for Soil
    //bindActor[SchedulerActorBodenSpa]("scheduler-actor-bodenspa")
    //bind(classOf[SchedulerBodenSpa]).asEagerSingleton()


  }

}

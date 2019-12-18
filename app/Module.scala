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
   /* bindActor[LogsSchedulerActor]("log-scheduler-actor")

    bind(classOf[Scheduler]).asEagerSingleton()
    bindActor[SchedulerActor]("scheduler-actor")

     bindActor[SchedulerActorCR1000]("scheduler-actor-cr1000")
     bind(classOf[SchedulerCR1000]).asEagerSingleton()*/

     //bindActor[SchedulerActorGP2Logger]("scheduler-actor-gp2logger")
     //bind(classOf[SchedulerGP2Logger]).asEagerSingleton()

     //bindActor[SchedulerActorETHFFLaegeren]("scheduler-actor-ethlae-ff")
     //bind(classOf[SchedulerETHLaegerenFF]).asEagerSingleton()

    bindActor[SchedulerActorETHDavosTower]("scheduler-actor-ethdav-t")
    bind(classOf[SchedulerETHDavosT]).asEagerSingleton()

    /*bindActor[SchedulerActorSwissSMEX]("scheduler-actor-swissmex")
    bind(classOf[SchedulerSwissSMEX]).asEagerSingleton()

    bindActor[SchedulerActorUniBasel]("scheduler-actor-unibasel")
    bind(classOf[SchedulerUniBasel]).asEagerSingleton()

    bindActor[SchedulerActorETHLaegeren]("scheduler-actor-ethlae")
    bind(classOf[SchedulerETHLaegeren]).asEagerSingleton()*/

    //bindActor[SchedulerActorHexenRubi]("scheduler-actor-hexenrubi")
    //bind(classOf[SchedulerHexenRubi]).asEagerSingleton()

    /*bindActor[SchedulerActorHexenRübiFixedFormatExport]("scheduler-actor-hexenrubi-export")
    bind(classOf[SchedulerHexenRubiExport]).asEagerSingleton()

    bindActor[SchedulerActorOttPluvio]("scheduler-actor-ottpluvio")
    bind(classOf[SchedulerOttPluvio]).asEagerSingleton()*/

    bindActor[SchedulerActorWeeklyPrecipVOR]("scheduler-actor-weekly-precip")
    bind(classOf[SchedulerWeeklyPrecipVOR]).asEagerSingleton()



    //bindActor[SchedulerActorMonitoringMeteoSwiss]("scheduler-actor-monitorMETSCH")
    //bind(classOf[SchedulerMonitorMeteoSchweiz]).asEagerSingleton()

    //bindActor[SchedulerActorPhano]("scheduler-actor-phano")
    //bind(classOf[SchedulerPhano]).asEagerSingleton()

    //bindActor[SchedulerActorOzone]("scheduler-actor-ozone")
    //bind(classOf[SchedulerOzone]).asEagerSingleton()*/

    //bindActor[SchedulerActorBodenSpa]("scheduler-actor-bodenspa")
    //bind(classOf[SchedulerBodenSpa]).asEagerSingleton()


  }

}

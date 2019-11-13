package models.domain


import anorm.SqlParser.get
import anorm.{Error, Row, RowParser, Success, TypeDoesNotMatch, ~}
import org.joda.time._
import anorm.JodaParameterMetaData._
import org.joda.time._
import org.joda.time.format._


import scala.math.BigInt

sealed abstract class Verdict {
  val code: Int
  val typeValue: String
  def fromCode(code: Int) : Verdict = {
    code match {
      case 1 => Summe
      case 2 => Durchschnitt
      case 3 => Vektor
      case _ => UnKnownVerdict
    }
  }
  def fromTypeValue(typeValue: String) : Verdict = {
    typeValue match {
      case "Summe" => Summe
      case "Durchschnitt" => Durchschnitt
      case "Vektor" => Vektor
      case _ => UnKnownVerdict
    }
  }
}

case object Summe extends Verdict {
  override val code = 1
  override val typeValue = "Summe"
}

case object Durchschnitt extends Verdict {
  override val code = 2
  override val typeValue = "Durchschnitt"
}
case object Vektor extends Verdict {
  override val code = 3
  override val typeValue = "Vektor"
}

case object UnKnownVerdict extends Verdict {
  override val code = -1
  override val typeValue = "Unknown"
}

object Verdict {
  val parser: RowParser[Verdict] = {
    get[Int]("CODE") ~
      get[String]("TEXT") map {
      case code ~ name => code match {
        case 1 => Summe
        case 2 => Durchschnitt
        case 3 => Vektor
        case _ => UnKnownVerdict
      }
    }
  }
}


sealed abstract class ManualValidation {
  val code: Int
  val typeValue: String
  def fromCode(code: Int) : ManualValidation = {
    code match {
      case 1 => MessWertOk
      case 2 => MessWertNotOk
      case _ => MessWertUnknown
    }
  }
  def fromTypeValue(typeValue: String) : ManualValidation = {
    typeValue match {
      case "Messwert OK" => MessWertOk
      case "Messwert nicht OK" => MessWertNotOk
      case _ => MessWertUnknown
    }
  }
}

case object MessWertOk extends ManualValidation {
  override val code = 1
  override val typeValue = "Messwert OK"
}

case object MessWertNotOk extends ManualValidation {
  override val code = 2
  override val typeValue = "Messwert nicht OK"
}

case object MessWertUnknown extends ManualValidation {
  override val code = -1
  override val typeValue = "Unknown Messwert"
}

object ManualValidation {
  val parser: RowParser[ManualValidation] = {
    get[Int]("CODE") ~
      get[String]("TEXT") map {
      case code ~ name => code match {
        case 1 => MessWertOk
        case 2 => MessWertNotOk
        case _ => MessWertUnknown
      }
    }
  }
}


sealed abstract class Ursprung {
  val code: Int
  val typeValue: String
  def fromCode(code: Int) : Ursprung = {
    code match {
      case 1 => MesswertOriginalValue
      case 2 => MessWertInterpolatedValue
      case _ => UrsprungUnknown
    }
  }
  def fromTypeValue(typeValue: String) : Ursprung = {
    typeValue match {
      case "Messwert" => MesswertOriginalValue
      case "Interpoliert nach Methode 1" => MessWertInterpolatedValue
      case _ => UrsprungUnknown
    }

  }
}

case object MesswertOriginalValue extends Ursprung {
  override val code = 1
  override val typeValue = "Messwert"
}

case object MessWertInterpolatedValue extends Ursprung {
  override val code = 2
  override val typeValue = "Interpoliert nach Methode 1"
}

case object UrsprungUnknown extends Ursprung {
  override val code = -1
  override val typeValue = "Unknown Ursprung"
}

object Ursprung {
  val parser: RowParser[Ursprung] = {
    get[Int]("CODE") ~
      get[String]("TEXT") map {
      case code ~ name => code match {
        case 1 => MesswertOriginalValue
        case 2 => MessWertInterpolatedValue
        case _ => UrsprungUnknown
      }
    }
  }
}


sealed abstract class StationType {
  val code: Int
  val typeValue: String
  def fromCode(code: Int) : StationType = {
    code match {
      case 1 => MiniMet
      case 2 => DataHog
      case 3 => Dues
      case 4 => Andere
      case _ => UnKnownStationType
    }
  }
  def fromTypeValue(typeValue: String) : StationType = {
    typeValue match {
      case "MiniMet" => MiniMet
      case "DataHog" => DataHog
      case "Dues" => Dues
      case "Andere" => Andere
      case _ => UnKnownStationType
    }

  }
}

case object MiniMet extends StationType {
  override val code = 1
  override val typeValue = "MiniMet"
}

case object DataHog extends StationType {
  override val code = 2
  override val typeValue = "DataHog"
}
case object Dues extends StationType {
  override val code = 3
  override val typeValue = "Dues"
}

case object Andere extends StationType {
  override val code = 4
  override val typeValue = "Andere"
}

case object UnKnownStationType extends StationType {
  override val code = -1
  override val typeValue = "UnKnownStationType"
}

object StationType {
  val parser: RowParser[StationType] =
    get[Int]("CODE") ~
      get[String]("TEXT") map {
      case code ~ name => code match {
        case 1 => MiniMet
        case 2 => DataHog
        case 3 => Dues
        case 4 => Andere
        case _ => UnKnownStationType
      }
    }
}

sealed abstract class Periode {
  val code: Int
  val periodValue: String
  val durationValue: BigInt
  def fromCode(code: Int) : Periode = {
    code match {
      case 1 => TenMinutes
      case 2 => SixtyMinutes
      case 3 => ThreeSixtyMinutes
      case 4 => TwoFourtyMinutes
      case 5 => OneTwentyMinutes
      case 6 => FiveMinutes
      case 7 => ThirtyMinutes
      case 8 => OneDay
      case 9 => OneMinute
      case 10 => Irregular
      case _ => UnknownPeriode
    }
  }
  def fromPeriodText(periodValue: String) : Periode = {
    periodValue match {
      case "10 Minuten" => TenMinutes
      case "60 Minuten" => SixtyMinutes
      case "360 Minuten" => ThreeSixtyMinutes
      case "240 Minuten" => TwoFourtyMinutes
      case "120 Minuten" => OneTwentyMinutes
      case "5 Minuten" => FiveMinutes
      case "30 Minuten" =>  ThirtyMinutes
      case "1 Tag" => OneDay
      case "1 Minute" => OneMinute
      case "unregelmässig" => Irregular
      case _ => UnknownPeriode
    }
  }
  def fromPeriodDuration(durationValue: BigInt) : Periode = {
    durationValue match {
      case a: BigInt if(a == 10) => TenMinutes
      case a: BigInt if(a == 60) => SixtyMinutes
      case a: BigInt if(a == 360) => ThreeSixtyMinutes
      case a: BigInt if(a == 240) => TwoFourtyMinutes
      case a: BigInt if(a == 120) => OneTwentyMinutes
      case a: BigInt if(a == 5) => FiveMinutes
      case a: BigInt if(a == 30) =>  ThirtyMinutes
      case a: BigInt if(a == 1440) => OneDay
      case a: BigInt if(a == 1) => OneMinute
      case a: BigInt if(a == 525600) => Irregular
      case _ => UnknownPeriode
    }
  }

}

object Periode {
   val parser: RowParser[Periode] = {
    get[Int]("CODE") ~
      get[String]("TEXT") ~
      get[BigInt]("PDAUER") map {
      case code ~ name ~ duration => code match {
        case 1 => TenMinutes
        case 2 => SixtyMinutes
        case 3 => ThreeSixtyMinutes
        case 4 => TwoFourtyMinutes
        case 5 => OneTwentyMinutes
        case 6 => FiveMinutes
        case 7 => ThirtyMinutes
        case 8 => OneDay
        case 9 => OneMinute
        case 10 => Irregular
        case _ => UnknownPeriode

      }
    }

  }
}


case object TenMinutes extends Periode {
  override val code = 1
  override val periodValue = "10 Minuten"
  override val durationValue: BigInt = BigInt(10)
}

case object SixtyMinutes extends Periode {
  override val code = 2
  override val periodValue = "60 Minuten"
  override val durationValue: BigInt = BigInt(60)
}

case object ThreeSixtyMinutes extends Periode {
  override val code = 3
  override val periodValue = "360 Minuten"
  override val durationValue: BigInt = BigInt(360)
}

case object TwoFourtyMinutes extends Periode {
  override val code = 4
  override val periodValue = "240 Minuten"
  override val durationValue: BigInt = BigInt(240)
}

case object OneTwentyMinutes extends Periode {
  override val code = 5
  override val periodValue = "120 Minuten"
  override val durationValue: BigInt = BigInt(120)
}

case object FiveMinutes extends Periode {
  override val code = 6
  override val periodValue = "5 Minuten"
  override val durationValue: BigInt = BigInt(5)
}

case object ThirtyMinutes extends Periode {
  override val code = 7
  override val periodValue = "30 Minuten"
  override val durationValue: BigInt = BigInt(30)
}

case object OneDay extends Periode {
  override val code = 8
  override val periodValue = "1 Tag"
  override val durationValue: BigInt = BigInt(1440)
}

case object OneMinute extends Periode {
  override val code = 9
  override val periodValue = "1 Minute"
  override val durationValue: BigInt = BigInt(1)
}

case object Irregular extends Periode {
  override val code = 10
  override val periodValue = "unregelmässig"
  override val durationValue: BigInt = BigInt(525600)
}

case object UnknownPeriode extends Periode {
  override val code = -1
  override val periodValue = "Unknown Period Duration"
  override val durationValue: BigInt = BigInt(-1)
}



case class Station (stationNumber: Int, stationsName: String, kurzNameCode: Option[Int])

object Station {
  val parser: RowParser[Station] = {
    get[Int]("STATNR") ~
      get[String]("BESCHR") ~
        get[Option[Int]]("STAT_GRUPPE") map {
          case stationNumber~stationsName~kurzNameCode => Station(stationNumber, stationsName, kurzNameCode)
    }
  }
}

case class Unit_Einheit(code: Int, unitValue: String)

object Unit_Einheit {
  val parser: RowParser[Unit_Einheit] = {
    get[Int]("CODE") ~
      get[String]("TEXT") map {
      case code~unitText => Unit_Einheit(code, unitText)
    }
  }
}



case class Sensor(sensorNumber: Int, posArt: String)

object Sensor {
  val parser: RowParser[Sensor] = {
    get[Int]("SENSORNR") ~
      get[String]("POSART") map {
      case sensorNr~posArt => Sensor(sensorNr, posArt)
    }
  }
}


case class MessArt(code: Int, name: String, period: Periode, einHeit: Unit_Einheit, verdict: Verdict)

object MessArt {
  val parser: RowParser[MessArt] = {
    get[Int]("messart.CODE") ~
      get[String]("messart.TEXT") ~
        Periode.parser ~
          Unit_Einheit.parser  ~
            Verdict.parser map {
              case code~name~period~unitEinheit~verdict => MessArt(code,name,period,unitEinheit,verdict)
    }
  }
}

case class MeteoStationConfiguration(station: Int,
                                     messArt: Int,
                                     configNumber: Int,
                                     sensorNr: Option[Int],
                                     validFromDate: String,
                                     validToDate: Option[String],
                                     folgeNr: Option[Int],
                                     clusterNr: Option[Int],
                                     completeness: Option[Int],
                                     methode: Option[String]
                                    )

object MeteoStationConfiguration {
  val parser: RowParser[MeteoStationConfiguration] = {
    get[Int]("STATNR") ~
      get[Int]("MESSART") ~
        get[Int]("KONFNR") ~
          get[Option[Int]]("SENSORNR") ~
            get[String]("ABDATUM") ~
              get[Option[String]]("BISDATUM") ~
                get[Option[Int]]("FOLGENR") ~
                  get[Option[Int]]("CLNR")~
                    get[Option[Int]]("completeness")~
                      get[Option[String]]("apply_method_agg") map {
                    case station ~ messart ~ confnr ~ sensor ~ abdatum ~ bisdatum ~ folgnr ~ clnr ~ completeness ~ method => MeteoStationConfiguration(station, messart, confnr, sensor, abdatum, bisdatum, folgnr, clnr, completeness, method)
    }
  }
}





case class StationAbbrevations(code: Int, kurzName: String, fullName: String)
object StationAbbrevation {
  val parser: RowParser[StationAbbrevations] = {
    get[Int]("CODE") ~
      get[String]("KURZ_BESCHR") ~
      get[String]("BESCHREIBUNG") map {
      case code ~ kurzName ~ name => StationAbbrevations(code, kurzName, name)
    }
  }
}


case class OttPulvioDataRow(sumPrecipitation: BigDecimal, countValues: BigDecimal)
object OttPulvioData {
  val parser: RowParser[OttPulvioDataRow] = {
    get[BigDecimal]("summessart") ~
      get[BigDecimal]("countval") map {
      case summessart ~ countval => OttPulvioDataRow(summessart, countval)
    }
  }
}

case class WeeklyPreciVordemwaldDataRow(tag: String, measdate: String, sumPrecipitation: BigDecimal, countValues: BigDecimal)
object WeeklyPreciVordemwaldData {
  val parser: RowParser[WeeklyPreciVordemwaldDataRow] = {
    get[String]("tag") ~
      get[String]("measdate") ~
        get[BigDecimal]("summessart") ~
          get[BigDecimal]("countval") map {
      case tag ~ measdate ~ summessart ~ countval => WeeklyPreciVordemwaldDataRow(tag, measdate, summessart, countval)
    }
  }
}


case class MeteoData(station: Station,
                     messArt: MessArt,
                     configuration: MeteoStationConfiguration,
                     dateReceived: LocalDate,
                     valueOfMeasurement: BigDecimal,
                     dateOfInsertion: LocalDate,
                     methodApplied: Ursprung,
                     status: ManualValidation
                    )


case class MessArtRow(code: Int,text: String, period: Int, messProjNr: Option[Int], pDauer: Int, multi: Int, einheit: String)
object MessArtRow {
  val parser: RowParser[MessArtRow] = {
    get[Int]("CODE") ~
      get[String]("TEXT") ~
      get[Int]("PERIODE") ~
      get[Option[Int]]("MPROJNR") ~
      get[Int]("PDAUER") ~
      get[Int] ("MULTI") ~
      get[String]("einheit") map {
      case code ~ text ~ periode ~ messprojnr ~ pDauer ~multi ~einheit=> {
        MessArtRow(code, text, periode, messprojnr, pDauer, multi, einheit)
      }
    }
  }
}

case class MeteoDataRow(station: Int,
                        messArt: Int,
                        configuration: Int,
                        dateReceived: String,
                        valueOfMeasurement: BigDecimal,
                        dateOfInsertion: String,
                        methodApplied: Int,
                        status: Option[Int])


case class MeteoDataRowTableInfo(meteoDataRow: MeteoDataRow, multi: Option[Int],filename: String)

object MeteoDataRow {
  val parser: RowParser[MeteoDataRow] = {
    get[Int]("STATNR") ~
      get[Int]("MESSART") ~
      get[Int]("KONFNR") ~
      get[String]("MESSDATE") ~
      get[BigDecimal]("MESSWERT") ~
      get[String]("EINFDATE") ~
      get[Int]("URSPRUNG") ~
      get[Option[Int]]("MANVAL") map {
      case station ~ messart ~ config ~ messdate ~ messwert ~ einfdat ~ ursprung ~ status =>
        {
          MeteoDataRow(station, messart, config, messdate, messwert, einfdat, ursprung, status)
        }
    }
  }
}

case class BodenDataRow(profilNr: BigInt,
                        profilKonfId: BigInt,
                        measurementDate: String,
                        valueOfMeasurement: BigDecimal,
                        valid: Int,
                        valVersion: Int)


case class PhanoPersonsDataRow(personNr: Int, personName: String)
object PhanoPersonsData {
  val parser: RowParser[PhanoPersonsDataRow] = {
    get[Int]("persnr") ~
      get[String]("personname") map {
      case persnr ~ personname => PhanoPersonsDataRow(persnr, personname)
    }
  }
}

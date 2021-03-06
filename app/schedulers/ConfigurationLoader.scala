package schedulers

import com.typesafe.config.Config
import play.api.Logger
import play.api.Configuration

import scala.collection.mutable

case class StationKonfig(fileName: String, statNr: Int,  projs : List[ParametersProject])

case class SpecialParamKonfig(measurementParameter: String, konfNr: Int)

case class ParametersProject(projNr :Int, param :Int, duration: Int)


case class ConfigurationMeteoSchweizData(frequency :Int,
                                         userNameFtp :String,
                                         passwordFtp :String,
                                         pathForFtpFolder :String,
                                         ftpUrlMeteo :String,
                                         pathInputFile :String,
                                         pathForLocalWrittenFiles :String,
                                         pathForArchivedFiles :String,
                                         pathForLogFiles :String,
                                         pathForArchivedLogFiles :String,
                                         pathForTempFiles: String)

case class ConfigurationSwissSMEXData(frequency :Int,
                                         userNameFtp :String,
                                         passwordFtp :String,
                                         pathForFtpFolder :String,
                                         ftpUrlMeteo :String,
                                         pathInputFile :String,
                                         pathForLocalWrittenFiles :String,
                                         pathForArchivedFiles :String,
                                         pathForLogFiles :String,
                                         pathForArchivedLogFiles :String,
                                         pathForTempFiles: String)


case class ConfigurationUniBaselData(frequency :Int,
                                     userNameFtp :String,
                                     passwordFtp :String,
                                     pathForFtpFolder :String,
                                     ftpUrlMeteo :String,
                                     pathInputFile :String,
                                     pathForLocalWrittenFiles :String,
                                     pathForArchivedFiles :String,
                                     pathForLogFiles :String,
                                     pathForArchivedLogFiles :String,
                                     pathForTempFiles: String)

case class ConfigurationCR10XData(frequency :Int,
                                  ftpUrlCR10X: String,
                                  userNameHexenRubi :String,
                                  passwordHexenRubi :String,
                                  pathForIncomingFileHexenRubi :String,
                                  pathForArchivedFiles :String,
                                  dataFileNameHexenRubi :String,
                                  stationNrHexenRubi :Int,
                                  projectNrHexenRubi : Int,
                                  periodeHexenRubi: Int,
                                  emailUserListHexenRubi: String
                                     )
case class ConfigurationBodenSpaData(frequencyBodenSpa :Int,
                                     pathForIncomingFileBodenSpa :String,
                                     pathForArchivedFilesBodenSpa :String,
                                     dataFileNameBodenSpa: String)

case class ConfigurationOttPluvioData(frequencyOttPluvio: Int, stationNrOttPluvio: Int, messartOttPluvio: Int, emailUserListOttPluvio: String, startTimeForOttPulvio: String)

case class ConfigurationMeteoSchweizReportData(frequencyMeteoSchweizMonitoring: Int, emailListMeteoSchweizMonitoring: String, startTimeForMeteoSchweizMonitoring: String)


case class ConfigurationPreciVordemwaldData(frequencyPreciVordemwald: Int, stationNrPreciVordemwaldF: Int, stationNrPreciVordemwaldB: Int, messartPreciVordemwaldB: Int, messartPreciVordemwaldF: Int, emailUserListPreciVordemwald: String, startTimeForPreciVordemwald: String)


case class CR1000LoggerFileConfig(frequencyCR1000 :Int,
                                  ftpUrlCR1000 :String,
                                  fptUserNameCR1000 :String,
                                  ftpPasswordCR1000 :String,
                                  ftpPathForIncomingFileCR1000 :String,
                                  ftpPathForCR1000FaultyFile :String,
                                  ftpPathForCR1000ArchiveFiles :String,
                                  stationConfigs: List[StationKonfig],
                                  emailUserList: String
                                 )

case class OzoneFileConfig(frequencyOzone :Int,
                                  ftpUrlOzone :String,
                                  fptUserNameOzone :String,
                                  ftpPasswordOzone :String,
                                  ftpPathForIncomingFileOzone :String,
                                  ftpPathForOzoneFaultyFile :String,
                                  ftpPathForOzoneArchiveFiles :String,
                                  emailUserList: String,
                                  pathForTempFiles: String
                                 )
case class PhanoFileConfig(frequencyPhano :Int,
                           ftpUrlPhano :String,
                           fptUserNamePhano :String,
                           ftpPasswordPhano :String,
                           ftpPathForIncomingFilePhano :String,
                           ftpPathForPhanoFaultyFile :String,
                           ftpPathForPhanoeArchiveFiles :String,
                           emailUserList: String
                          )


case class ETHLaegerenLoggerFileConfig(frequencyETHLae :Int,
                                  ftpUrlETHLae :String,
                                  fptUserNameETHLae :String,
                                  ftpPasswordETHLae :String,
                                  ftpPathForIncomingFileETHLae :String,
                                  ftpPathForETHLaeFaultyFile :String,
                                  ftpPathForETHLaeArchiveFiles :String,
                                  stationConfigs: List[StationKonfig],
                                  emailUserList: String,
                                       ethHeaderLineT1_47: String,
                                       ethHeaderPrefixT1_47: String,
                                       specialStationKonfNrsETHLae: Seq[SpecialParamKonfig]
                                 )

case class ETHDavosLoggerFileConfig(frequencyETHDav :Int,
                                       ftpUrlETHDav :String,
                                       fptUserNameETHDav :String,
                                       ftpPasswordETHDav :String,
                                       ftpPathForIncomingFileETHDav :String,
                                       ftpPathForETHDavFaultyFile :String,
                                       ftpPathForETHDavArchiveFiles :String,
                                       stationConfigs: List[StationKonfig],
                                       emailUserList: String,
                                       ethHeaderLineDav: String,
                                       ethHeaderPrefixDav: String,
                                       specialStationKonfNrsETHDav: Seq[SpecialParamKonfig]
                                      )

object ConfigurationLoader {


  def loadMeteoSchweizConfiguration(configuration: Configuration) = {
    val frequency = configuration.getInt("frequency").get
    val userNameFtp = configuration.getString("fptUserNameMeteo").get
    val passwordFtp = configuration.getString("ftpPasswordMeteo").get
    val pathForFtpFolder = configuration.getString("ftpPathForOutgoingFile").get
    val ftpUrlMeteo = configuration.getString("ftpUrlMeteo").get
    val pathInputFile = configuration.getString("pathInputFile").get
    val pathForLocalWrittenFiles = configuration.getString("pathForLocalWrittenFiles").get
    val pathForArchivedFiles = configuration.getString("pathForArchivedFiles").get
    val pathForLogFiles = configuration.getString("pathForLogFiles").get
    val pathForArchivedLogFiles = configuration.getString("pathForArchivedLogFiles").get
    val pathForTempFiles = configuration.getString("pathForTempFiles").get
    ConfigurationMeteoSchweizData(frequency, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, pathInputFile, pathForLocalWrittenFiles, pathForArchivedFiles, pathForLogFiles, pathForArchivedLogFiles, pathForTempFiles)
  }

  def loadSwissSMEXConfiguration(configuration: Configuration) = {
    val frequency = configuration.getInt("frequencySwissMEX").get
    val userNameFtp = configuration.getString("fptUserNameSwissMEX").get
    val passwordFtp = configuration.getString("ftpPasswordSwissMEX").get
    val pathForFtpFolder = configuration.getString("ftpPathForOutgoingFileSwissMEX").get
    val ftpUrlMeteo = configuration.getString("ftpUrlSwissMEX").get
    val pathInputFile = configuration.getString("ftpPathForIncomingFileSwissMEX").get
    val pathForLocalWrittenFiles = configuration.getString("pathForLocalWrittenFiles").get
    val pathForArchivedFiles = configuration.getString("pathForArchivedFiles").get
    val pathForLogFiles = configuration.getString("pathForLogFiles").get
    val pathForArchivedLogFiles = configuration.getString("pathForArchivedLogFiles").get
    val pathForTempFiles = configuration.getString("pathForTempFiles").get

    ConfigurationSwissSMEXData(frequency, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, pathInputFile, pathForLocalWrittenFiles, pathForArchivedFiles, pathForLogFiles, pathForArchivedLogFiles, pathForTempFiles)
  }

  def loadUniBaselConfiguration(configuration: Configuration) = {
    val frequency = configuration.getInt("frequencyUniBasel").get
    val userNameFtp = configuration.getString("fptUserNameUniBasel").get
    val passwordFtp = configuration.getString("ftpPasswordUniBasel").get
    val pathForFtpFolder = configuration.getString("ftpPathForOutgoingFileUniBasel").get
    val ftpUrlMeteo = configuration.getString("ftpUrlUniBasel").get
    val pathInputFile = configuration.getString("ftpPathForIncomingFileUniBasel").get
    val pathForLocalWrittenFiles = configuration.getString("pathForLocalWrittenFiles").get
    val pathForArchivedFiles = configuration.getString("pathForArchivedFiles").get
    val pathForLogFiles = configuration.getString("pathForLogFiles").get
    val pathForArchivedLogFiles = configuration.getString("pathForArchivedLogFiles").get
    val pathForTempFiles = configuration.getString("pathForTempFiles").get

    ConfigurationUniBaselData(frequency, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, pathInputFile, pathForLocalWrittenFiles, pathForArchivedFiles, pathForLogFiles, pathForArchivedLogFiles, pathForTempFiles)
  }

  def loadCR10XConfiguration(configuration: Configuration) = {
    val frequency = configuration.getInt("frequencyCR10X").get
    val ftpUrlCR10X = configuration.getString("ftpUrlCR10X").get
    val userNameHexenRubi = configuration.getString("userNameCR10X").get
    val passwordHexenRubi = configuration.getString("passwordCR10X").get
    val pathForIncomingFileHexenRubi = configuration.getString("pathForIncomingFileCR10X").get
    val pathForArchivedFiles = configuration.getString("pathForArchivedFiles").get
    val dataFileNameHexenRubi = configuration.getString("dataFileNameCR10X").get
    val stationNrHexenRubi = configuration.getInt("stationNrCR10X").get
    val projectNrHexenRubi = configuration.getInt("projectNrCR10X").get
    val periodeHexenRubi = configuration.getInt("periodeCR10X").get
    val emailUserListHexenRubi = configuration.getString("emailUserListCR10X").get
    import scala.collection.JavaConversions._


    ConfigurationCR10XData(frequency, ftpUrlCR10X, userNameHexenRubi, passwordHexenRubi, pathForIncomingFileHexenRubi, pathForArchivedFiles, dataFileNameHexenRubi ,stationNrHexenRubi, projectNrHexenRubi, periodeHexenRubi, emailUserListHexenRubi )
  }

  def loadBodenSpaConfiguration(configuration: Configuration) = {
    val frequency = configuration.getInt("frequencyBodenSpa").get
    val pathForIncomingFileBodenSpa = configuration.getString("pathForIncomingFileBodenSpa").get
    val pathForArchivedFilesBodenSpa = configuration.getString("pathForArchivedFilesBodenSpa").get
    val dataFileNameBodenSpa = configuration.getString("dataFileNameBodenSpa").get


    ConfigurationBodenSpaData(frequency, pathForIncomingFileBodenSpa, pathForArchivedFilesBodenSpa, dataFileNameBodenSpa)
  }

  def loadOttPluvioConfiguration(configuration: Configuration) = {
    val frequencyOttPluvio = configuration.getInt("frequencyOttPluvio").get
    val stationNrOttPluvio = configuration.getInt("stationNrOttPluvio").get
    val messartOttPluvio = configuration.getInt("messartOttPluvio").get
    val emailUserListOttPluvio = configuration.getString("emailUserListOttPluvio").get
    val startTimeForOttPulvio = configuration.getString("startTimeForOttPulvio").get
    ConfigurationOttPluvioData(frequencyOttPluvio, stationNrOttPluvio, messartOttPluvio, emailUserListOttPluvio, startTimeForOttPulvio)
  }


  def loadMeteoSchweizMonitorConfiguration(configuration: Configuration) = {
    val frequencyMeteoSchweizMonitoring = configuration.getInt("frequencyMeteoSchweizMonitoring").get
    val emailListMeteoSchweizMonitoring = configuration.getString("emailListMeteoSchweizMonitoring").get
    val startTimeForMeteoSchweizMonitoring = configuration.getString("startTimeForMeteoSchweizMonitoring").get
    ConfigurationMeteoSchweizReportData(frequencyMeteoSchweizMonitoring, emailListMeteoSchweizMonitoring, startTimeForMeteoSchweizMonitoring)
  }

  def loadPreciVordemwaldConfiguration(configuration: Configuration) = {
    val frequencyPreciVordemwald = configuration.getInt("frequencyPreciVordemwald").get
    val stationNrPreciVordemwaldF = configuration.getInt("stationNrPreciVordemwaldF").get
    val stationNrPreciVordemwaldB = configuration.getInt("stationNrPreciVordemwaldB").get
    val messartPreciVordemwaldB = configuration.getInt("messartPreciVordemwaldB").get
    val messartPreciVordemwaldF = configuration.getInt("messartPreciVordemwaldF").get

    val emailUserListPreciVordemwald = configuration.getString("emailUserListPreciVordemwald").get
    val startTimeForPreciVordemwald = configuration.getString("startTimeForPreciVordemwald").get
    ConfigurationPreciVordemwaldData(frequencyPreciVordemwald, stationNrPreciVordemwaldF, stationNrPreciVordemwaldB, messartPreciVordemwaldB, messartPreciVordemwaldF, emailUserListPreciVordemwald, startTimeForPreciVordemwald)
  }


  def loadCR1000Configuration(configuration: Configuration) = {
    val frequencyCR1000 = configuration.getInt("frequencyCR1000").get
    val ftpUrlCR1000 = configuration.getString("ftpUrlCR1000").get
    val fptUserNameCR1000 = configuration.getString("fptUserNameCR1000").get
    val ftpPasswordCR1000 = configuration.getString("ftpPasswordCR1000").get
    val ftpPathForIncomingFileCR1000 = configuration.getString("ftpPathForIncomingFileCR1000").get
    val ftpPathForCR1000FaultyFile = configuration.getString("ftpPathForCR1000FaultyFile").get
    val ftpPathForCR1000ArchiveFiles = configuration.getString("ftpPathForCR1000ArchiveFiles").get
    val cr1000EmailUserList = configuration.getString("cr1000EmailUserList").get
    import scala.collection.JavaConversions._

    val statKonfigs =  configuration.getConfigList("stationConfig").map { statKonfig =>
      statKonfig.map(sk => {
        val fileName = sk.getString("fileName").get
        val stationNumber = sk.getInt("stationNumber").get
        val params = sk.getConfigList("projectParam")
          .map(pList => {
         val ppList =  pList.map(pp => {
            val projNr = pp.getInt("projNr").get
            val numParams = pp.getInt("params").get
            val duration = pp.getInt("duration").get
           ParametersProject(projNr,numParams,duration)
          }).toList
            ppList
        })

        StationKonfig(fileName, stationNumber, params.getOrElse(List()))

      }).toList
    }.getOrElse(List())
    Logger.info(s"Station config are: ${statKonfigs.mkString("\n")}")

    CR1000LoggerFileConfig(frequencyCR1000, ftpUrlCR1000, fptUserNameCR1000, ftpPasswordCR1000, ftpPathForIncomingFileCR1000, ftpPathForCR1000FaultyFile, ftpPathForCR1000ArchiveFiles, statKonfigs, cr1000EmailUserList)
  }


  def loadGP2LoggerConfiguration(configuration: Configuration) = {
    val frequencyGP2Logger = configuration.getInt("frequencyGP2Logger").get
    val ftpUrlGP2Logger = configuration.getString("ftpUrlGP2Logger").get
    val fptUserNameGP2Logger = configuration.getString("fptUserNameGP2Logger").get
    val ftpPasswordGP2Logger = configuration.getString("ftpPasswordGP2Logger").get
    val ftpPathForIncomingFileGP2Logger = configuration.getString("ftpPathForIncomingFileGP2Logger").get
    val ftpPathForGP2LoggerFaultyFile = configuration.getString("ftpPathForGP2LoggerFaultyFile").get
    val ftpPathForGP2LoggerArchiveFiles = configuration.getString("ftpPathForGP2LoggerArchiveFiles").get
    val gp2LoggerEmailUserList = configuration.getString("gp2LoggerEmailUserList").get
    import scala.collection.JavaConversions._

    val statKonfigs =  configuration.getConfigList("stationConfig").map { statKonfig =>
      statKonfig.map(sk => {
        val fileName = sk.getString("fileName").get
        val stationNumber = sk.getInt("stationNumber").get
        val params = sk.getConfigList("projectParam")
          .map(pList => {
            val ppList =  pList.map(pp => {
              val projNr = pp.getInt("projNr").get
              val numParams = pp.getInt("params").get
              val duration = pp.getInt("duration").get
              ParametersProject(projNr,numParams,duration)
            }).toList
            ppList
          })

        StationKonfig(fileName, stationNumber, params.getOrElse(List()))

      }).toList
    }.getOrElse(List())
    Logger.info(s"Station config are: ${statKonfigs.mkString("\n")}")

    CR1000LoggerFileConfig(frequencyGP2Logger, ftpUrlGP2Logger, fptUserNameGP2Logger, ftpPasswordGP2Logger, ftpPathForIncomingFileGP2Logger, ftpPathForGP2LoggerFaultyFile, ftpPathForGP2LoggerArchiveFiles, statKonfigs, gp2LoggerEmailUserList)
  }

  def loadOzoneConfiguration(configuration: Configuration) = {
    val frequencyOzone = configuration.getInt("frequencyOzone").get
    val ftpUrlOzone = configuration.getString("ftpUrlOzone").get
    val fptUserNameOzone = configuration.getString("fptUserNameOzone").get
    val ftpPasswordOzone = configuration.getString("ftpPasswordOzone").get
    val ftpPathForIncomingFileOzone = configuration.getString("ftpPathForIncomingFileOzone").get
    val ftpPathForOzoneFaultyFile = configuration.getString("ftpPathForOzoneFaultyFile").get
    val ftpPathForOzoneArchiveFiles = configuration.getString("ftpPathForOzoneArchiveFiles").get
    val ozoneEmailUserList = configuration.getString("emailUserListOzone").get
    val pathForTempFiles = configuration.getString("pathForTempFiles").get

    OzoneFileConfig(frequencyOzone, ftpUrlOzone, fptUserNameOzone, ftpPasswordOzone, ftpPathForIncomingFileOzone, ftpPathForOzoneFaultyFile, ftpPathForOzoneArchiveFiles, ozoneEmailUserList, pathForTempFiles)
  }

  def loadPhanoConfiguration(configuration: Configuration) = {
    val frequencyPhano = configuration.getInt("frequencyPhano").get
    val ftpUrlPhano = configuration.getString("ftpUrlPhano").get
    val fptUserNamePhano = configuration.getString("fptUserNamePhano").get
    val ftpPasswordPhano = configuration.getString("ftpPasswordPhano").get
    val ftpPathForIncomingFilePhano = configuration.getString("ftpPathForIncomingFilePhano").get
    val ftpPathForPhanoFaultyFile = configuration.getString("ftpPathForPhanoFaultyFile").get
    val ftpPathForPhanoArchiveFiles = configuration.getString("ftpPathForPhanoArchiveFiles").get
    val phanoEmailUserList = configuration.getString("emailUserListPhano").get
    PhanoFileConfig(frequencyPhano, ftpUrlPhano, fptUserNamePhano, ftpPasswordPhano, ftpPathForIncomingFilePhano, ftpPathForPhanoFaultyFile, ftpPathForPhanoArchiveFiles, phanoEmailUserList)
  }

  def loadETHLaeConfiguration(configuration: Configuration) = {
    val frequencyETHLae = configuration.getInt("frequencyETHLae").get
    val ftpUrlETHLae = configuration.getString("ftpUrlETHLae").get
    val fptUserNameETHLae = configuration.getString("fptUserNameETHLae").get
    val ftpPasswordETHLae = configuration.getString("ftpPasswordETHLae").get
    val ftpPathForIncomingFileETHLae = configuration.getString("ftpPathForIncomingFileETHLae").get
    val ftpPathForETHLaeFaultyFile = configuration.getString("ftpPathForETHLaeFaultyFile").get
    val ftpPathForETHLaeArchiveFiles = configuration.getString("ftpPathForETHLaeArchiveFiles").get
    val ethLaeEmailUserList = configuration.getString("etHLaeEmailUserList").get
    val ethHeaderLineT1_47 = configuration.getString("ethHeaderLineT1_47").get
    val ethHeaderPrefixT1_47 = configuration.getString("ethHeaderPrefixT1_47").get
    import scala.collection.JavaConversions._

    val statKonfigs =  configuration.getConfigList("stationConfigETH").map { statKonfig =>
      statKonfig.map(sk => {
        val fileName = sk.getString("fileName").get
        val stationNumber = sk.getInt("stationNumber").get
        val params = sk.getConfigList("projectParam")
          .map(pList => {
            val ppList =  pList.map(pp => {
              val projNr = pp.getInt("projNr").get
              val numParams = pp.getInt("params").get
              val duration = pp.getInt("duration").get
              ParametersProject(projNr,numParams,duration)
            }).toList
            ppList
          })

        StationKonfig(fileName, stationNumber, params.getOrElse(List()))

      }).toList
    }.getOrElse(List())

    import scala.collection.JavaConversions._

    val specialStationKonfNrsETHLae: Seq[SpecialParamKonfig] =  configuration.getConfigList("specialStationKonfNrsETHLae").map { specialStatKonfig =>
      specialStatKonfig.flatMap(sk => {
        val windSpeed = sk.getInt("windSpeed").get
        val windDirection = sk.getInt("windDirection").get
        List(SpecialParamKonfig("windSpeed", windSpeed),SpecialParamKonfig("windDirection", windDirection))
      })}.toList.flatten
    Logger.info(s"Station config are: ${statKonfigs.mkString("\n")}")

    ETHLaegerenLoggerFileConfig(frequencyETHLae, ftpUrlETHLae, fptUserNameETHLae, ftpPasswordETHLae, ftpPathForIncomingFileETHLae, ftpPathForETHLaeFaultyFile, ftpPathForETHLaeArchiveFiles, statKonfigs, ethLaeEmailUserList, ethHeaderLineT1_47, ethHeaderPrefixT1_47, specialStationKonfNrsETHLae)
  }


  def loadETHLaeFFConfiguration(configuration: Configuration) = {
    val frequencyETHLaeFF = configuration.getInt("frequencyETHLaeFF").get
    val ftpUrlETHLaeFF = configuration.getString("ftpUrlETHLaeFF").get
    val fptUserNameETHLaeFF = configuration.getString("fptUserNameETHLaeFF").get
    val ftpPasswordETHLaeFF = configuration.getString("ftpPasswordETHLaeFF").get
    val ftpPathForIncomingFileETHLaeFF = configuration.getString("ftpPathForIncomingFileETHLaeFF").get
    val ftpPathForETHLaeFaultyFileFF = configuration.getString("ftpPathForETHLaeFaultyFileFF").get
    val ftpPathForETHLaeArchiveFilesFF = configuration.getString("ftpPathForETHLaeArchiveFilesFF").get
    val ethLaeEmailUserListFF = configuration.getString("etHLaeEmailUserListFF").get
    val ethHeaderLineFF = configuration.getString("ethHeaderLineFF").get
    val ethHeaderPrefixFF = configuration.getString("ethHeaderPrefixFF").get
    import scala.collection.JavaConversions._

    val statKonfigsFF =  configuration.getConfigList("stationConfigETH_FF").map { statKonfig =>
      statKonfig.map(sk => {
        val fileName = sk.getString("fileName").get
        val stationNumber = sk.getInt("stationNumber").get
        val params = sk.getConfigList("projectParam")
          .map(pList => {
            val ppList =  pList.map(pp => {
              val projNr = pp.getInt("projNr").get
              val numParams = pp.getInt("params").get
              val duration = pp.getInt("duration").get
              ParametersProject(projNr,numParams,duration)
            }).toList
            ppList
          })

        StationKonfig(fileName, stationNumber, params.getOrElse(List()))

      }).toList
    }.getOrElse(List())

    import scala.collection.JavaConversions._

    val specialStationKonfNrsETHLae: Seq[SpecialParamKonfig] = Seq()
    Logger.info(s"Station config are: ${statKonfigsFF.mkString("\n")}")

    ETHLaegerenLoggerFileConfig(frequencyETHLaeFF, ftpUrlETHLaeFF, fptUserNameETHLaeFF, ftpPasswordETHLaeFF, ftpPathForIncomingFileETHLaeFF, ftpPathForETHLaeFaultyFileFF, ftpPathForETHLaeArchiveFilesFF, statKonfigsFF, ethLaeEmailUserListFF, ethHeaderLineFF, ethHeaderPrefixFF, specialStationKonfNrsETHLae)
  }

  def loadETHDavTConfiguration(configuration: Configuration) = {
    val frequencyETHDavT = configuration.getInt("frequencyETHDavT").get
    val ftpUrlETHDavT = configuration.getString("ftpUrlETHDavT").get
    val fptUserNameETHDavT = configuration.getString("fptUserNameETHDavT").get
    val ftpPasswordETHDavT = configuration.getString("ftpPasswordETHDavT").get
    val ftpPathForIncomingFileETHDavT = configuration.getString("ftpPathForIncomingFileETHDavT").get
    val ftpPathForETHFaultyFileDavT = configuration.getString("ftpPathForETHFaultyFileDavT").get
    val ftpPathForETHArchiveFilesDavT = configuration.getString("ftpPathForETHArchiveFilesDavT").get
    val etHEmailUserListDavT = configuration.getString("etHEmailUserListDavT").get
    val ethHeaderLineDavT = configuration.getString("ethHeaderLineDavT").get
    val ethHeaderPrefixDavT = configuration.getString("ethHeaderPrefixDavT").get
    import scala.collection.JavaConversions._

    val statKonfigsFF =  configuration.getConfigList("stationConfigETH_DAV_T").map { statKonfig =>
      statKonfig.map(sk => {
        val fileName = sk.getString("fileName").get
        val stationNumber = sk.getInt("stationNumber").get
        val params = sk.getConfigList("projectParam")
          .map(pList => {
            val ppList =  pList.map(pp => {
              val projNr = pp.getInt("projNr").get
              val numParams = pp.getInt("params").get
              val duration = pp.getInt("duration").get
              ParametersProject(projNr,numParams,duration)
            }).toList
            ppList
          })

        StationKonfig(fileName, stationNumber, params.getOrElse(List()))

      }).toList
    }.getOrElse(List())

    import scala.collection.JavaConversions._

    val specialStationKonfNrsETHDav: Seq[SpecialParamKonfig] = Seq()
    Logger.info(s"Station config are: ${statKonfigsFF.mkString("\n")}")

    ETHDavosLoggerFileConfig(frequencyETHDavT, ftpUrlETHDavT, fptUserNameETHDavT, ftpPasswordETHDavT, ftpPathForIncomingFileETHDavT, ftpPathForETHFaultyFileDavT, ftpPathForETHArchiveFilesDavT, statKonfigsFF, etHEmailUserListDavT, ethHeaderLineDavT, ethHeaderPrefixDavT, specialStationKonfNrsETHDav)
  }

}

package schedulers

import com.typesafe.config.Config
import play.api.Logger
import play.api.Configuration

import scala.collection.mutable

case class StationKonfig(fileName: String, statNr: Int,  projs : List[ParametersProject])

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
                                         pathForArchivedLogFiles :String)

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
                                  emailUserList: String
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
    ConfigurationMeteoSchweizData(frequency, userNameFtp, passwordFtp, pathForFtpFolder, ftpUrlMeteo, pathInputFile, pathForLocalWrittenFiles, pathForArchivedFiles, pathForLogFiles, pathForArchivedLogFiles)
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

  def loadOzoneConfiguration(configuration: Configuration) = {
    val frequencyOzone = configuration.getInt("frequencyOzone").get
    val ftpUrlOzone = configuration.getString("ftpUrlOzone").get
    val fptUserNameOzone = configuration.getString("fptUserNameOzone").get
    val ftpPasswordOzone = configuration.getString("ftpPasswordOzone").get
    val ftpPathForIncomingFileOzone = configuration.getString("ftpPathForIncomingFileOzone").get
    val ftpPathForOzoneFaultyFile = configuration.getString("ftpPathForOzoneFaultyFile").get
    val ftpPathForOzoneArchiveFiles = configuration.getString("ftpPathForOzoneArchiveFiles").get
    val ozoneEmailUserList = configuration.getString("emailUserListOzone").get
    OzoneFileConfig(frequencyOzone, ftpUrlOzone, fptUserNameOzone, ftpPasswordOzone, ftpPathForIncomingFileOzone, ftpPathForOzoneFaultyFile, ftpPathForOzoneArchiveFiles, ozoneEmailUserList)
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
    Logger.info(s"Station config are: ${statKonfigs.mkString("\n")}")

    ETHLaegerenLoggerFileConfig(frequencyETHLae, ftpUrlETHLae, fptUserNameETHLae, ftpPasswordETHLae, ftpPathForIncomingFileETHLae, ftpPathForETHLaeFaultyFile, ftpPathForETHLaeArchiveFiles, statKonfigs, ethLaeEmailUserList)
  }
}

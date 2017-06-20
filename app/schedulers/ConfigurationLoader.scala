package schedulers

import play.api.Configuration

case class ConfigurationMeteoSchweizData(frequency: Int, userNameFtp: String, passwordFtp: String, pathForFtpFolder: String, ftpUrlMeteo: String, pathInputFile: String, pathForLocalWrittenFiles: String, pathForArchivedFiles: String, pathForLogFiles: String, pathForArchivedLogFiles: String)
object ConfigurationLoader {


  def loadConfiguration(configuration: Configuration) = {
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

}

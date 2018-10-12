package models.phano

sealed trait PhanoExceptions {
  def formatErrorString(s: String): String = {
        s"${s}\n"
      }
}

case class PhanoInvalidPlaIDException(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class PhanoInvalidStationException(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneInvalidNumberOfParametersException(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneInvalidDateException(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneConfigNotFoundException(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class OzoneInvalidNumberFoundException(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}
case class PhanoNotSufficientParameters(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}

case class OzoneOracleError(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}

case class OzoneFileError(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}

case class OzoneFileLevelInfoMissingError(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}

case class OzoneSuspiciousDataLineError(errorCode: Int,errorMessage: String) extends PhanoExceptions {
  override def toString() = formatErrorString(errorMessage)
}

case class OzoneErrorFileInfo(fileName: String, errors : List[PhanoExceptions])




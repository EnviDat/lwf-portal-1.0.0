package models.services

import java.time.LocalDateTime
import javax.inject.Inject

import anorm.SqlParser.get
import models.domain.Ozone.{OzoneFileConfig, PassSammData}
import models.domain._
import models.repositories.{MeteoDataRepository, MeteorologyDataRepository}
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.libs.functional.syntax._


class MeteorologyService @Inject()(meteoRepo: MeteorologyDataRepository) {

  def insertSoilBodenSpaData(meteoData: Seq[BodenDataRow]) = meteoRepo.insertSoilBodenSpaMeasurementsData(meteoData)

  }


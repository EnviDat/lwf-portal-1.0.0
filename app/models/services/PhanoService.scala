package models.services

import javax.inject.Inject

import models.domain._
import models.domain.pheno.BesuchInfo
import models.ozone.OzoneOracleError
import models.repositories.{MeteorologyDataRepository, PhanoDataRepository}


class PhanoService @Inject()(phanoRepo: PhanoDataRepository) {

  def getPhanoPersonId(name: String): Int = {
    val allPersonsList = phanoRepo.getPhanoPersonsList()
    allPersonsList.find(nameToMatch => {
      val nameMatched = nameToMatch.personName.replaceAll(",", "").toUpperCase().sorted
      nameMatched.contains(name)
    }).map(_.personNr).getOrElse(-999)
  }

  def getPhanoStationId(stationName: String): Int = phanoRepo.getPhanoStationId(stationName).headOption.getOrElse(-999)
  def insertPhanoPlotBesuchDatums(besuchInfo: List[BesuchInfo], einfdat: String) = phanoRepo.insertPhanoPlotBesuchDatums(besuchInfo, einfdat)
  def getSpeciesId(speciesName: String): Int = phanoRepo.getPhanoSpeciesId(speciesName).headOption.getOrElse(-999)

  def insertBesuchInfo(besuchInfo: List[BesuchInfo]) = besuchInfo.map(phanoRepo.insertBesuchInfoData(_))




}


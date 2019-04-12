package models.services

import javax.inject.Inject

import models.domain.pheno.{AufnEreig, BesuchInfo, PhanoGrowthData, PhanoPFLA}
import models.repositories.PhanoDataRepository


class PhanoService @Inject()(phanoRepo: PhanoDataRepository) {

  def getPhanoPersonId(name: String): Int = {
    val allPersonsList = phanoRepo.getPhanoPersonsList()
    allPersonsList.find(nameToMatch => {
      val nameMatched = nameToMatch.personName.replaceAll(",", "").toUpperCase().sorted
      nameMatched.contains(name)
    }).map(_.personNr).getOrElse(-999)
  }

  def getPhanoStationId(stationName: String): Int = phanoRepo.getPhanoStationId(stationName).headOption.getOrElse(-999)
  def getSpeciesId(speciesName: String): Int = phanoRepo.getPhanoSpeciesId(speciesName).headOption.getOrElse(-999)

  def insertPhanoPlotBesuchDatums(besuchInfo: List[BesuchInfo], einfdat: String) = phanoRepo.insertPhanoPlotBesuchDatums(besuchInfo, einfdat)
  def insertBesuchInfo(besuchInfo: List[BesuchInfo]) = besuchInfo.map(phanoRepo.insertBesuchInfoData(_))
  def insertPhanoPFLA(phanoPfla: PhanoPFLA) = phanoRepo.insertPhanoPFLA(phanoPfla)
  def insertPhanoPFLAParameter(phanoPflaParam: PhanoGrowthData) = phanoRepo.insertPhanoPFLAParameter(phanoPflaParam)
  def insertPhanoAufnEreignis(phanoPflaAufnEreig: AufnEreig) = phanoRepo.insertPhanoAufnEreigParameter(phanoPflaAufnEreig)



}


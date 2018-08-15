package models.services

import models.domain.Ozone.{OzoneFileInfo, OzoneKeysConfig}
import models.domain._

class OzoneFileGeneratorFromDB(meteoService: MeteoService)  {


  def generateFiles(years: List[Int]): List[OzoneFileInfo] = {
   years.map(y => {
      val ozoneData = meteoService.getOzoneDataForYear(y)
     val fileLines = ozoneData.flatMap(oDataLine => {

        val countryCode = "50"
        val plotConfig = OzoneKeysConfig.defaultPlotConfigs.filter(p => p.clnrPlot == oDataLine.clnr).headOption
        val codePlot = plotConfig.map(_.codePlot)
        val namePlot = plotConfig.map(_.abbrePlot)

        val dateStart = oDataLine.startDate
        val timeStart = oDataLine.startTime
        val dateEnd = oDataLine.endDate
        val endTime = oDataLine.endTime
        val expTime = oDataLine.duration
        val codeCompund = "O3"

        val blindValue = oDataLine.blindwert
        val farbreagenz = oDataLine.farbreagens
        val calfactor = oDataLine.calfactor
        val analysDate = oDataLine.analysedatum
        val methodAnalysis = oDataLine.analysemethode
        val fileName = oDataLine.filename
        val sammelMethod = oDataLine.sammel
        val nachweisgrenze = oDataLine.nachweisgrenze
        val fileBem = oDataLine.fileBem
        val dataBem = oDataLine.lineBem
        val mittel = oDataLine.mittel
        val relSD = oDataLine.relsd

        val stringBeforeSamplerValues = s"""${countryCode};${codePlot.getOrElse("-9999")};${namePlot.getOrElse("")};${dateStart};${timeStart};${dateEnd};${endTime};${expTime};${codeCompund};"""
        val stringAfterSamplerValues = s"""${mittel};${relSD};${blindValue};${farbreagenz};${calfactor};${analysDate};${methodAnalysis};${sammelMethod};${nachweisgrenze};${fileName};${fileBem};${dataBem}"""

        val sammplerNumber1 = 1
        val absorbCode1 = "WS " + oDataLine.absorpData1
        val absorbValue1 = oDataLine.rowData1
        val valueAQ1 = oDataLine.konzData1

        val samplerOneLine: String = stringBeforeSamplerValues + s"""${sammplerNumber1};${absorbCode1};${absorbValue1};${valueAQ1};""" + stringAfterSamplerValues


        val sammplerNumber2 = 2
        val absorbCode2 = "WS " + oDataLine.absorpData2
        val absorbValue2 = oDataLine.rowData2
        val valueAQ2 = oDataLine.konzData2

        val samplerTwoLine: String = stringBeforeSamplerValues + s"""${sammplerNumber2};${absorbCode2};${absorbValue2};${valueAQ2};""" + stringAfterSamplerValues


        val sammplerNumber3 = 3
        val absorbCode3 = "WS " + oDataLine.absorpData3
        val absorbValue3 = oDataLine.rowData3
        val valueAQ3 = oDataLine.konzData3

        val samplerThreeLine: String = stringBeforeSamplerValues + s"""${sammplerNumber3};${absorbCode3};${absorbValue3};${valueAQ3};""" + stringAfterSamplerValues


        val blindWertLine: Option[String] = if(oDataLine.bw_absorpData1 != -9999) {
          val sammplerNumber4 = "BW"
          val absorbCode4 = "WS " + oDataLine.bw_absorpData1
          val absorbValue4 = oDataLine.bw_rowData
          val valueAQ4 = oDataLine.bw_konzData1
          Some(stringBeforeSamplerValues + s"""${sammplerNumber4};${absorbCode4};${absorbValue4};${valueAQ4};""" + stringAfterSamplerValues)
        } else None

        if(blindWertLine.nonEmpty) {
          List(samplerOneLine, samplerTwoLine, samplerThreeLine, blindWertLine.get)
        } else {
          List(samplerOneLine, samplerTwoLine, samplerThreeLine)
        }
      })
     OzoneFileInfo("ICPQ_AQ_AQP_" + y + "CH_subm_ugm", OzoneKeysConfig.dbGeneratedFileHeader, fileLines)
    })
  }


  def saveLogInfoOfGeneratedFiles(fileInfos: List[MeteoDataFileLogInfo]) = {
   List()
  }

}
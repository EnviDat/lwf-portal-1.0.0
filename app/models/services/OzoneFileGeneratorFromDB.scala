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
        val expTime =  "=" + """"""" +  oDataLine.duration.toString().trim  + """""""
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
        val mittel = "=" + """"""" +  oDataLine.mittel.toString().trim  + """""""
        val relSD = "=" + """"""" + oDataLine.relsd.toString().trim  + """""""

        val stringBeforeSamplerValues = s"""${countryCode};${codePlot.getOrElse("-9999")};${namePlot.getOrElse("")};${dateStart};${timeStart};${dateEnd};${endTime};${expTime};${codeCompund};"""
        val stringAfterSamplerValues = s"""${mittel};${relSD};${blindValue};${farbreagenz};${calfactor};${analysDate};${methodAnalysis};${sammelMethod};${nachweisgrenze};${fileName};${fileBem};${dataBem}"""
        val sammplerNumber1 = if (oDataLine.blindSampler == 1) "BS 1" else if(oDataLine.blindSampler == 2) "NWG 1" else  "1"

        val absorbCode1 = "WS " + oDataLine.absorpData1
        val absorbValue1 = oDataLine.rowData1
        val valueAQ1 = "=" + """"""" +  oDataLine.konzData1.toString().trim  + """""""

        val samplerOneLine: Option[String] = if(oDataLine.absorpData1 == BigDecimal(-9999) && oDataLine.blindSampler == 1)
          None
       else
          Some(stringBeforeSamplerValues + s"""${sammplerNumber1};${absorbCode1};${absorbValue1};${valueAQ1};""" + stringAfterSamplerValues)


        val sammplerNumber2 = if (oDataLine.blindSampler == 1) "BS 2" else if(oDataLine.blindSampler == 2) "NWG 2" else  "2"
        val absorbCode2 = "WS " + oDataLine.absorpData2
        val absorbValue2 = oDataLine.rowData2
        val valueAQ2 = "=" + """"""" +  oDataLine.konzData2.toString().trim  + """""""

        val samplerTwoLine: Option[String] = if(oDataLine.absorpData2 == BigDecimal(-9999) && oDataLine.blindSampler == 1)
          None
       else
          Some(stringBeforeSamplerValues + s"""${sammplerNumber2};${absorbCode2};${absorbValue2};${valueAQ2};""" + stringAfterSamplerValues)


        val sammplerNumber3 = if (oDataLine.blindSampler == 1) "BS 3" else if(oDataLine.blindSampler == 2) "NWG 3" else  "3"
        val absorbCode3 = "WS " + oDataLine.absorpData3
        val absorbValue3 = oDataLine.rowData3
        val valueAQ3 = "=" + """"""" +  oDataLine.konzData3.toString().trim   + """""""

        val samplerThreeLine: Option[String] = if(oDataLine.absorpData3 == BigDecimal(-9999) && oDataLine.blindSampler == 1)
          None
       else
          Some(stringBeforeSamplerValues + s"""${sammplerNumber3};${absorbCode3};${absorbValue3};${valueAQ3};""" + stringAfterSamplerValues)

          List(samplerOneLine, samplerTwoLine, samplerThreeLine).flatten
      })
     OzoneFileInfo("ICPQ_AQ_AQP_" + y + "CH_subm_ugm", OzoneKeysConfig.dbGeneratedFileHeader, fileLines)
    })
  }


  def saveLogInfoOfGeneratedFiles(fileInfos: List[MeteoDataFileLogInfo]) = {
   List()
  }

}
/**
Open Bank Project - API
Copyright (C) 2011, 2014, TESOBE / Music Pictures Ltd

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Email: contact@tesobe.com
TESOBE / Music Pictures Ltd
Osloerstrasse 16/17
Berlin 13359, Germany

  This product includes software developed at
  TESOBE (http://www.tesobe.com/)
  by
  Ayoub Benali: ayoub AT tesobe DOT com

 */
package com.tesobe.status.api

import net.liftweb.http.rest.RestHelper
import net.liftweb.common.Loggable

object Statues extends RestHelper with Loggable{
  import net.liftweb.json.JsonAST.JString
  import net.liftweb.json.Extraction
  import net.liftweb.common.Full
  import com.tesobe.status.model.DetailedBankStatues
  import net.liftweb.actor.LAFuture
  import com.tesobe.status.messageQueue.BankStatuesHandler
  import net.liftweb.http.JsonResponse

  serve{
    case "data" :: Nil JsonGet json => {
      val banksStatues: LAFuture[DetailedBankStatues] = new LAFuture()
      val actor  = new BankStatuesHandler(banksStatues)


      banksStatues.get(8000) match {
        case Full(statuesReplay) =>{
          val body= Extraction.decompose(statuesReplay)
          JsonResponse(body, Nil, Nil, 200)
        }
        case _ => {
          logger.warn("future time out.")
          case class Error(
            error: String
          )
          val body = Extraction.decompose(Error("could not fetch the bank statues"))

          JsonResponse(body, Nil, Nil, 500)
        }
      }
    }
  }
}
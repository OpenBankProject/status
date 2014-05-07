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

package com.tesobe.status.snippet

import net.liftweb.common.Loggable

//TODO: use comet actor to generate this page
class Status extends Loggable{
  import net.liftweb.util.Helpers._
  import scala.xml.NodeSeq

  def render(xhtml: NodeSeq) : NodeSeq = {
    import net.liftweb.actor.{
      LAFuture,
      LAScheduler
    }
    import net.liftweb.http.S
    import net.liftweb.common.Full
    import java.util.Date

    import com.tesobe.status.messageQueue.BankStatuesHandler
    import com.tesobe.status.model.DetailedBankStatues
    import com.tesobe.status.util.LiftHelper._

    lazy val NOOP_SELECTOR = "#i_am_an_id_that_should_never_exist" #> ""

    val banksStatues: LAFuture[DetailedBankStatues] = new LAFuture()
    val actor  = new BankStatuesHandler(banksStatues)
    val statues: LAFuture[NodeSeq] = new LAFuture()

    def generateNodeSeq(f: LAFuture[NodeSeq]): Unit = {
      banksStatues.foreach(reply => {
        val nodeSeq: NodeSeq =
          reply.statues.map(s =>{
            ".country *" #> s.country &
            ".bankName *" #> s"${s.name} - ${s.id}" &
            ".status *" #> s.tested &
            {
              val cssClassName = 
                if(s.tested)
                  "succeeded"
                else
                  "pending"
              ".status [class+]" #> cssClassName
            } &
            ".lastUpdate *" #>{
              s.lastTest match {
                case Some(date) => date.toString
                case _ => ""
              }
            }
          }).toList.flatMap(_.apply(xhtml))

        f.complete(Full(nodeSeq))
      })
    }

    LAScheduler.execute( ()=> generateNodeSeq(statues))

    ("#statues" #> statues).apply(xhtml)
  }
}
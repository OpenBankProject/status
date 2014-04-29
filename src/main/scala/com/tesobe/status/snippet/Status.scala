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
class Status extends Loggable{
  import net.liftweb.util.Helpers._
  import net.liftweb.common.Full
  import scala.xml.NodeSeq

  def render(xhtml: NodeSeq) : NodeSeq = {
    import com.tesobe.status.messageQueue.{
      MessageSender,
      BankStatuesListener
    }
    import net.liftweb.actor.{
      LAFuture,
      LiftActor
    }
    import net.liftmodules.amqp.AMQPMessage
    import net.liftweb.http.S

    import com.tesobe.status.model.BanksStatuesReply


    lazy val NOOP_SELECTOR = "#i_am_an_id_that_should_never_exist" #> ""

    MessageSender.getStatues

    val response: LAFuture[BanksStatuesReply] = new LAFuture()
    // watingActor waits for acknowledgment if the bank account was added
    object bankStatuesListener extends LiftActor with Loggable{
      protected def messageHandler = {
        case msg@AMQPMessage(statues: BanksStatuesReply) => {
          logger.info("received bank statues message")
          // complete the future
          response.complete(Full(statues))
        }
      }
    }

    BankStatuesListener.subscribe(bankStatuesListener)

    //TODO: change that to be asynchronous
    val cssSelector =
      response.get(5000) match {
        case Full(statuesReplay) =>{
          statuesReplay.statues.map(s =>{
            ".country *" #> s.country &
            ".bankName *" #> s.id &
            ".status *" #> s.status &
            ".lastUpdate *" #> s.lastUpdate.toString
          }).toList
        }
        case _ => {
          logger.warn("data storage time out.")
          S.error("could not fetch the bank statues")
          NOOP_SELECTOR :: Nil
        }
      }

    cssSelector.flatMap(_.apply(xhtml))
  }
}
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
    import net.liftweb.common.{Box, Full, Empty}
    import java.util.Date

    import com.tesobe.status.model.{BanksStatuesReply, SupportedBanksReply}

    case class DetailedBankStatus(
      country: String,
      id: String,
      name: String,
      tested: Boolean,
      lastTest: Option[Date]
    )
    case class DetailedBankStatues(
      statues: Set[DetailedBankStatus]
    )

    lazy val NOOP_SELECTOR = "#i_am_an_id_that_should_never_exist" #> ""

    MessageSender.getStatues
    MessageSender.getBankList

    val banksStatues: LAFuture[DetailedBankStatues] = new LAFuture()

    // watingActor waits for acknowledgment if the bank account was added
    object bankStatuesListener extends LiftActor with Loggable{
      private var bankStatues: Box[BanksStatuesReply] = Empty
      private var supportedBanks: Box[SupportedBanksReply] = Empty

      def completeFutureIfPossible(): Unit = {
        if(bankStatues.isDefined && supportedBanks.isDefined){
          val statues: Set[DetailedBankStatus] =
            supportedBanks.get.banks.map{b => {
              val isFound = bankStatues.get.find(b.country, b.nationalIdentifier)
              val (status, lastUpdate) =
                if(isFound.isDefined){
                  (isFound.get.status, Some(isFound.get.lastUpdate))
                }else{
                  (false, None)
                }
              DetailedBankStatus(
                b.country,
                b.nationalIdentifier,
                b.name,
                status,
                lastUpdate
              )
            }}
          banksStatues.complete(Full(DetailedBankStatues(statues)))
        }
      }

      protected def messageHandler = {
        case msg@AMQPMessage(statues: BanksStatuesReply) => {
          logger.info("received bank statues message")
          if(bankStatues.isEmpty){
            bankStatues = Full(statues)
            completeFutureIfPossible
          }
        }

        case msg@AMQPMessage(banks: SupportedBanksReply) => {
          logger.info("received supported banks message")
          if(supportedBanks.isEmpty){
            supportedBanks = Full(banks)
            completeFutureIfPossible
          }

        }
      }
    }

    BankStatuesListener.subscribeForBanksStatues(bankStatuesListener)
    BankStatuesListener.subscribeForBanksList(bankStatuesListener)

    //TODO: change that to be asynchronous
    val cssSelector =
      banksStatues.get(5000) match {
        case Full(statuesReplay) =>{
          statuesReplay.statues.map(s =>{
            ".country *" #> s.country &
            ".bankName *" #> s"${s.name} - ${s.id}" &
            ".status *" #> s.tested &
            ".lastUpdate *" #>{
              s.lastTest match {
                case Some(date) => date.toString
                case _ => ""
              }
            }
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
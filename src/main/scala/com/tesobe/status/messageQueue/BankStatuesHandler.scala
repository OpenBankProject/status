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
package com.tesobe.status.messageQueue

import net.liftweb.actor.{LiftActor, LAFuture}
import net.liftweb.common.Loggable

import com.tesobe.status.model.DetailedBankStatues

class BankStatuesHandler(banksStatues: LAFuture[DetailedBankStatues]) extends LiftActor with Loggable{
  import net.liftweb.common.{Full, Empty, Box}
  import net.liftmodules.amqp.AMQPMessage

  import com.tesobe.status.messageQueue.BankStatuesListener
  import com.tesobe.status.model.{
    BanksStatuesReply,
    SupportedBanksReply,
    DetailedBankStatus
  }

  private var bankStatues: Box[BanksStatuesReply] = Empty
  private var supportedBanks: Box[SupportedBanksReply] = Empty

  BankStatuesListener.subscribeForBanksStatues(this)
  BankStatuesListener.subscribeForBanksList(this)
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

  MessageSender.getStatues
  MessageSender.getBankList
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
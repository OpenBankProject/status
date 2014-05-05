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

import net.liftmodules.amqp.{AMQPAddListener,AMQPMessage, AMQPDispatcher, SerializedConsumer}
import com.rabbitmq.client.{ConnectionFactory, Channel}
import net.liftweb.actor._
import net.liftweb.common.Loggable
import net.liftweb.util._
import scala.concurrent
import scala.concurrent.ExecutionContext.Implicits.global



// an AMQP dispatcher that waits for message coming from a specif queue
// and dispatching them to the subscribed actors

class BankStatuesDispatcher[T](factory: ConnectionFactory)
    extends AMQPDispatcher[T](factory) {
  override def configure(channel: Channel) {
    channel.exchangeDeclare("bankStatuesResponse", "direct", false)
    channel.queueDeclare("bankStatues", false, false, false, null)
    channel.queueBind ("bankStatues", "bankStatuesResponse", "bankStatues")
    channel.basicConsume("bankStatues", false, new SerializedConsumer(channel, this))
  }
}

class BankListDispatcher[T](factory: ConnectionFactory)
    extends AMQPDispatcher[T](factory) {
  override def configure(channel: Channel) {
    channel.exchangeDeclare("banksListResponse", "direct", false)
    channel.queueDeclare("banksList", false, false, false, null)
    channel.queueBind ("banksList", "banksListResponse", "banksList")
    channel.basicConsume("banksList", false, new SerializedConsumer(channel, this))
  }
}

object BankStatuesListener extends Loggable{
  import com.tesobe.status.model.{BanksStatuesReply, SupportedBanksReply}

  private val amqp1 = new BankStatuesDispatcher[BanksStatuesReply](MQConnection.factory)
  private val amqp2 = new BankListDispatcher[SupportedBanksReply](MQConnection.factory)

  def subscribeForBanksStatues(actor: LiftActor): Unit = {
    amqp1 ! AMQPAddListener(actor)
  }

  def subscribeForBanksList(actor: LiftActor): Unit = {
    amqp2 ! AMQPAddListener(actor)
  }
}

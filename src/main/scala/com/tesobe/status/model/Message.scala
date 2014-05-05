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
package com.tesobe.status.model

import java.util.Date

case class GetSupportedBanks
case class SupportedBanksReply(
  banks: Set[BankInfo]
)
case class BankInfo(
  country: String,
  nationalIdentifier: String,
  name: String
)

case class GetBanksStatues
case class BankStatus(
  country: String,
  id: String,
  status: Boolean,
  lastUpdate: Date
)

case class BanksStatuesReply(
  statues: Set[BankStatus]
){
  def find(country: String, id: String): Option[BankStatus]= {
    statues.find(s => {s.country == country && s.id == id})
  }
}

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
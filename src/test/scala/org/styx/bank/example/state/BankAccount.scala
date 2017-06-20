package org.styx.bank.example.state

import org.styx.model.{DynamicData, Event}
import org.styx.state.State
import org.styx.state.State.AggregationId

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
case class BankAccount(lastEventVersion: Long, aggregationId: AggregationId) extends DynamicData with State
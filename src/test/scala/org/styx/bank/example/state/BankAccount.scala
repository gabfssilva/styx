package org.styx.bank.example.state

import org.styx.model.DynamicData
import org.styx.state.State
import org.styx.state.State.AggregationId

/**
  * @author Gabriel Francisco <peo_gfsilva@uolinc.com>
  */
case class BankAccount(aggregationId: AggregationId) extends DynamicData with State
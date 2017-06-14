package org.styx.bank.example.store

import org.styx.bank.example.state.BankAccount
import org.styx.store.{EventStore, InMemoryEventStore}

/**
  * @author Gabriel Francisco <peo_gfsilva@uolinc.com>
  */
object BankAccountEventStore {
  implicit val eventStore: EventStore[BankAccount] = new InMemoryEventStore[BankAccount]
}

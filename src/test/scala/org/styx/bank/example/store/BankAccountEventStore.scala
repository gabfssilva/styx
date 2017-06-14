package org.styx.bank.example.store

import org.styx.bank.example.state.BankAccount
import org.styx.store.{EventStore, InMemoryEventStore}

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object BankAccountEventStore {
  implicit val eventStore: EventStore[BankAccount] = new InMemoryEventStore[BankAccount]
}

package org.styx.bank.example.commands

import org.styx.bank.example.events.OwnerChanged
import org.styx.bank.example.state.BankAccount
import org.styx.command.Command
import org.styx.model.Request

import org.styx.bank.example.store.BankAccountEventStore._

object ChangeOwnerCommand extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    val event = OwnerChanged()
    event.newOwner = request.newOwner
    event
  }
}
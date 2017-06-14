package org.styx.bank.example.commands

import org.styx.bank.example.events.BankAccountCreated
import org.styx.bank.example.state.BankAccount
import org.styx.command.Command
import org.styx.model.Request

import org.styx.bank.example.store.BankAccountEventStore._

object CreateAccountCommand extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    val event = BankAccountCreated()
    event.id = request.id
    event.owner = request.owner
    event
  }

  override def validate: ValidationProduce =
    request => state => validation(state.status == null, "this account is already created")
}
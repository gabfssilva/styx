package org.styx.bank.example.commands

import org.styx.bank.example.events.BankAccountClosed
import org.styx.bank.example.state.BankAccount
import org.styx.command.Command
import org.styx.model.Request
import org.styx.bank.example.store.BankAccountEventStore._

import scala.concurrent.{ExecutionContext, Future}

class CloseCommand(implicit override val executionContext: ExecutionContext) extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    Future {
      val event = BankAccountClosed()
      event.closeReason = request.reason
      event
    }
  }

  override def validate: ValidationProduce =
    request => state => validation(!state.status.equals("CLOSED"), "this account is already closed")
}
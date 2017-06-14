package org.styx.bank.example.commands

import org.styx.bank.example.events.WithdrawalPerformed
import org.styx.bank.example.state.BankAccount
import org.styx.command.Command
import org.styx.model.Request

import org.styx.bank.example.store.BankAccountEventStore._

object WithdrawalCommand extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    val event = WithdrawalPerformed()
    event.amount = request.amount
    event
  }

  override def validate: ValidationProduce =
    request => state => validation((state.balance[Int] - request.amount[Int]) >= 0,
      s"the account cannot have a balance lower than zero. current balance: ${state.balance[Int]}, withdrawal amount: ${request.amount[Int]}")
}
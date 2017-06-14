package org.styx.bank.example.commands

import org.styx.bank.example.state.BankAccount
import org.styx.command.Command.ExecutionRequest
import org.styx.model.Request

object BankAccountCommands {
  val createAccount: ExecutionRequest[Request, BankAccount] = CreateAccountCommand
  val withdrawal: ExecutionRequest[Request, BankAccount] = WithdrawalCommand
  val deposit: ExecutionRequest[Request, BankAccount] = DepositCommand
  val changeOwner: ExecutionRequest[Request, BankAccount] = ChangeOwnerCommand
  val close: ExecutionRequest[Request, BankAccount] = CloseCommand
}
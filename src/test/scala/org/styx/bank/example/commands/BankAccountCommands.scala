package org.styx.bank.example.commands

import java.util.concurrent.Executors

import org.styx.bank.example.state.BankAccount
import org.styx.command.Command.ExecutionRequest
import org.styx.model.Request

import scala.concurrent.ExecutionContext
//import scala.concurrent.ExecutionContext.Implicits.global

object BankAccountCommands {
  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(10))

  val createAccount: ExecutionRequest[Request, BankAccount] = new CreateAccountCommand
  val withdrawal: ExecutionRequest[Request, BankAccount] = new WithdrawalCommand
  val deposit: ExecutionRequest[Request, BankAccount] = new DepositCommand
  val changeOwner: ExecutionRequest[Request, BankAccount] = new ChangeOwnerCommand
  val close: ExecutionRequest[Request, BankAccount] = new CloseCommand
}
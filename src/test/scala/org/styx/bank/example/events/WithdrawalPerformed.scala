package org.styx.bank.example.events

import org.styx.bank.example.state.BankAccount
import org.styx.model.Event

case class WithdrawalPerformed() extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)
    account copyTo newAccount
    newAccount.balance = account.balance[Int] - this.amount[Int]
    newAccount
  }
}
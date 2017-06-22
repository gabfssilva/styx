package org.styx.bank.example.events

import java.util.Date

import org.styx.bank.example.state.BankAccount
import org.styx.model.Event

case class DepositPerformed(override val revision: Long, override val eventDate: Date = new Date()) extends Event[BankAccount](revision) {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(revision, account.aggregationId)
    account copyTo newAccount
    newAccount.balance = account.balance[Int] + this.amount[Int]
    newAccount
  }
}
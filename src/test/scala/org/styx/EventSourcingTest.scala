package org.styx

import java.util.UUID

import org.scalatest.{FeatureSpec, Matchers}
import org.styx.bank.example.commands.BankAccountCommands._
import org.styx.bank.example.state.BankAccount
import org.styx.model.{Event, Request}
import org.styx.bank.example.store.BankAccountEventStore._
import org.styx.exceptions.InvalidExecutionException
import org.styx.player.EventPlayer._

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
class EventSourcingTest extends FeatureSpec with Matchers {
  feature("Creating an account") {
    scenario("assert that replaying restores the actual state of the BankAccount object") {
      val aggregationId = UUID.randomUUID().toString

      val f =
        createAccount(Request("owner" -> "John Doe", "id" -> 123))
          .andThen(deposit(Request("amount" -> 20)))
          .andThen(changeOwner(Request("newOwner" -> "Jane Doe")))
          .andThen(withdrawal(Request("amount" -> 10)))
          .andThen(close(Request("reason" -> "Unavailable address")))

      val actualState: BankAccount = f(BankAccount(aggregationId))
      val events: Seq[Event[BankAccount]] = eventStore.get(aggregationId)
      val playedState: BankAccount = events.play(BankAccount(aggregationId))

      actualState shouldEqual playedState
    }

    scenario("withdrawing more money than the balance has should throw an exception") {
      val aggregationId = UUID.randomUUID().toString

      val f =
        createAccount(Request("owner" -> "John Doe", "id" -> 123))
          .andThen(deposit(Request("amount" -> 20)))
          .andThen(changeOwner(Request("newOwner" -> "Jane Doe")))
          .andThen(withdrawal(Request("amount" -> 10)))
          .andThen(withdrawal(Request("amount" -> 10)))
          .andThen(withdrawal(Request("amount" -> 10)))
          .andThen(close(Request("reason" -> "Unavailable address")))

      an [InvalidExecutionException] should be thrownBy f(BankAccount(aggregationId))
    }
  }
}

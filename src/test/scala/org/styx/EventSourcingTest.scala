package org.styx

import java.util.UUID
import java.util.concurrent.Executors

import org.scalatest.{FeatureSpec, Matchers}
import org.styx.bank.example.commands.BankAccountCommands._
import org.styx.bank.example.state.BankAccount
import org.styx.bank.example.store.BankAccountEventStore._
import org.styx.exceptions.InvalidExecutionException
import org.styx.model.Request
import org.styx.player.EventPlayer._

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Success


/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
class EventSourcingTest extends FeatureSpec with Matchers {
  feature("Creating an account") {
    scenario("assert that replaying restores the actual state of the BankAccount object") {
      implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(20))

      val result = List.range(0, 2000).map { i =>
        val aggregationId = UUID.randomUUID().toString

        val eventualBankAccount = for {
          account <- createAccount(Request("owner" -> "John Doe", "id" -> 123))(BankAccount(aggregationId))
          account <- deposit(Request("amount" -> 20))(account)
          account <- changeOwner(Request("newOwner" -> "Jane Doe"))(account)
          account <- withdrawal(Request("amount" -> 10))(account)
          account <- close(Request("reason" -> "Unavailable address"))(account)
        } yield account

        val result: Future[BankAccount] = eventualBankAccount
          .andThen({
            case Success(state) => eventStore.get(aggregationId).play(BankAccount(aggregationId))
          })

        eventualBankAccount -> result
      }

      result.foreach(f => {
        val eventualActualState = f._1
        val eventualPlayedState = f._2

        val playedState = Await.result(eventualPlayedState, 60 seconds)
        val actualState = Await.result(eventualActualState, 60 seconds)

        playedState shouldBe actualState
      })
    }
  }

  scenario("withdrawing more money than the balance has should throw an exception") {
    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(3))

    val aggregationId = UUID.randomUUID().toString

    val eventualBankAccount = for {
      account <- createAccount(Request("owner" -> "John Doe", "id" -> 123))(BankAccount(aggregationId))
      account <- deposit(Request("amount" -> 20))(account)
      account <- changeOwner(Request("newOwner" -> "Jane Doe"))(account)
      account <- withdrawal(Request("amount" -> 10))(account)
      account <- withdrawal(Request("amount" -> 10))(account)
      account <- withdrawal(Request("amount" -> 10))(account)
      account <- close(Request("reason" -> "Unavailable address"))(account)
    } yield account

    an[InvalidExecutionException] should be thrownBy Await.result(eventualBankAccount, 1000 millis)
  }
}

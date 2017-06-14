# styx
A simple event sourcing library written in Scala.

The following example is based on: https://ookami86.github.io/event-sourcing-in-practice/


## The state

```scala
case class BankAccount(aggregationId: AggregationId) extends DynamicData with State
```

## The event store

```scala
object BankAccountEventStore {
  implicit val eventStore: EventStore[BankAccount] = new InMemoryEventStore[BankAccount]
}
```

## The events

```scala
case class BankAccountCreated() extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)

    newAccount.balance = 0
    newAccount.id = this.id
    newAccount.status = "ACTIVE"
    newAccount.owner = this.owner

    newAccount
  }
}
```

```scala
case class DepositPerformed() extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)
    account copyTo newAccount
    newAccount.balance = account.balance[Int] + this.amount[Int]
    newAccount
  }
}
```

```scala
case class OwnerChanged() extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)
    account copyTo newAccount
    newAccount.owner = this.newOwner
    newAccount
  }
}
```

```scala
case class WithdrawalPerformed() extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)
    account copyTo newAccount
    newAccount.balance = account.balance[Int] - this.amount[Int]
    newAccount
  }
}
```

```scala
case class BankAccountClosed() extends Event[BankAccount] {
  def applyTo(account: BankAccount): BankAccount = {
    val newAccount = BankAccount(account.aggregationId)
    account copyTo newAccount
    newAccount.closeReason = this.closeReason
    newAccount.status = "CLOSED"
    newAccount
  }
}
```

## The commands

```scala
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
```

```scala
object DepositCommand extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    val event = DepositPerformed()
    event.amount = request.amount
    event
  }
}
```

```scala
object ChangeOwnerCommand extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    val event = OwnerChanged()
    event.newOwner = request.newOwner
    event
  }
}
```

```scala
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
```

```scala
object CloseCommand extends Command[Request, BankAccount] {
  override def execute: ExecutionProduce = (request) => (_) => {
    val event = BankAccountClosed()
    event.closeReason = request.reason
    event
  }

  override def validate: ValidationProduce =
    request => state => validation(!state.status.equals("CLOSED"), "this account is already closed")
}
```

#### Commands as functions

```scala
object BankAccountCommands {
  val createAccount: ExecutionRequest[Request, BankAccount] = CreateAccountCommand
  val withdrawal: ExecutionRequest[Request, BankAccount] = WithdrawalCommand
  val deposit: ExecutionRequest[Request, BankAccount] = DepositCommand
  val changeOwner: ExecutionRequest[Request, BankAccount] = ChangeOwnerCommand
  val close: ExecutionRequest[Request, BankAccount] = CloseCommand
}
```

## Running

```scala
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

```


package org.styx.player

import org.styx.model.Event
import org.styx.state.State

import scala.concurrent.{ExecutionContext, Future}


/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object EventPlayer {
  implicit class EventPlayerImplicit[S <: State](val events: Seq[Event[S]]) {
    def play(initialState: S): S = EventPlayer.play(initialState, events)
  }

  implicit class FutureEventPlayerImplicit[S <: State](val events: Future[Seq[Event[S]]]) {
    def play(initialState: S)(implicit executionContext: ExecutionContext): Future[S] = events.map(e => e.play(initialState))
  }

  def play[S <: State](initialState: S, events: Seq[Event[S]]): S = events.foldLeft(initialState)((s, e) => e applyTo s)
}
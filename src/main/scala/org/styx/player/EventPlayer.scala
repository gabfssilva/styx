package org.styx.player

import org.styx.model.Event
import org.styx.state.State


/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object EventPlayer {
  implicit class EventPlayerImplicit[S <: State](val events: Seq[Event[S]]) {
    def play(initialState: S) = EventPlayer.play(initialState, events)
  }

  def play[S <: State](initialState: S, events: Seq[Event[S]]): S = events.foldLeft(initialState)((s, e) => e applyTo s)
}
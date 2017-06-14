package org.styx.model

import java.util.Date

import org.styx.state.State

abstract class Event[S <: State](val eventDate: Date = new Date()) extends DynamicData {
  override def toString: String = s"${this.getClass.getSimpleName}${super.toString}"

  def applyTo(state: S): S
}
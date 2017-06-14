package org.styx.model

case class Request(d: (String, Any)*) extends DynamicData(Map(d:_*))
package org.styx.mongo

import java.util.Date

import org.mongodb.scala.Document

case class MongoDBEvent(eventType: String,
                        eventDate: Date,
                        version: Long,
                        aggregationId: String,
                        data: Document)
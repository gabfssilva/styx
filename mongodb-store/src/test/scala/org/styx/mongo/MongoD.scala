package org.styx.mongo

import de.flapdoodle.embed.mongo.config.Net
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistries.{fromProviders, fromRegistries}
import org.mongodb.scala.bson.codecs.{DEFAULT_CODEC_REGISTRY, Macros}
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.model.IndexOptions
import org.mongodb.scala.{MongoClient, MongoCollection}

/**
  * @author Gabriel Francisco <gabfssilva@gmail.com>
  */
object MongoD {

  import de.flapdoodle.embed.mongo.MongodStarter
  import de.flapdoodle.embed.mongo.config.{IMongodConfig, MongodConfigBuilder}
  import de.flapdoodle.embed.mongo.distribution.Version
  import de.flapdoodle.embed.process.runtime.Network

  val starter: MongodStarter = MongodStarter.getDefaultInstance

  val bindIp = "localhost"
  val port = 27017
  val mongodConfig: IMongodConfig = new MongodConfigBuilder().version(Version.Main.V3_5).net(new Net(bindIp, port, Network.localhostIsIPv6)).build
  val mongodExecutable = starter.prepare(mongodConfig)

  import de.flapdoodle.embed.mongo.MongodProcess

  val mongod: MongodProcess = mongodExecutable.start

  val client = MongoClient(uri = "mongodb://localhost:27017/?waitqueuemultiple=10000")

  def codedProvider: CodecProvider = Macros.createCodecProvider[MongoDBEvent]()

  val registries = fromRegistries(fromProviders(codedProvider), DEFAULT_CODEC_REGISTRY)
  val db = client.getDatabase("eventSourcingSample").withCodecRegistry(registries)

  val collection: MongoCollection[MongoDBEvent] = db.getCollection("bankAccountEvents")

  val indexes = for {
    aggregationIdAndVersion <- collection.createIndex(Document("aggregationId" -> 1, "revision" -> 1), IndexOptions().unique(true))
  } yield aggregationIdAndVersion

  indexes.subscribe((indexes: String) => println("Indexes created:" + indexes))
}

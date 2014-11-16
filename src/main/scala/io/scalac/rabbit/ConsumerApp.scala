package io.scalac.rabbit

import scala.concurrent.Future
import scala.util.{Failure, Success}

import akka.actor.ActorSystem

import akka.stream.FlowMaterializer
import akka.stream.scaladsl.{Source, Sink}

import com.typesafe.scalalogging.slf4j.LazyLogging

import io.scalac.amqp.Connection

import io.scalac.rabbit.DeclarationsRegistry._


object ConsumerApp extends App with FlowFactory with LazyLogging {

  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
  
  import actorSystem.dispatcher
  
  implicit val materializer = FlowMaterializer()
  
  val connection = Connection()
  
  setupRabbit() onComplete { 
    case Success(_) =>
      logger.info("Exchanges, queues and bindings declared successfully.")
    
      val rabbitConsumer = Source(connection.consume(inboundQueue.name))
      val rabbitPublisher = Sink(connection.publish(outboundExchange.name))
      
      val flow = rabbitConsumer via consumerMapping via domainProcessing via publisherMapping to rabbitPublisher
    
      logger.info("Starting the flow")
      flow.run()
    case Failure(ex) =>
      logger.error("Failed to declare RabbitMQ infrastructure.", ex)
  }  
    
  def setupRabbit(): Future[List[Any]] =
    Future.sequence { List(
        
      /* declare and bind inbound exchange and queue */
      Future.sequence {
        connection.exchangeDeclare(inboundExchange) :: 
        connection.queueDeclare(inboundQueue) :: Nil
      } map { _ =>
	      connection.queueBind(inboundQueue.name, inboundExchange.name, "")          
      },

      /* declare and bind outbound exchange and queues */
      Future.sequence {
        connection.exchangeDeclare(outboundExchange) :: 
        connection.queueDeclare(outOkQueue) ::
        connection.queueDeclare(outNokQueue) :: Nil
      } map { _ => 
        connection.queueBind(outOkQueue.name, outboundExchange.name, outOkQueue.name) ::
	      connection.queueBind(outNokQueue.name, outboundExchange.name, outNokQueue.name) :: Nil
      })
    }
}
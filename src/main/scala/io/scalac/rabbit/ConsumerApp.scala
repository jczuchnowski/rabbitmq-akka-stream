package io.scalac.rabbit

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}

import akka.actor.ActorSystem

import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.stream.scaladsl._

import com.typesafe.scalalogging.slf4j.LazyLogging

import io.scalac.amqp.{Connection, Direct, Exchange, Queue}

import io.scalac.rabbit.DeclarationsRegistry._


object ConsumerApp extends App with FlowFactory with LazyLogging {

  implicit val actorSystem = ActorSystem("rabbit-akka-stream")
  
  implicit val executionConext = actorSystem.dispatcher
  
  implicit val materializer = FlowMaterializer(MaterializerSettings(actorSystem))
  
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
      logger.error("Failed to declare RabbitMQ objects.", ex)
  }  
    
  def setupRabbit(): Future[List[Any]] = {
    
    val declarations: List[Future[Any]] = 
      List(
        connection.exchangeDeclare(inboundExchange), 
        connection.exchangeDeclare(outboundExchange), 
        connection.queueDeclare(inboundQueue), 
        connection.queueDeclare(outOkQueue), 
        connection.queueDeclare(outNokQueue))

    val futureDeclarations = processFutures(declarations)
        
    futureDeclarations flatMap { _ =>

      val bindings: List[Future[Any]] = 
	      List(
	        connection.queueBind(inboundQueue.name, inboundExchange.name, ""), 
	        connection.queueBind(outOkQueue.name, outboundExchange.name, outOkQueue.name), 
	        connection.queueBind(outNokQueue.name, outboundExchange.name, outNokQueue.name))

	    processFutures(bindings)
    }
  }  

  def processFutures(ops: List[Future[Any]]): Future[List[Any]] = {
    val promiseOps = Promise[List[Any]]()
    
    ops foreach { _.onFailure { 
        case th => promiseOps.tryFailure(th) 
    }}

    Future.sequence(ops).foreach(promiseOps trySuccess _)

    promiseOps.future
  }
}
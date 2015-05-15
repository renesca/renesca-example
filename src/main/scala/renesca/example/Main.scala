package renesca.example

import renesca.graph.{Node, Relation}
import renesca.parameter._
import renesca.parameter.implicits._
import renesca.{DbService, RestService, Transaction}
import spray.http.BasicHttpCredentials

object Main extends App {
  // set up database connection
  val credentials = BasicHttpCredentials("neo4j", "neo4j")
  val restService = new RestService("http://localhost:7474", Some(credentials))

  val db = new DbService // interface for submitting single requests
  // dependency injection
  db.restService = restService


  // only proceed if database is available and empty
  val wholeGraph = db.queryGraph("MATCH (n) RETURN n LIMIT 1")
  if(wholeGraph.nonEmpty) {
    restService.system.shutdown()
    sys.error("Database is not empty.")
  }


  db.query("CREATE (:ANIMAL {name:'snake'})-[:EATS]->(:ANIMAL {name:'dog'})")

  val tx = new Transaction
  // dependency injection
  tx.restService = restService


  implicit val graph = tx.queryGraph("MATCH (n:ANIMAL)-[r]->() RETURN n,r")
  // query a subgraph from the database
  val snake = graph.nodes.find(_.properties("name").asInstanceOf[StringPropertyValue] == "snake").get // access the graph like scala collections

  // useful methods to access the graph (require implicit val graph in scope)
  // e.g.: neighbours,  successors,  predecessors,  inDegree,  outDegree,  degree, ...
  val name = snake.neighbours.head.properties("name") match {
    case StringPropertyValue(string) => string
    case _                           => ???
  }
  println("Name of one snake neighbour: " + name) // prints "dog"

  snake.labels += "REPTILE" // changes to the graph are tracked
  snake.properties("hungry") = true

  val hippo = Node.local // creating a local Node (a Node the database does not know about yet)
  hippo.labels += "ANIMAL" // changes to local Nodes are also tracked
  hippo.properties("name") = "hippo"
  graph.nodes += hippo // add the local node to the Node Set
  // create a new relation between a local and existing Node
  graph.relations += Relation.local(snake, "EATS", hippo)

  // persist all tracked changes to the database and commit the transaction
  tx.persistChanges(graph)
  tx.commit()
  //  ODO: tx.commit.persistChanges(graph) // not working yet


  val animals = db.queryTable("MATCH (n:ANIMAL)-[r:EATS]->() RETURN n.name as name, COUNT(r) as eatcount")
  //TODO: print instead of assert
  assert(animals.columns == List("name", "eatcount"))
  assert(animals.rows.maxBy(_.apply("eatcount").asInstanceOf[LongPropertyValue].value).apply("name").asInstanceOf[StringPropertyValue] == "snake")


  // shut down actor system
  restService.system.shutdown()
}

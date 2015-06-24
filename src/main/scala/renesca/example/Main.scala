package renesca.example

import renesca.graph.{Node, Relation}
import renesca.parameter._
import renesca.parameter.implicits._
import renesca.{DbService, RestService, Transaction, Query}
import spray.http.BasicHttpCredentials

object Main extends App {
  // set up database connection
  val credentials = BasicHttpCredentials("neo4j", "neo4j")
  // RestService contains an ActorSystem to handle HTTP communication via spray-client
  val restService = new RestService("http://localhost:7474", Some(credentials))

  // query interface for submitting single requests
  val db = new DbService
  // dependency injection
  db.restService = restService


  // only proceed if database is available and empty
  val wholeGraph = db.queryGraph("MATCH (n) RETURN n LIMIT 1")
  if(wholeGraph.nonEmpty) {
    restService.actorSystem.shutdown()
    sys.error("Database is not empty.")
  }


  // create example graph:  snake -eats-> dog
  db.query("CREATE (:ANIMAL {name:'snake'})-[:EATS]->(:ANIMAL {name:'dog'})")

  val tx = db.newTransaction

  // query a subgraph from the database
  implicit val graph = tx.queryGraph("MATCH (n:ANIMAL)-[r]->() RETURN n,r")

  // access the graph like scala collections
  val snake = graph.nodes.find(_.properties("name").
    asInstanceOf[StringPropertyValue] == "snake").get

  // useful methods to access the graph (requires implicit val graph in scope)
  // e.g.: neighbours,  successors,  predecessors,  inDegree,  outDegree,  degree, ...
  val name = snake.neighbours.head.properties("name").
    asInstanceOf[StringPropertyValue].value
  println("Name of one snake neighbour: " + name) // prints "dog"

  // changes to the graph are tracked
  snake.labels += "REPTILE"
  snake.properties("hungry") = true

  // creating a local Node (a Node the database does not know about yet)
  val hippo = Node.create

  // changes to locally created Nodes are also tracked
  hippo.labels += "ANIMAL"
  hippo.properties("name") = "hippo"

  // add the created node to the Node Set
  graph.nodes += hippo

  // create a new local relation from a locally created Node to an existing Node
  graph.relations += Relation.create(snake, "EATS", hippo)

  // persist all tracked changes to the database and commit the transaction
  tx.commit.persistChanges(graph)

  // different transaction syntax
  db.transaction { tx =>
    val hippo = tx.queryGraph(
      Query( """MATCH (n:ANIMAL {name: {name}}) return n""",
        Map("name" -> "hippo")) // Cypher query parameters
    ).nodes.head
    hippo.properties("nose") = true
  }

  db.transaction { tx =>
    // delete hippo
    tx.query( """MATCH (n:ANIMAL {name: "hippo"}) OPTIONAL MATCH (n)-[r]-() DELETE n,r""")

    // roll back deletion
    tx.rollback()
  }

  // interpret query result as a table
  val animals = db.queryTable("""MATCH (n:ANIMAL) OPTIONAL MATCH (n)-[r:EATS]->()
    RETURN n.name as name, COUNT(r) as eatcount""")

  println("\n" + animals.columns.mkString("\t")) // prints "name eatcount"
  for(row <- animals.rows) {
    print(row.cells(0).asInstanceOf[StringPropertyValue].value)
    print("\t")
    println(row.cells(1).asInstanceOf[LongPropertyValue].value)
  }
  println()
  // loop prints:
  //  dog	  0
  //  snake	2
  //  hippo	0

  val hungriest = animals.rows.maxBy(_.apply("eatcount").
    asInstanceOf[LongPropertyValue].value).
    apply("name").asInstanceOf[StringPropertyValue].value
  println("hungriest: " + hungriest) // prints "snake"


  // clear database
  db.query("MATCH (n) OPTIONAL MATCH (n)-[r]-() DELETE n,r")

  // shut down actor system
  restService.actorSystem.shutdown()
}

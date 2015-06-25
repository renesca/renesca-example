package renesca.example

import renesca.graph._
import renesca.parameter._
import renesca.parameter.implicits._

case class Animal(node: Node) {
  val label = Label("ANIMAL")
  def eats(implicit graph: Graph): Set[Food] = node.outRelations.
    filter(_.relationType == Eats.relationType).map(_.endNode).
    filter(_.labels.contains(Food.label)).map(Food.wrap)
  def name: String = node.properties("name").asInstanceOf[StringPropertyValue]
}

object Animal {
  val label = Label("ANIMAL")
  def wrap(node: Node) = new Animal(node)
  def create(name: String): Animal = {
    val wrapped = wrap(Node.create(List(label)))
    wrapped.node.properties.update("name", name)
    wrapped
  }
}

case class Food(node: Node) {
  val label = Label("FOOD")
  def rev_eats(implicit graph: Graph): Set[Animal] = node.inRelations.
    filter(_.relationType == Eats.relationType).map(_.startNode).
    filter(_.labels.contains(Animal.label)).map(Animal.wrap)
  def name: String = node.properties("name").asInstanceOf[StringPropertyValue]
  def amount: Long = node.properties("amount").asInstanceOf[LongPropertyValue]
  def `amount_=`(newValue: Long) { node.properties.update("amount", newValue) }
}

object Food {
  val label = Label("FOOD")
  def wrap(node: Node) = new Food(node)
  def create(amount: Long, name: String): Food = {
    val wrapped = wrap(Node.create(List(label)))
    wrapped.node.properties.update("amount", amount)
    wrapped.node.properties.update("name", name)
    wrapped
  }
}

case class Eats(startNode: Animal, relation: Relation, endNode: Food)

object Eats {
  val relationType = RelationType("EATS")
  def wrap(relation: Relation) = {
    Eats(Animal.wrap(relation.startNode), relation, Food.wrap(relation.endNode))
  }
  def create(startNode: Animal, endNode: Food): Eats = {
    wrap(Relation.create(startNode.node, relationType, endNode.node))
  }
}



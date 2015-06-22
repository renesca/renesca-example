# renesca-example

A simple example to demonstrate the features of the [renesca](https://github.com/renesca/renesca) library.

Find the code in [src/main/scala/renesca/example/Main.scala](https://github.com/renesca/renesca-example/blob/master/src/main/scala/renesca/example/Main.scala)

This example assumes your server is running on `http://localhost:7474` with username and password set to `neo4j`.
You can change this in the code.
It also expects your database to be completely empty and exits otherwise to not destroy any of your data.

To run this project type:
```sh
$ sbt run
```


This will print

```
Name of one snake neighbour: dog

name	eatcount
dog	0
snake	2
hippo	0

hungriest: snake
```

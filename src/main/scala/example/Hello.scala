package example

object Hello extends Greeting with App {

  //Client should be newed with a blocker, same with close of resource
  //care about covering https://docs.launchdarkly.com/sdk/server-side/java#variation variation -> identify

  println(greeting)
}

trait Greeting {
  lazy val greeting: String = "hello"
}

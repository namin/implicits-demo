/*
Demo: Implicits in Scala
========================
*/

package playground

import org.scalatest._

class ImplicitsTest extends FunSuite {

  case class Complex(r: Int, i: Int) {
    val a = this
    def + (b: Complex) =
      Complex(a.r+b.r, a.i+b.i)
    def - (b: Complex) =
      Complex(a.r - b.r, a.i-b.i)
    def * (b: Complex) =
      Complex(a.r*b.r - a.i*b.i, a.r*b.i + a.i*b.r)
    override def toString = s"($r + ${i}i)"
  }

  object Complex {
    implicit def fromInt(r: Int) = Complex(r, 0)
  }

  test("implicit conversion") {
    assert(Complex(2,1)*3==Complex(6,3))
    assert(3*Complex(2,1)==Complex(6,3))

    // Note: implicit fromInt is in Scope because it is in the companion object of Complex.
    // Try: Rename object Complex and notice that implicit is no longer in scope.
    // Try: Apply the conversion manually.
    // Try: Import the implicit by importing the new object as a package.
    // Try: Import the implicit by importing it selectively by name.
  }

  object pimp {
    def all2list(it: java.util.StringTokenizer) = {
      val buf = new scala.collection.mutable.ListBuffer[String]()
      while (it.hasMoreTokens) {
        buf += it.nextToken
      }
      buf.toList
    }
    implicit class PimpedOps[A](it: java.util.StringTokenizer) {
      def all: List[String] = all2list(it)
      def all[B](f: String => B): List[B] = all.map(f)
    }
  }
  test("pimp my library") {
    import pimp._
    def st = new java.util.StringTokenizer("this is a test");
    assert(st.all==List("this", "is", "a", "test"))
    assert(st.all(_.length)==List(4, 2, 1, 4))

    // Note: overloading resolution works here.
    // Try: replace import of pimp with pimp_ambiguous below.
  }

  object pimp_ambiguous {
    implicit class PimpedOps1[A](it: java.util.StringTokenizer) {
      def all: List[String] = pimp.all2list(it)
    }
    implicit class PimpedOps2[A](it: java.util.StringTokenizer) {
      def all[B](f: String => B): List[B] = pimp.all2list(it).map(f)
    }
  }

  test("implicit parameters") {

    // type classes, e.g. Numeric[T], Ordering[T]

    abstract class Ordering[A] {
      def compare(x:A,y:A): Int
    }

    implicit object intOrdering extends Ordering[Int] {
      def compare(x:Int,y:Int) = x-y
    }

    implicit object stringOrdering extends Ordering[String] {
      def compare(x:String,y:String) = x.compareTo(y)
    }

    assert(intOrdering.compare(3,7) < 0)

    def implicitly[T](implicit e: T) = e

    def ordering[T](implicit e: Ordering[T]) = e

    // the `T:Ordering` is typeclass-inspired syntactic sugar
    // for an additional implicit requirement:
    // def quicksort[T](xs: List[T])(implicit e: Ordering[T])
    def quicksort[T:Ordering](xs: List[T]): List[T] = xs match {
      case Nil => Nil
      case p::xs => 
        val (as,bs) = xs.partition(x => ordering[T].compare(x,p) < 0)
        quicksort(as) ++ (p::quicksort(bs))
    }

    def reverse[A](o: Ordering[A]) = new Ordering[A] {
      def compare(x:A,y:A) = - o.compare(x,y)
    }

    val xs1 = quicksort(List(2,9,3,8,1))

    assert(xs1 == List(1,2,3,8,9))

    val xs2 = quicksort(List(2,9,3,8,1))(reverse(intOrdering))

    assert(xs2 == List(9,8,3,2,1))

    // instead of the function syntax quicksort() we may want to use 
    // xs.quicksort: let's use pimp my library again

    implicit class RichList[T](xs:List[T]) {
      def quicksort(implicit ev: Ordering[T]): List[T] = xs match {
        case Nil => Nil
        case p::xs => 
          val (as,bs) = xs.partition(x => ev.compare(x,p) < 0)
          as.quicksort ++ (p::bs.quicksort)
      }
    }

    implicit class RichOrdering[A](o: Ordering[A]) {
      def reverse = new Ordering[A] {
        def compare(x:A,y:A) = - o.compare(x,y)
      }
    }

    val xs3 = List(2,9,3,8,1).quicksort

    assert(xs3 == List(1,2,3,8,9))

    val xs4 = List(2,9,3,8,1).quicksort(intOrdering.reverse)

    assert(xs4 == List(9,8,3,2,1))


    // here's something weird:

    val xs0 = List(2,9,3,8,1)

    assert(xs0(2) == 3)

    // assert(xs0.quicksort(2) == 3)
    //
    //  found   : Int(2)
    //  required: Ordering[Int]
    //     assert(xs0.quicksort(2) == 3)

    // assert(quicksort(xs0)(2) == 3)
    //
    // (same error)
  }

  test("implicit resolution") {

    // companion object: low-priority (--> CanBuildFrom again)

    abstract class Ordering[A] {
      def compare(x:A,y:A): Int
    }

    trait LowPriorityImplicits {
      implicit object intOrdering extends Ordering[Int] {
        def compare(x:Int,y:Int) = x-y
      }
    }

    object Ordering extends LowPriorityImplicits

    implicit object myIntOrdering extends Ordering[String] {
      def compare(x:String,y:String) = x.compareTo(y)
    }

    implicit object stringOrdering extends Ordering[String] {
      def compare(x:String,y:String) = x.compareTo(y)
    }

    implicit object myStringOrdering extends Ordering[String] {
      def compare(x:String,y:String) = x.compareTo(y)
    }

    def ordering[T](implicit e: Ordering[T]) = e

    val oint = ordering[Int] // ok

    // val ostr = ordering[String]
    //
    // ambiguous implicit values:
    //   both object myStringOrdering of type myStringOrdering.type
    //   and object stringOrdering of type stringOrdering.type
    //   match expected type Ordering[String]
    //      val ostr = ordering[String]
    //                         ^

    // used in the collection library (CanBuildFrom)

    import scala.collection.immutable.BitSet

    val bs1 = BitSet(1,4,9,3)

    val bs2: BitSet = bs1 map (x => 2 * x)

    val s1: Set[String] = bs1 map (x => x.toString)


    def convert(xs: Set[Int]) = xs map (x => 2 * x)

    // val s3: BitSet = convert(bs1)
    //
    // type mismatch;
    //   found   : scala.collection.immutable.Set[Int]
    //   required: scala.collection.immutable.BitSet
    //      val s3: BitSet = convert(bs1)
    //                              ^

    import scala.collection.generic.CanBuildFrom
    import scala.collection.SetLike

    def convert2[A <: SetLike[Int,A] with Set[Int],B](xs: A)(implicit ev: CanBuildFrom[A,Int,B]): B = 
      xs.map (x => 2 * x)(ev)

    val s3: BitSet = convert2(bs1)

  }

  object resolution {
    implicit def generateDefault[A](implicit f: () => A): A = f()
    implicit def defaultString: String = ""
    implicit def generateInt: () => Int = () => 1
  }
  test("implicit resolution and priority") {
    import resolution._
    assert(implicitly[String]=="")
    assert(implicitly[Int]==1)
  }

  object resolution_specificity {
    implicit def defaultInt: Int = 0
  }
  test("implicit resolution and specificity") {
    import resolution._
    import resolution_specificity._
    assert(implicitly[Int]==0)
    assert(implicitly[Int]==0)
  }

  object resolution_chaining {
    implicit def defaultComplex(implicit i: Int): Complex = i // manual chaining
  }
  test("implicit resolution and chaining") {
    import resolution_chaining._
    import resolution_specificity._
    assert(implicitly[Complex]==Complex(0,0))

    // Try: variations on import.
    // will be ambiguous when adding both
    // import resolution_chaining_alt._
    // import resolution._
  }

  object resolution_chaining_alt {
    implicit def anotherComplex(implicit ignored: String): Complex = Complex(2, 0)
  }

  test("macros and compiler generated") {
    assert(Immutable.is[Int])

    class Foo()

    final class Bar()

    assert(!Immutable.is[Foo])

    assert(Immutable.is[Bar])

    // implicitly[Immutable[Foo]]

    // playground.this.Immutable.materialize is not a valid implicit value for playground.Immutable[Foo] because:
    //    open classes are not guaranteed to be immutable
    //        implicitly[Immutable[Foo]]
    //                  ^

    // other examples: Manifest, TypeTag, SourceContext
  }

  object diverging {
    implicit def diverging(implicit i: Int): Int = i
  }
  test("diverging implicit") {
    import diverging._
    //println(implicitly[Int])
  }

  test("debugging implicits ") {
    // compiler command flag:
    // -Xlog-implicits, -Yinfer-debug
    // sometimes useful for diverging implicit, but usually noisy

    // ImplicitNotFound annotation to customize error message for library user:
    //@implicitNotFound(msg = " type ${To} with elements of type ${Elem} based on a collection of type ${From}.")
    // https://github.com/scala/scala/blob/2.11.x/src/library/scala/collection/generic/CanBuildFrom.scala#L29

    // use case in documenation
    // https://github.com/scala/scala/blob/2.11.x/src/library/scala/collection/immutable/List.scala#L104
    // http://www.scala-lang.org/api/current/index.html#scala.collection.immutable.List
  }
}

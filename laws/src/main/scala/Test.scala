package laws

/** The collection to be tested: this provides all elements and collections
  * and mappings thereon which are available inside the tests.
  */
abstract class Test[A, B, CC, T <: Test[A, B, CC, T]](
  val num: Numbers,
  val instance: Instance[A, CC],
  val ops: Ops[A, B]
)(implicit file: sourcecode.File, line: sourcecode.Line, nm: sourcecode.Name) 
extends Sourced
with Named
with TestInfo {
  /** Arbitrary element of the type in the collection */
  def a: A = instance.a

  /** An arbitrary element of a type not in the basic collection.  Should match g(a). */
  lazy val b: B = ops.secret.g(instance.secret.a)

  /** Some function that preserves the type of the elements */
  def f: A => A = ops.f

  /** Some function that changes the type of the elements.  Note that g(a) == b should be true. */
  def g: A => B = ops.g

  /** Some integer.  May be positive or negative.  Could really be anything. */
  def L: Int = num.L

  /** A fixed positive number that is no bigger than the length of `y` */
  def m: Int = if (num.secret.m < 0) (-num.m-1) % ysize else num.m % ysize

  /** A fixed positive number that may be bigger than the length of `y` **/
  val mm: Int = if (num.secret.mm < 0) -num.mm-1 else num.mm

  /** A fixed positive number that is no bigger than the length of `x` */
  def n: Int = if (num.secret.n < 0) (-num.n-1) % xsize else num.n % xsize

  /** A fixed positive number that may be bigger than the length of `x` */
  def nn: Int = if (num.secret.nn < 0) -num.nn-1 else num.nn

  /** A binary operation on the collection elements.
    *
    * If no such operation exists, leave it as `Coll.noOp` and tests requiring this operator will be skipped.
    */
  def op: (A, A) => A = ops.op

  /** A predicate on the type of the elements */
  def p: A => Boolean = ops.p

  /** A partial function that preserves the type of the elements */
  def pf: PartialFunction[A, A] = ops.pf

  /** A specific collection of some size */
  def x: CC = instance.x

  /** The known size of the collection `x` */
  def xsize: Int = instance.xsize

  /** Another specific collection, not necessarily the same as `x` */
  def y: CC = instance.y

  /** The known size of collection `y` */
  def ysize: Int = instance.ysize

  /** Element of type `A` that is a zero with respect to `op`, if `op` exists and a zero exists */
  def zero: A = ops.z

  /** Converts a value to a characteristic integer.  (Default is just hashcode.) */
  def intFrom(a: A): Int = a.##

  /** Operation has a zero */
  def hasZero = ops.hasZ

  /** Operation is symmetric and associative */
  def isSymOp = ops.isSymOp

  def flags: Set[String] = instance.flags

  def boxedRuntime = a.getClass

  def runtimeColl = x.getClass

  def name = nm.value.toString

  def count = Test.Count(num.count, instance.count , ops.count)

  def resetCount { num.resetCount; instance.resetCount; ops.resetCount }

  override lazy val toString =
    nm.value.toString + " @ " + source +
    f"\n  $num\n" +
    instance.toString.split("\n").map("  " + _).mkString("", "\n", "\n") +
    ops.toString.split("\n").map("  " + _).mkString("", "\n", "\n")

  def tryE[A](f: => A): Either[Throwable, A] = try { Right(f) } catch { case e: Throwable => Left(e) }

  def tryO[A](f: => A): Option[A] = try { Some(f) } catch { case _: Throwable => None }

  def renumber(numb: Numbers): T

  def reinstance(inst: Instance[A, CC]): T

  def reoperate(ops: Ops[A, B]): T

  /** The law tested by this (kind of) test, and the ability to run it */
  def law: Law

  /** This is the actual test which runs a law and returns `true` if it passes with these parameters */
  def run: Boolean
}
object Test {
  case class Count(numbers: Numbers.Count, instances: Instance.Count, ops: Ops.Count) {
    def -(that: Count) = new Count(numbers - that.numbers, instances - that.instances, ops - that.ops)
  }

  private class N(var count: Int = 0) { def ++(){ count += 1 } }
  implicit class ComparesTo[A, CC](me: CC)(implicit onceCC: CC => collection.TraversableOnce[A]) {
    def theSameAs[DD](you: DD)(implicit onceDD: DD => collection.TraversableOnce[A]) = {
      val meB, youB = collection.mutable.ArrayBuffer.empty[A]
      onceCC(me).foreach(meB += _)
      onceDD(you).foreach(youB += _)
      meB == youB
    }
    def hasAllOf[DD](you: DD)(implicit onceDD: DD => collection.TraversableOnce[A]) = {
      val meM, youM = collection.mutable.HashMap.empty[A, N]
      onceCC(me).foreach(a => meM.getOrElseUpdate(a, new N).++)
      onceDD(you).foreach(a => youM.getOrElseUpdate(a, new N).++)
      meM.forall{ case (a, n) => youM.get(a).exists(_.count == n.count) } &&
      youM.forall{ case (a, n) => meM contains a }
    }
    def isPartOf[DD](you: DD)(implicit onceDD: DD => collection.TraversableOnce[A]) = {
      val meM, youM = collection.mutable.HashMap.empty[A, N]
      onceCC(me).foreach(a => meM.getOrElseUpdate(a, new N).++)
      onceDD(you).foreach(a => youM.getOrElseUpdate(a, new N).++)
      meM.forall{ case (a, n) => youM.get(a).exists(_.count >= n.count) }
    }
  }

  trait Companion[A, B, CC] {
    def instanceExplorer: Exploratory[Instance[A, CC]]
    def opsExplorer: Exploratory[Ops[A, B]]
    def numberExplorer(inst: Instance[A, CC]): Exploratory[Numbers] =
      new Numbers.Restricted(inst.secret.xsize, inst.secret.ysize)
    def ccType: String
    def eltType: String
    def altType: String
    def heritage: String
    def flatName: String = f"$ccType$eltType"
    def colType: String = f"$ccType[$eltType]"
    def instTypes: String = f"$eltType, $colType"
    def opsTypes: String = f"$eltType, $altType"
    def code(law: Law): String = {
      val name = f"Test$flatName";
      Array(
        f"class $name(num: Numbers, instance: Instance[$instTypes], ops: Ops[$opsTypes])",
        f"extends $heritage[$colType, $name](num, instance, ops) {",
        f"  import Test.ComparesTo",
        f"  def renumber(numb: Numbers) = new $name(numb, instance, ops)",
        f"  def reinstance(inst: Instance[$instTypes]) = new $name(num, inst, ops)",
        f"  def reoperate(oper: Ops[$opsTypes]) = new $name(num, instance, oper)",
        f"  def law = Laws.byLineNumber(${law.lineNumber})",
        f"  def run: Boolean = {",
        law.code.lines.map("    " + _).mkString("\n"),
        f"  }",
        f"}"
      ).mkString("\n")
    }
  }
}


/////////////////////////////////////////
// Selection of element type for tests //
/////////////////////////////////////////

abstract class IntTest[CC, T <: IntTest[CC, T]](
  num: Numbers, instance: Instance[Int, CC], ops: Ops[Int, Long]
)(
  implicit file: sourcecode.File, line: sourcecode.Line, name: sourcecode.Name
)
extends Test[Int, Long, CC, T](num, instance, ops)(file, line, name) {
  type A = Int
  type B = Long
  type Inst = Instance[Int, CC]
  type Oper = Ops[Int, Long]
  override def intFrom(a: Int) = a
}
object IntTest {
  trait Companion[CC] extends Test.Companion[Int, Long, CC] {
    val opsExplorer = IntOpsExplorer
    val heritage = "IntTest"
    val eltType = "Int"
    val altType = "Long"
  }
}

abstract class StrTest[CC, T <: StrTest[CC, T]](
  num: Numbers, instance: Instance[String, CC], ops: Ops[String, Option[String]]
)(
  implicit file: sourcecode.File, line: sourcecode.Line, name: sourcecode.Name
)
extends Test[String, Option[String], CC, T](num, instance, ops)(file, line, name) {
  type A = String
  type B = Option[String]
  type Inst = Instance[String, CC]
  type Oper = Ops[String, Option[String]]
  override def intFrom(s: String) = s.length
}
object StrTest {
  trait Companion[CC] extends Test.Companion[String, Option[String], CC] {
    val io = InstantiatorsOfStr
    val opsExplorer = StrOpsExplorer
    val heritage = "StrTest"
    val eltType = "String"
    val altType = "Option[String]"
    override def flatName = f"${ccType}Str"
  }
}


////////////////////////////////////////////////////
// Enumeration of all collections we want to test //
////////////////////////////////////////////////////

object AllIntTestCompanions {
  val io = InstantiatorsOfInt

  private val everyoneBuffer = Array.newBuilder[Companion[_]]

  class Companion[CC](iexp: InstantiatorsOf[Int] => Instance.FromArray[Int, CC])(implicit name: sourcecode.Name)
  extends IntTest.Companion[CC] {
    val instanceExplorer = io.map(iexp(io).tupled)
    def ccType = name.value.toString.capitalize
  }

  def register[CC](iexp: InstantiatorsOf[Int] => Instance.FromArray[Int, CC])(implicit name: sourcecode.Name): Companion[CC] = {
    val ans = new Companion(iexp)(name)
    everyoneBuffer += ans
    ans
  }

  val list = new Companion(_.list)

  lazy val all = everyoneBuffer.result
}

object AllStrTestCompanions {
  val io = InstantiatorsOfStr

  class Companion[CC](iexp: InstantiatorsOf[String] => Instance.FromArray[String, CC])(implicit name: sourcecode.Name)
  extends StrTest.Companion[CC] {
    val instanceExplorer = io.map(iexp(io).tupled)
    def ccType = name.value.toString.capitalize
  }

  val list = new Companion(_.list)
}

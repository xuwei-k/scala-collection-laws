package laws

/** A quality that can be attached to a collection or law to categorize its properties.
  * Flags are used both for general properties of collections (e.g. is it a sequence)
  * and to indicate atypical or buggy behavior for which a test should or should not
  * be run.
  */
final class Flag()(implicit val nm: sourcecode.Name)
extends Ordered[Flag] { 
  override val toString = nm.value.toString
  override def compare(that: Flag) = toString.compareTo(that.toString)
  override def equals(that: Any) = that match {
    case f: Flag => this.toString == that.toString
    case _       => false
  }
  override val hashCode = toString.hashCode

  // Is this going to break strawman collections?  THESE FLAGS MAY BE FILTERED OUT--SEE Tags.scala!
  def isCamel: Boolean = toString startsWith "CAMEL"
}
object Flag {
  def F(implicit nm: sourcecode.Name) = new Flag()(nm)

  // Fundamental properties of sequences
  val INT = F     // Uses integers as the element type
  val MAP = F     // Is a map
  val SEQ = F     // Is a sequence
  val SET = F     // Is a set
  val STR = F     // Uses strings as the element type

  // Unusual "collections" that are not expected to behave exactly like others
  val ARRAY   = F   // Is an Array
  val STRING  = F   // Is a String
  val STRAW   = F   // strawman collections (when used together with regular collections)
  val ORDERLY = F   // Collection is sorted, but can't maintain itself with all operations as it might lose its ordering
  val ONCE    = F   // Collection is consumed on traversal
  val INDEF   = F   // Collection's size is not yet fixed (lazy collections)
  val SPECTYPE= F   // Collection has constraints on element type, which makes some operations not work

  // Test failures for strawman (straw will break the camel's back...)
  //val CAMEL    = F   // Generic problem (not described)
  //val CAMELMAP = F   // Generic problem with maps (not described)
  //val CAMELSET = F   // Generic problem with sets (not described)
  //val CAMEL_MAP_NEEDS_ORDER = F  // Ordered collections can't map something unordered
  //val CAMEL_SYM_PREPEND     = F  // Prepending with ++: doesn't work
  //val CAMEL_BUFFER_VS_SEQ   = F  // Buffer gives a Seq in a lot of operations
  //val CAMEL_LBUF_X_REMOVE   = F  // ListBuffer will throw exceptions on `remove`
  //val CAMEL_SYM_PREMUT      = F  // Prepending in place with +=: doesn't work on ArrayBuffer and Buffer
  //val CAMEL_ITER_STRING     = F  // Strawman Iterator doesn't have addString without extra args.
  //val CAMEL_SPECMAP_SUPER   = F  // AnyRefMap and LongMap give superclass on map, flatMap, ++, collect
  //val CAMEL_WEAKMAP_SUPER   = F  // WeakHashMap never seems to return a WeakHashMap
  //val CAMEL_BITSET_AMBIG    = F  // Bitsets have an ambiguous zip and map-to-non-int
  //val CAMEL_LZY_X_DROP      = F  // LazyList can throw an exception on `drop` by trying to take `tail`
  //val CAMEL_SETS_NONPLUSSED = F  // Some sets can't be added to without losing their type
  //val CAMEL_MQUEUE_HANG     = F  // Mutable queues hang on filterInPlace
  //val CAMEL_QUEUE_REVERSE   = F  // Queue reverses into an ArrayDeque

  // Everything below here is non-ideal but may reflect the best behavior we can get.
  val SUPER_IHASHM  = F  // Some immutable.HashMap operations revert to the supertype
  val SUPER_ITREES  = F  // Some immutable.TreeSet operations revert to the supertype 
  val SUPER_MXMAP   = F  // Some mutable.Map subclass operations always revert to the supertype
  val SUPER_MOPENHM = F  // mutable.OpenHashMap is especially bad with supertype reversion
  val SUPER_ON_ZIP  = F  // Some collections lose their type when zipped (due to ordering or structure)
  
  // Everything down here is _highly_ dubious behavior but is included to get tests to pass
  val PRIORITYQUEUE_IS_SPECIAL = F  // Inconsistent behavior regarding what is dequeued (ordered) vs. not
  val BITSET_MAP_BREAKS_BOUNDS = F  // Because BitSet doesn't allow negative numbers, maps are problematic
  //val TRANSFORM_INCONSISTENT   = F  // API for method `transform` is inconsistent.  Just ignore it for now.

  // Workarounds for identified bugs go here.
  val MAP_CANT_MKSTRING = F    // Maps have ambiguous `mkString` with no args
  val BITSET_MAP_AMBIG  = F    // Bit maps don't know whether to use StrictOptimized or SortedSet ops for map.
  val BITSET_ZIP_AMBIG  = F    // Same problem with zipping
  val SPEC_MAP_CANT_ADD = F    // AnyRefMap and LongMap lose their identity with `+`

  // Pure bugs that aren't fixed yet
  val QUEUE_PATCH_INDEX = F    // Queue and ArrayStack have an off-by-one error in `patchInPlace`
  val QUEUE_SLIDING     = F    // Queue and ArrayStack will not give you an underfull sliding window (everything else does)
  val UNSAFE_COPY_ARRAY = F    // Array and some friends have an unsafe copyToArray method
  val PQ_RETURNS_NULL   = F    // Priority Queue can just give null when empty!
  val QUEUE_NORESIZE    = F    // Queue and ArrayStack and ArrayDeque don't resize properly on `insert`
}

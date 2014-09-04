package playground

import language.experimental.macros
import scala.reflect.macros.whitebox.Context

// from: https://github.com/scalamacros/macrology201

trait Immutable[T]
object Immutable {

  /* reify implicit search success/failure into a value */

  def is[T]: Boolean = macro isImpl[T]
  def isImpl[T: c.WeakTypeTag](c: Context) = {
    import c.universe._
    val immutableOfT = appliedType(typeOf[Immutable[_]], weakTypeOf[T])
    val inferred = c.inferImplicitValue(immutableOfT, silent = true)
    q"${inferred.nonEmpty}"
  }

  def collapseToNull[T](evidence: Immutable[T]): Immutable[T] = macro collapseToNullImpl[T]
  def collapseToNullImpl[T](c: Context)(evidence: c.Tree): c.Tree = {
    import c.universe._
    q"null"
  }

  /* synthesize implicit based on type */

  implicit def materialize[T]: Immutable[T] = macro materializeImpl[T]
  def materializeImpl[T: c.WeakTypeTag](c: Context) = {
    import c.universe._, definitions.ArrayClass
    val T = weakTypeOf[T]
    val deps =
      T.typeSymbol match {
        case sym if sym == ArrayClass =>
          c.abort(c.enclosingPosition, "arrays are mutable")
        case sym: ClassSymbol =>
          if (!sym.isFinal && !sym.isSealed && !sym.isModuleClass)
            c.abort(c.enclosingPosition, "open classes are not guaranteed to be immutable")
          val childTpes = sym.knownDirectSubclasses.toList.map { case sub: ClassSymbol => sub.toType }
          val fieldTpes =
            T.members.collect { case s: TermSymbol if !s.isMethod =>
              if (s.isVar) c.abort(c.enclosingPosition, s"$T is not immutable because it has mutable field ${s.name}")
              s.typeSignatureIn(T)
            }
          childTpes ++ fieldTpes
        case sym: TypeSymbol =>
          val TypeBounds(_, high) = sym.info
          high :: Nil
      }
      println(s"--- Hello from macro materialize[$T] ---")
    val implicitlies = deps.map { tpe => q"_root_.scala.Predef.implicitly[_root_.playground.Immutable[$tpe]]" }
    val name = TermName(c.freshName())

    /* create implicit object to guard recursion (for recursive types) */
    /* use collapseToNull to eliminate runtime object creation (never used!) */
    q"""
      _root_.playground.Immutable.collapseToNull {
        implicit object $name extends _root_.playground.Immutable[$T]
        ..$implicitlies
        $name
      }
    """
  }
}
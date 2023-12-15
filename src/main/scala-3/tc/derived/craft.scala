package tc
package derived
import hkd._


import scala.compiletime._
import scala.deriving._
import Functor.given

inline def deriveCraft[U[f[_]] <: Product]: Craft[U] = 
    val pack = craftPack[U].toArray
    new { def craft[F[+_]: Applicative, G[_]](gain: [A] => Rep[U, A] => F[G[A]]): F[U[G]] = summonFrom{
      case p: Mirror.ProductOf[U[G]] =>      
          val elements = Vector.tabulate(pack.length){i => 
            type R[f[_]]
            val rep = pack(i).asInstanceOf[Craft[R]]
            rep.craft([A] => (r: Rep[R, A]) => 
              gain[A]([G[_]] => (ug: U[G]) => r(ug.productElement(i).asInstanceOf[R[G]])))            
          }.sequence

          elements.map( es => 
            p.fromProduct(new {
              def productArity = pack.length
              def canEqual(that: Any) = true
              def productElement(i: Int) = es(i)                            
            }))
      case _ => error("can handle only case classes at the moment")
    }
  }


inline def craftPack[U[_[_]]]: Vector[Craft[?]] = 
  type F[_]
  summonFrom{
    case p: Mirror.ProductOf[U[F]] => craftIter[F, Widen[p.MirroredElemTypes]]
  }

inline def craftIter[F[_], P]: Vector[Craft[?]] = 
  inline erasedValue[P] match 
    case _ : (t *: ts) => elemCraft[F, t] +: craftIter[F, ts]
    case _ : EmptyTuple   => Vector()
    case t => customErr[P]("expecting a tuple got ")

inline def elemCraft[F[_], T]: Craft[?] = summonFrom{
  case ur : UnapplyCraft[F, T] => ur.craft
}

trait UnapplyCraft[F[_], T]{
  type U[f[_]]
  def craft: Craft[U]
  def eq: T =:= U[F]
}

object UnapplyCraft:
  given [F[_], A] : UnapplyCraft[F, F[A]] with
    type U[f[_]] = f[A]
    def craft = monoCraft
    def eq = summon[U[F] =:= F[A]]

  given [V[f[_]], F[_]] (using V: Craft[V]): UnapplyCraft[F, V[F]] with
    type U[f[_]] = V[f]
    def craft = V
    def eq = summon[U[F] =:= V[F]]


given monoCraft[X]: Craft[[F[_]] =>> F[X]] with
  def craft[F[+_]: Applicative, G[_]](gain: [A] => ([G[_]] => G[X] => G[A]) => F[G[A]]): F[G[X]] = 
    gain([G[_]] => (x: G[X]) => x)
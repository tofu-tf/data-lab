package tc

import cats.*
import cats.tagless.*
import cats.tagless.syntax.all.*
import hkd.*
import tc.derived.{deriveRepresentable, deriveCraft}

type Rep[-U[_[_]], A] = [F[_]] => U[F] => F[A]

trait PureK[U[_[_]]]:
  def pureK[F[_]](gen: [A] => () => F[A]): U[F]

trait ApplicativeK[U[_[_]]] extends ApplyK[U] with PureK[U]

trait RepresentableK[U[_[_]]] extends ApplicativeK[U]:
  def tabulate[F[_]](gain: [A] => Rep[U, A] => F[A]): U[F]

  override def pureK[F[_]](gen: [A] => () => F[A]): U[F] = 
    tabulate([A] => (rep: Rep[U, A]) => gen[A]())

  extension [F[_], G[_], H[_]](left: U[F])
    def map2K(right: U[G])(f: [A] => (F[A], G[A]) => H[A]): U[H] = 
      tabulate([A] => (rep: Rep[U, A]) => f[A](rep(left) , rep(right)))

object RepresentableK: 
  inline def derived[U[_[_]] <: Product : ApplyK]: RepresentableK[U] = deriveRepresentable

trait TraversableK[U[_[_]]] extends FunctorK[U]:
  extension[F[_], G[+_], H[_]](uf: U[F])
    def traverseK(f: [A] => F[A] => G[H[A]])(using Applicative[G]): G[U[H]]

  extension[F[+_], G[_]](uf: U[[A] =>> F[G[A]]])
    def sequenceK(using Applicative[F]): F[U[G]] = uf.traverseK([A] => (a : F[G[A]]) => a)

trait Craft[U[_[_]]] extends RepresentableK[U] with TraversableK[U]:
  def craft[F[+_]: Applicative, G[_]](gain: [A] => Rep[U, A] => F[G[A]]): F[U[G]]

  def tabulate[F[_]](gain: [A] => Rep[U, A] => F[A]): U[F] = craft[Id, F](gain)

  extension[F[_], G[+_], H[_]](uf: U[F])
    def traverseK(f: [A] => F[A] => G[H[A]])(using Applicative[G]): G[U[H]] = 
      craft[G, H]([A] => (frep: Rep[U, A]) => f(frep(uf)))

object Craft:
  inline def derived[U[f[_]] <: Product: ApplyK]: Craft[U] = deriveCraft

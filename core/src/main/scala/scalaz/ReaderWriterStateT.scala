package scalaz

import Kleisli._

sealed trait ReaderWriterStateT[R, W, S, F[_], A] {
  val apply: R => S => F[(A, S, W)]

  def *->* : (({type λ[α] = ReaderWriterStateT[R, W, S, F, α]})#λ *->* A) =
    scalaz.*->*.!**->**![({type λ[α] = ReaderWriterStateT[R, W, S, F, α]})#λ, A](this)

  def run(r: R, s: S)(implicit i: F[(A, S, W)] =:= Identity[(A, S, W)]): (A, S, W) =
    apply(r)(s).value

  def rT(r: R, s: S)(implicit ftr: Functor[F]): F[A] =
    implicitly[Functor[F]].fmap((asw: (A, S, W)) => asw._1)(apply(r)(s))

  def r(r: R, s: S)(implicit ftr: Functor[F], i: F[A] =:= Identity[A]): A =
    rT(r, s).value

  def sT(r: R, s: S)(implicit ftr: Functor[F]): F[S] =
    implicitly[Functor[F]].fmap((asw: (A, S, W)) => asw._2)(apply(r)(s))

  def s(r: R, s: S)(implicit ftr: Functor[F], i: F[S] =:= Identity[S]): S =
    sT(r, s).value

  def wT(r: R, s: S)(implicit ftr: Functor[F]): F[W] =
    implicitly[Functor[F]].fmap((asw: (A, S, W)) => asw._3)(apply(r)(s))

  def w(r: R, s: S)(implicit ftr: Functor[F], i: F[W] =:= Identity[W]): W =
    wT(r, s).value

  def state(r: R)(implicit ftr: Functor[F]): StateT[S, F, A] =
    StateT.stateT((s: S) => ftr.fmap((asw: (A, S, W)) => (asw._1, asw._2))(apply(r)(s)))

  def rsw(implicit ftr: Functor[F]): ReaderT[R, ({type λ[α] = StateT[S, ({type λ[α] = WriterT[W, F, α]})#λ, α]})#λ, A] =
    Kleisli.kleisli[R, ({type λ[α] = StateT[S, ({type λ[α] = WriterT[W, F, α]})#λ, α]})#λ, A](r =>
      StateT.stateT[S, ({type λ[α] = WriterT[W, F, α]})#λ, A](s =>
        WriterT.writerT[W, F, (A, S)](implicitly[Functor[F]].fmap((asw: (A, S, W)) => (asw._3, (asw._1, asw._2)))(apply(r)(s)))))

  def rws(implicit ftr: Functor[F]): ReaderT[R, ({type λ[α] = WriterT[W, ({type λ[α] = StateT[S, F, α]})#λ, α]})#λ, A] =
    Kleisli.kleisli[R, ({type λ[α] = WriterT[W, ({type λ[α] = StateT[S, F, α]})#λ, α]})#λ, A](r =>
      WriterT.writerT[W, ({type λ[α] = StateT[S, F, α]})#λ, A](
        StateT.stateT[S, F, (W, A)](s => implicitly[Functor[F]].fmap((asw: (A, S, W)) => ((asw._3, asw._1), asw._2))(apply(r)(s)))))

  def evalT(r: R, s: S)(implicit ftr: Functor[F]): F[(A, W)] =
    ftr.fmap((asw: (A, S, W)) => (asw._1, asw._3))(apply(r)(s))

  def eval(r: R, s: S)(implicit ftr: Functor[F], i: F[(A, W)] =:= Identity[(A, W)]): (A, W) =
    evalT(r, s).value

  def exec(r: R)(implicit ftr: Functor[F]): StateT[S, F, W] =
    StateT.stateT((s: S) => ftr.fmap((asw: (A, S, W)) => (asw._3, asw._2))(apply(r)(s)))

}

object ReaderWriterStateT extends ReaderWriterStateTs {
  def apply[R, W, S, F[_], A](k: R => S => F[(A, S, W)]): ReaderWriterStateT[R, W, S, F, A] = new ReaderWriterStateT[R, W, S, F, A] {
    val apply = k
  }
}

trait ReaderWriterStateTs {
  type ReaderWriterState[R, W, S, A] =
  ReaderWriterStateT[R, W, S, Identity, A]

  type RWST[R, W, S, F[_], A] =
  ReaderWriterStateT[R, W, S, F, A]

  type RWS[R, W, S, A] =
  ReaderWriterState[R, W, S, A]

  def readerWriterStateT[R, W, S, F[_], A](k: R => S => F[(A, S, W)]): ReaderWriterStateT[R, W, S, F, A] = new ReaderWriterStateT[R, W, S, F, A] {
    val apply = k
  }

  def readerWriterState[R, W, S, A](k: R => S => (A, S, W)): ReaderWriterState[R, W, S, A] = new ReaderWriterState[R, W, S, A] {
    val apply = (r: R) => (s: S) => Identity.id(k(r)(s))
  }
}
/*
 * Copyright (c) 2021-2022 International Innovation Center of Tsinghua University, Shanghai
 * Ventus is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details. */

package config

object config {

  /** Superclass of all Parameter keys
    * @param default Optional default value, if a query with this key is not found
    */
  abstract class Field[T] private (val default: Option[T]) {
    def this() = this(None)
    def this(default: T) = this(Some(default))
  }

  /** Super class of all Parameter classes. Defines basic querying APIs.
    */
  abstract class View {

    /** Looks up a parameter name within this view to obtain the corresponding value
      * Throws an error if the parameter value is not found
      *
      * @param pname name of the parameter
      * @tparam T the type of the returned value
      * @return the returned value
      */
    final def apply[T](pname: Field[T]): T = {
      val out = find(pname)
      require(out.isDefined, s"Key ${pname} is not defined in Parameters")
      out.get
    }

    // We need to leave this in until all submodules can be bumped
    @deprecated(
      "up(XYZ, site) is no longer necessary; remove the superfluous site argument",
      "CDE 0.1"
    )
    final def apply[T](pname: Field[T], ignore: View): T = apply(pname)

    /** Looks up a parameter name in `this` view to obtain the corresponding value
      * If found, returns Some(value), otherwise returns None
      *
      * @param pname name of the parameter
      * @tparam T the type of the returned value
      * @return the returned value
      */
    final def lift[T](pname: Field[T]): Option[T] = find(pname)

    // All queries start here
    protected[config] def find[T](pname: Field[T]): Option[T]
  }

  /** Parameters are chain-able views, which can be strung together like a linked list.
    * A query will start at the head of the list; if not found, will go to the next parameter object, up, to
    * continue the query.
    */
  abstract class Parameters extends View {

    /** Create a new Parameters object, when queried, searches first in the rhs Parameters. If no value is found, then
      * continue search in this Parameters. Note that this is an immutable operation, creating a new Parameters object.
      * Neither `this` nor `rhs` are modified by this function.
      *
      * e.g. val z = x.alter(y) // z settings are the settings in 'y', then the settings in 'x'
      *
      * @param rhs Parameters to first search in, 'altering' the settings of this Parameters
      * @return a new Parameters object whose settings are first the rhs, then this.
      */
    final def alter(rhs: Parameters): Parameters =
      new ChainParameters(rhs, this)

    /** Create a new Parameters object, when queried, searches first in the `f` function. If no value is found, then
      * continue search in `this`. Parameters. Note that this is an immutable operation, creating a new Parameters object.
      * Neither `this` nor `f` are modified by this function.
      *
      * e.g. val z = x.alter(y) // z settings are the settings in 'y', then the settings in 'x'
      *
      * @param f A function to first search in
      * @return a new Parameters object whose settings are first the `f`, then `this`
      */
    final def alter(
      f: (View, View, View) => PartialFunction[Any, Any]
    ): Parameters =
      alter(Parameters(f))

    /** Create a new Parameters object, when queried, searches first in the `f` partial function. If no value is found,
      * then continue search in `this`. Parameters. Note that this is an immutable operation, creating a new Parameters
      * object. Neither `this` nor `f` are modified by this function.
      *
      * e.g. val z = x.alter(y) // z settings are the settings in 'y', then the settings in 'x'
      *
      * @param f A function to first search in
      * @return a new Parameters object whose settings are first the `f`, then `this`
      */
    final def alterPartial(f: PartialFunction[Any, Any]): Parameters =
      alter(Parameters((_, _, _) => f))

    /** Create a new Parameters object, when queried, searches first in the map. If no value is found,
      * then continue search in `this`. Parameters. Note that this is an immutable operation, creating a new Parameters
      * object. Neither `this` nor `m` are modified by this function.
      *
      * e.g. val z = x.alterMap(y) // z settings are the settings in 'y', then the settings in 'x'
      *
      * @param m a map containing parameters
      * @return a new Parameters object whose settings are first the `f`, then `this`
      */
    final def alterMap(m: Map[Any, Any]): Parameters =
      alter(new MapParameters(m))

    protected[config] def chain[T](
      site:  View,
      here:  View,
      up:    View,
      pname: Field[T]
    ): Option[T]
    protected[config] def find[T](pname: Field[T]): Option[T] =
      chain(this, this, new TerminalView, pname)

    // x orElse y: settings in 'x' overrule settings in 'y'
    final def orElse(x: Parameters): Parameters = x.alter(this)

    /** DEPRECATED!!!
      * Please replace `++` with `orElse`, e.g.
      *   `a ++ b`
      * should become
      *   `a.orElse(b)`
      */
    final def ++(x: Parameters): Parameters = orElse(x)
  }

  object Parameters {

    /** Create a new empty Parameters object
      * @return
      */
    def empty: Parameters = new EmptyParameters

    /** Create a new Parameters object from a lookup function.
      * The arguments to the lookup function are (site, here, up) => { case ... => ... }
      *
      * @param f Function to create a new parameters object
      * @return
      */
    def apply(f: (View, View, View) => PartialFunction[Any, Any]): Parameters =
      new PartialParameters(f)
  }

  /** Configs are concrete user-extensible parameter objects.
    * They have overidden the toString method to be the class name.
    * @param p
    */
  class Config(p: Parameters) extends Parameters {
    def this(f: (View, View, View) => PartialFunction[Any, Any]) =
      this(Parameters(f))

    protected[config] def chain[T](
      site:  View,
      here:  View,
      up:    View,
      pname: Field[T]
    ) = p.chain(site, here, up, pname)
    override def toString = this.getClass.getSimpleName
    def toInstance = this
  }

  // Internal implementation:

  private class TerminalView extends View {
    def find[T](pname: Field[T]): Option[T] = pname.default
  }

  private class ChainView(head: Parameters, site: View, up: View) extends View {
    def find[T](pname: Field[T]) = head.chain(site, this, up, pname)
  }

  private class ChainParameters(x: Parameters, y: Parameters) extends Parameters {
    def chain[T](site: View, here: View, up: View, pname: Field[T]) = {
      x.chain(site, here, new ChainView(y, site, up), pname)
    }
  }

  private class EmptyParameters extends Parameters {
    def chain[T](site: View, here: View, up: View, pname: Field[T]) =
      up.find(pname)
  }

  private class PartialParameters(
    f: (View, View, View) => PartialFunction[Any, Any])
      extends Parameters {
    protected[config] def chain[T](
      site:  View,
      here:  View,
      up:    View,
      pname: Field[T]
    ) = {
      val g = f(site, here, up)
      if (g.isDefinedAt(pname)) Some(g.apply(pname).asInstanceOf[T])
      else up.find(pname)
    }
  }

  private class MapParameters(map: Map[Any, Any]) extends Parameters {
    protected[config] def chain[T](
      site:  View,
      here:  View,
      up:    View,
      pname: Field[T]
    ) = {
      val g = map.get(pname)
      if (g.isDefined) Some(g.get.asInstanceOf[T]) else up.find(pname)
    }
  }
}

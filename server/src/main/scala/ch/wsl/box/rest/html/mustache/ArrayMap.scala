package ch.wsl.box.rest.html.mustache

import java.util
import java.util.Map.Entry

import scala.util.Try

class ArrayMap(val obj: Seq[Any]) extends java.util.AbstractMap[Any, Any] with java.lang.Iterable[Any] {

  override def isEmpty() = size == 0

  override def get(key: Any): Any = Try {
    val index = Integer.parseInt(key.toString)
    return obj(index)
  }.toOption.orNull

  override def containsKey(key: Any): Boolean = get(key) != null

  override def entrySet(): util.Set[Entry[Any, Any]] = throw new UnsupportedOperationException()

  override def iterator(): util.Iterator[Any] = new util.Iterator[Any] {
    var index = 0
    val length = obj.length

    override def hasNext: Boolean = index < length

    override def next(): Any = {
      val nextObj = obj(index)
      index += 1
      nextObj
    }
  }
}

package play

//import pprint.Tree.KeyValue

import scala.collection.mutable
import scala.io.Source

class IniFile() {
  type KeyValues = mutable.Map[String, Seq[String]]
  var sections: mutable.Map[String, KeyValues] = mutable.Map("" -> mutable.Map.empty)
  var path = ""

  def isComment(s: String): Boolean = s.isEmpty || s.trim.startsWith(";") || s.trim.startsWith("#")
  def isSection(s: String): Boolean = s.trim.startsWith("[") && s.trim.endsWith("]")
  def trimSection(s: String): String = s.trim.replaceAll("^\\[", "").replaceAll("\\]$", "")
  def keyValue(s: String): Option[(String, String)] = {
    val pattern = "(.*)=(.*)".r
    s.trim match {
      case pattern (k, v) => Some(k.trim, v.trim)
      case f => None
    }
  }
  def valueSplit(s: String): Seq[String] = s.split(',').map(_.trim)

  def this(p: String){
    this()
    this.path = p
    this.parse
  }
  def parse = {
    val file = Source.fromFile(path)
    val buf = file.getLines().filter(!isComment(_))
    var curSection = "";
    for(l <- buf){
      if(isSection(l)){
        curSection = trimSection(l)
        sections(curSection) = mutable.Map.empty
      }
      else{
        keyValue(l).foreach{ case (k, v) =>
          sections(curSection) += k -> valueSplit(v)
        }
      }
    }
    file.close()
  }
}

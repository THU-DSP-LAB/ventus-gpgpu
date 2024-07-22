import scala.io.Source

object ReadCSV {
  def main(args: Array[String]): Unit = {
    // 确保文件路径是正确的
    val filePath = "ventus/tests/src/TensorCoreTest/testData/RA.txt"

    try {
      // 使用Source从文件路径创建一个输入流
      val lines = Source.fromFile(filePath).getLines()

      // 遍历每一行
      for (line <- lines) {
        // 以 为分隔符将字符串分割成数组
        val values = line.split(" ")

        // 打印出数组中的每个值
        values.foreach(println)
      }
    } catch {
      case e: Exception => println("An error occurred: " + e.getMessage)
    }
  }
}
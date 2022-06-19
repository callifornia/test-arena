def encode(arabic: Int): String = {
  val mapping = List(1 -> "I", 5 -> "V", 10 -> "X", 50 -> "L", 100 -> "C").reverse

  def inner(target: Int, acc: List[String] = List.empty): List[String] =
    if (target == 0) acc
    else {
      mapping.find(_._1 <= target) match {
        case None => acc
        case Some((k, v)) => inner(target - k, acc :+ v)
      }
    }

  inner(arabic).mkString
}

// A few examples:
println(encode(1) == "I")
println(encode(3) == "III")
println(encode(4) == "IIII")
println(encode(6) == "VI")
println(encode(14) == "XIIII")
println(encode(21) == "XXI")
println(encode(89) == "LXXXVIIII")
println(encode(91) == "LXXXXI")

encode _ eq encode _
val b = encode _


//object Roman extends App {
//  /** Encode an integer as a roman string.
//   *
//   * Symbol  Value
//   * I       1
//   * V       5
//   * X       10
//   * L       50
//   * C       100
//   *
//   * Roman numerals come with a few oddities (such as 4 being IV, understood as V/5 minus 1/I).
//   * But don't worry about these for now. Assume 4 is IIII.
//   *
//   * @param arabic an integer between 1 and 100 included (for instance 21)
//   * @return the associated roman numeral (for instance "XXI")
//   * */
//}


//    89 - 50(L) = 39
//    39 - 10(X) = 29
//    29 - 10
//    0
//
//    ???
//}
//  89 ->      LXXXVIIII
//  50 + 39 -> LXXXVIII
//    39 -> XXXVIIII
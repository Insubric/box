package ch.wsl.box.client.utils

object StringUtils {
  def levenshteinDistance(s1: String, s2: String): Int = {
    val m = s1.length
    val n = s2.length
    // Create a matrix to store results of subproblems
    val dp = Array.ofDim[Int](m + 1, n + 1)

    // Initialize the first row and column with indices
    for (i <- 0 until m + 1) {
      dp(i)(0) = i
    }
    for (j <- 0 until n + 1) {
      dp(0)(j) = j
    }

    // Fill the matrix using dynamic programming
    for (i <- 1 to m) {
      for (j <- 1 to n) {
        if (s1.charAt(i - 1) == s2.charAt(j - 1)) {
          dp(i)(j) = dp(i - 1)(j - 1)
        } else {
          val substitutionCost = dp(i - 1)(j - 1) + 1
          val insertionCost = dp(i)(j - 1) + 1
          val deletionCost = dp(i - 1)(j) + 1
          dp(i)(j) = Math.min(substitutionCost, Math.min(insertionCost, deletionCost))
        }
      }
    }

    dp(m)(n)
  }

  def stringSimilarity(s1: String, s2: String): Double = {
    val distance = levenshteinDistance(s1, s2)
    val maxLength = Math.max(s1.length, s2.length)
    if (maxLength == 0) return 1.0 // Avoid division by zero
    1 - (distance / maxLength.toDouble)
  }
}

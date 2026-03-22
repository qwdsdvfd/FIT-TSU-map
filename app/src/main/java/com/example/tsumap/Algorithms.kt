package com.example.tsumap

import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.max

object PUBLICMATRIX{
    var value: BitMatrix? = null
        private set

    fun set(matrix: BitMatrix?) {
        value = matrix
    }
}
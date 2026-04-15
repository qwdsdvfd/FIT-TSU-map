package com.example.tsumap

import java.util.BitSet

class matrixToBit(val height: Int, val width: Int, val data: BitSet) {
    fun get(x: Int, y: Int): Boolean = data[y * width + x]
    fun set(x: Int, y: Int, value: Boolean) {
        if (value) data.set(y * width + x) else data.clear(y * width + x)
    }
}